package actors

import akka.actor._
import scala.collection.mutable.{Set => MutSet}
import java.util.Date
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.routing.SmallestMailboxRouter
import play.api.libs.json.{Json, JsValue}
import play.api.libs.iteratee.Concurrent
import pl.prxsoft.registry.land.scraper.SiteParser
import models.{ParseLedger, ParseLedgerTable, ParseSite}
import akka.actor.OneForOneStrategy
import pl.prxsoft.registry.land.ExtLandProperty
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 10.08.2013
 * Time: 17:34
 */

sealed trait RssParserMessages

case class StartNewFetch(parseSites: IndexedSeq[ParseSite],
                         channelOut: Option[Concurrent.Channel[JsValue]],
                         parseSet: Option[String]) extends RssParserMessages

case class GetLastFetchResults() extends RssParserMessages
case class LastFetchResult(date: Date, resultSeq: IndexedSeq[ExtLandProperty]) extends RssParserMessages

sealed trait State
case object Idle extends State
case object Parsing extends State

sealed trait Data
case object Uninitialized extends Data
case class Parsed(lastDate: Date, lastResult: IndexedSeq[ExtLandProperty]) extends Data
case class ParsingData(lastDate : Date, lastResult : IndexedSeq[ExtLandProperty],
                       startDate: Date, sitesToProcess: Set[String],
                       parsedEstates: List[ExtLandProperty], channelOut: Option[Concurrent.Channel[JsValue]]) extends Data


/**
 * Main parser FSM
 */
class ParserActor extends Actor with  ActorLogging with FSM[State,Data] {
  implicit val exWrites = Json.writes[ExtLandProperty]


  startWith(Idle, Uninitialized)

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: NullPointerException     ⇒ Restart
      case e: Exception                ⇒ {
        log.error(e, "Error on ParserActor")
        Escalate
      }
    }

  override def preStart(): Unit = {
    log.debug("preStart action")
  }

  def checkIfEndParsing(data: ParsingData, g: String, newItems: List[ExtLandProperty]) = {
    val rest = data.sitesToProcess - g
    val newList =  newItems ::: data.parsedEstates
    if (rest.isEmpty) {
      // stop all children
      context.children.foreach(context.stop(_))
      log.info(s"[DONE] Ended parsing sites")

      goto(Idle) using Parsed(data.startDate, newList.toVector)
    } else
      stay using data.copy(sitesToProcess = rest, parsedEstates = newList)
  }

  when(Idle, stateTimeout = 5 minute) {

    case Event(StartNewFetch(parseSites, channel, parseSet), d: Data) => {
      log.info(s"Starting parsers for url ${parseSites.map(_.name).mkString(",")}")
      // build parser
      val parsers = parseSites.map({ s => SiteParser.build(s.name, s.url) })
      val sitesToProcess = parseSites.map(_.name)

      val childActor = context.actorOf(Props[SiteParserActor].withRouter(SmallestMailboxRouter(nrOfInstances = 5)))
      // start child workers
      for (parse <- parseSites zip parsers) {
        log.info(s"!!! Sending parse message to site: ${parse._1.name} actor parser")
        childActor ! SiteParserMessages.StartParsing(parse._1.url, parse._1.name, parse._2, parseSet)
      }

      val startDate = new Date()
      val lastDate = d match {
        case Parsed(d, el) => d
        case _ => startDate
      }
      val lastData = d match {
        case Parsed(d, el) => el
        case _ => Vector()
      }

      goto(Parsing) using
        ParsingData(lastDate, lastData, startDate, sitesToProcess.toSet, List(), channel)
    }

    case Event(StateTimeout, _) =>
      log.info("Timeout in idle state. Stopping parser:" + self.path)
      context.stop(self)

      stay()
  }

  when(Parsing, stateTimeout = 5 minute) {
    case Event(SiteParserMessages.SuccessfullyParsed(siteName, items, n, o, f), data: ParsingData) => {
      log.info(s"!!! Parser $siteName finished with stats [n=${n.size}, o=${o.size}, f=${f.size}]")
      val allitems = n.map(("new", _)) ::: o.map(("old", _)) ::: f.map(("failed", _))
      allitems.foreach {
        params =>
          val (status, i) = params
          val json = Json.obj("status" -> status, "estate" -> i)
          data.channelOut.map {
            es => es.push(json)
          }
      }
      val newItems = data.sitesToProcess - siteName

      if (newItems.isEmpty) {
        // add
        DB.withSession {
          implicit s: scala.slick.session.Session =>
            ParseLedgerTable.insert(ParseLedger(n.size, o.size, f.size,
              new java.sql.Date(data.startDate.getTime) , new java.sql.Date(new Date().getTime)))
        }
      }

      checkIfEndParsing(data, siteName, items)
    }

    case Event(SiteParserMessages.FailedToParse(siteName, reason), data: ParsingData) => {
      log.info(s"Site parse $siteName failed: $reason")
      // inform in ui
      val json = Json.obj("status" -> "failed", "site" -> siteName)
      data.channelOut.map { es => es.push(json) }

      checkIfEndParsing(data, siteName, Nil)
    }

    case Event(StateTimeout, _) =>
      log.info("Timeout in parsing state. Stopping parser:" + self.path)
      context.stop(self)
      stay()
  }

  whenUnhandled {
    case Event(GetLastFetchResults, d: Data) => {
      log.info("Replying with last fetch result")
      val lastDate = d match {
        case dd: ParsingData => dd.lastDate
        case dd: Parsed => dd.lastDate
        case _ => null
      }

      val lastResult = d match {
        case dd: ParsingData => dd.lastResult
        case dd: Parsed => dd.lastResult
        case _ => null
      }

      sender ! LastFetchResult(lastDate, lastResult)
      stay
    }

    case Event(a, s) => {
      log.info(s"unknown action $a in state $s")
      goto(Idle) using Parsed(new Date, Vector())
    }

  }
  initialize
}
