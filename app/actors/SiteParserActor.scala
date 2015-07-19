package actors

import akka.actor._
import pl.prxsoft.registry.land.scraper.{EstateItemParser, SiteParser}
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import models.Estates
import scala.concurrent.{ExecutionContext, Future}
import akka.pattern.pipe
import akka.routing.RoundRobinRouter
import play.api.Play.current
import ExecutionContext.Implicits.global
import akka.event.LoggingReceive
import scala.Some
import pl.prxsoft.registry.land.ExtLandProperty
import akka.actor.SupervisorStrategy.{Restart, Stop}
import scala.concurrent.duration._

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 10.11.2013
 * Time: 23:29
 */

object SiteParserMessages {
  sealed trait Message
  case class StartParsing(url: String, siteName: String,  siteParser : SiteParser, setName : Option[String])
  case class SuccessfullyParsed(siteName: String, items: List[ExtLandProperty],
                                newItems: List[ExtLandProperty], oldItems: List[ExtLandProperty], failedItems: List[ExtLandProperty])
  case class FailedToParse(siteName: String, reason: String)
}

class SiteParserActor  extends Actor with  ActorLogging {
  val MAX_SIZE: Int = 255
  override def preStart = log.info("Pre start actor: {}", this.self)
  override def postStop = log info "Post stop actor: " + this.self

  import SiteParserMessages._
  import EstateItemScraperMessage._

  private var siteName: String = ""
  private var childrenUrls : Set[String] = Set()
  private var estateItems : List[ExtLandProperty] = List()
  private var parseSet : Option[String] = None
  private[this] var parentParser : ActorRef = context.parent

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 2, withinTimeRange = 1 minute) {
      case e: NullPointerException     ⇒
        log.error(e, "NPE on SiteParserActor")
        Restart

      case e: Exception                ⇒ {
        log.error(e, "Error on SiteParserActor")
        Stop
      }
    }

  def persisItem(e: ExtLandProperty)(implicit session: Session)  = {
    var (oldItems,newItems, failed) = (0, 0, 0)
    try {
      Estates.findByGUID(e.guid) match {
        case Some(x) => oldItems += 1
        case None => {
          Estates.insert(e.copy(title = e.title.take(MAX_SIZE)))
          newItems += 1
        }
      }

    } catch {
      case ex: Exception => {
        log.error(ex, s"Some error occured when persisting item: $e")
        failed += 1
      }
    }
    (newItems, oldItems, failed)
  }

  /**
  *  Persist and return number of items (0,0,0)
  */
  def persistItems(items : Seq[ExtLandProperty], parseSet: Option[String]) : (List[ExtLandProperty], List[ExtLandProperty], List[ExtLandProperty]) = {
    log.info(s"Persisting ${items.size} items")

    DB.withSession {
      implicit s: Session =>
        val (newItems, oldItems, failed) = items.foldLeft((List[ExtLandProperty](), List[ExtLandProperty](), List[ExtLandProperty]())) {
          (a, e) =>
            val (n, o, f) = persisItem(e.copy(parseSet = parseSet))
            val ff = (l : List[ExtLandProperty], cnt: Int) => if (cnt > 0) e :: l else l
            (ff(a._1, n), ff(a._2, o), ff(a._3, f))
        }

      (newItems, oldItems, failed)
    }
  }

  def persistAndRespond(siteName :String , parentParser: ActorRef, items: Seq[ExtLandProperty], parseSet: Option[String]) = {
    val f = Future {
      persistItems(items, parseSet)
    }
    // map to message and sent back
    f.map {
        x => SuccessfullyParsed(siteName, items.toList, x._1, x._2, x._3)
    } recover {
      case t : Throwable =>
        log.error(t, s"Error while persisting items on site: $siteName. Piping to parent actor.")
        FailedToParse(siteName, t.toString)
    } pipeTo parentParser
  }

  def spawnChildren(siteName: String, seq: IndexedSeq[ExtLandProperty], itemParser: EstateItemParser, parseSet: Option[String]) = {
    import EstateItemScraperMessage._

    this.siteName = siteName
    this.parseSet = parseSet
    this.childrenUrls = seq.map(_.guid).toSet
    this.estateItems = List()

    val childActor = context.actorOf(
            Props[EstateItemScraper].withRouter(RoundRobinRouter(nrOfInstances = 5)), name = "siteItemParser-" + siteName)
    // start children and scraping
    log.info(s"Spawning childs for site: $siteName")
    for {
      elp <- seq
    } childActor ! ScrapEstate(elp.guid, elp, itemParser)
  }


  def receive: Receive = LoggingReceive {

    case StartParsing(url, siteName, parser, parseSet) => {
      log.info(s"Starting parser $siteName")
      parentParser = context.sender
      val itemsF = parser run url

      log.info(s"Done parser run, responding to $sender")

      val mapped = itemsF.map {
        items =>
          parser.itemParser match {
            case Some(parser) => {
              spawnChildren(siteName, items, parser, parseSet)
              None
            }
            case None => Some(items)
          }
      }

      // send response
      mapped onSuccess { case items =>
        items.map {

          items1 => persistAndRespond(siteName, parentParser, items1, parseSet)
        }
      }

      mapped onFailure {
        case t =>
          log.error(t, s"Error while parsing main site $siteName")
          sender ! FailedToParse(siteName, t.getMessage)
      }
    }

    case SuccessfullyScrapped(guid: String, estate: ExtLandProperty) => {
      val newChildrenUrls = childrenUrls - guid
      log.info(s"Ended parsing item: $guid")
      checkIfAllDone(newChildrenUrls, Some(estate))
    }

    case FailedToScrap(guid: String) => {
      log.info(s"Failed parsing item: $guid")
      checkIfAllDone(childrenUrls - guid, None)
    }
  }

  def checkIfAllDone(newChildrenUrls: Set[String], estate: Option[ExtLandProperty]) {
    val estates =  estate match {
      case Some(x) => x :: estateItems
      case None => estateItems
    }

    if (newChildrenUrls.isEmpty) {
      log.info(s"Sending all gathered items to parent ${parentParser}")
      persistAndRespond(siteName, parentParser, estates, this.parseSet)
    } else {
      this.childrenUrls = newChildrenUrls
      this.estateItems = estates
    }
  }
}
