package actors

import akka.actor.{ActorLogging, Actor}
import akka.pattern.pipe

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import pl.prxsoft.registry.land.ExtLandProperty
import scala.Some
import pl.prxsoft.registry.land.scraper.EstateItemParser

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 18.08.2013
 * Time: 12:33
 *
 */
object EstateItemScraperMessage {
  sealed trait ScrapperMessage
  case class ScrapEstate(guid: String, estate : ExtLandProperty, itemParser: EstateItemParser) extends ScrapperMessage
  case class SuccessfullyScrapped(guid: String, estate: ExtLandProperty) extends ScrapperMessage
  case class FailedToScrap(guid: String) extends ScrapperMessage
}

class EstateItemScraper extends Actor with ActorLogging {
  import EstateItemScraperMessage._

  override def receive = {
    case ScrapEstate(guid, estate, itemParser) => {
      val f = scrapNewEstate(guid, estate, itemParser) recover {
        case t : Throwable =>
          log.error(t, s"Failed to scrap guid=[$guid]. Parser ${itemParser.site}")
          FailedToScrap(guid)
      } pipeTo sender
    }
  }


  def scrapNewEstate(guid: String, estate: ExtLandProperty, parser: EstateItemParser) = {
    log.info(s"URL[$guid] is to be scrapped")

    val f = parser.scrapPage(guid).map {
      case Some(extLand) =>
        log.info(s"Scrapped page $guid")
        SuccessfullyScrapped(guid, estate.merge(extLand))

      case None =>
        log.info(s"Failed to scrapp guid=[$guid]")
        FailedToScrap(guid)
    }

    f
  }
}