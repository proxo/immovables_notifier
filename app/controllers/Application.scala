package controllers

import play.api.mvc._
import akka.actor.{Cancellable, DeadLetter, Props}
import play.libs.Akka
import actors._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.{ExecutionContext, Await}
import java.util.concurrent.TimeUnit
import play.api.libs.json.{JsValue, Json}
import pl.prxsoft.registry.land._
import play.api.db.slick.DB
import play.api.Play.current

import models._
import actors.LastFetchResult
import actors.StartNewFetch
import actors.GetLastFetchResults
import play.api.data.Forms._
import play.api.data._
import play.api.libs.iteratee.Concurrent
import play.api.libs.EventSource
import java.util.concurrent.atomic.{AtomicReference, AtomicLong}
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import play.api.libs.json.Json._

object Application extends Controller with LoginController.Secured {
  val requestCounter = new AtomicLong(0L)
  val listener = Akka.system.actorOf(Props[Listener])
  Akka.system.eventStream.subscribe(listener, classOf[DeadLetter])

  implicit val timeout = new Timeout(5, TimeUnit.SECONDS)
  implicit val exWrites = Json.writes[ExtLandProperty]
  implicit val landWrites = Json.writes[LandProperty]

  val (out, wsOutChannel) = Concurrent.broadcast[JsValue]
  var scheduledFetch : AtomicReference[Option[Cancellable]] = new AtomicReference[Option[Cancellable]](None)

  case class MLParams(simT: BigDecimal, eqT : BigDecimal, days: Int, neigh: Int, parseSet: String)

  val mlForm : Form[MLParams] = Form(
    mapping(
      "simT" -> bigDecimal,
      "eqT" -> bigDecimal,
      "days" -> number(min=1,max=30),
      "neigh" -> number(min=1,max=30),
      "parseSet" -> nonEmptyText(1, 30)
    )(MLParams.apply)(MLParams.unapply)
  )

  def index = IsAuthenticated { username => _ =>
    DB.withSession {
      implicit s: scala.slick.session.Session =>
        val parseSets = ParseSetTable.findAllActive.toVector
        Ok(views.html.parsesets("Select immovables set", "Home", Some(parseSets)))
    }
  }

  def produceParseSites(parseSet: String) : Vector[ParseSite] =
    DB.withSession {
      implicit s: scala.slick.session.Session =>
        ParseSiteTable.findBySetName(parseSet).toVector
    }


  def startFetch(setName: String) = IsAuthenticated { username => _ =>
    val sites = produceParseSites(setName)
    if (!sites.isEmpty) {
      val myActor = Akka.system.actorOf(Props[ParserActor], name = "parserActor" + requestCounter.incrementAndGet())
      myActor ! StartNewFetch(sites, Some(wsOutChannel), Some(setName))
      val siteNames = sites.map(_.name).mkString(", ")
      Ok(views.html.index(s"Starting workers to fetch sites: ${siteNames}", "Fetch"))
    } else {
      Ok(views.html.index("No sites set selected", "Fetch"))
    }
  }

  def showResults(days: Int) = IsAuthenticated { username => _ =>
    DB.withSession {
      implicit s: scala.slick.session.Session =>
        val estates = Estates.findNewerThanDays(days)
        Ok(views.html.fetchresults("Last results of fetch " + estates.size, "Results", estates, days))
    }
  }

  def showSingle = IsAuthenticated { username => _ =>
    val myActor = Akka.system.actorOf(Props[ParserActor], name = "parserActor" + requestCounter.incrementAndGet())
    val res = myActor.ask(GetLastFetchResults).mapTo[LastFetchResult]
    val result = Await.result(res, timeout.duration)
    Ok(Json.toJson(result.resultSeq)) as "application/json"
  }

  def startClustering(daysBefore: Int = 10, maxItems: Int = 1000, eqT: Double = 0.95, simT: Double = 0.8) = IsAuthenticated {
    username => implicit request =>
      implicit val timeout = new Timeout(300, TimeUnit.SECONDS)

      mlForm.bindFromRequest.fold(
        errors => {
          println(errors.errors.foldLeft("") {
            _ + _.message
          })
          BadRequest(views.html.mlparams("ML parameters", "ML", errors, List()))
        },
        ml => {
          val mlActor = Akka.system().actorOf(Props[MLActor], name = "mlActor" + requestCounter.incrementAndGet())
          val parseSet = if (ml.parseSet.isEmpty || ml.parseSet == "all") None else Some(ml.parseSet)
          val knnRes = (mlActor ? StartKNN(ml.days, maxItems, ml.eqT.toDouble, ml.simT.toDouble, parseSet)).mapTo[FinishedKNN]
          val result = Await.result(knnRes, timeout.duration)

          // get calculator
          val calcs = result.mat.map { res => res._2 }
          // first to go
          val finishMLFut = (mlActor ? StartML(calcs.head, result.numItems, ml.eqT.toDouble, ml.simT.toDouble, ml.neigh)).mapTo[FinishML]
          val finishMLRes = Await.result(finishMLFut, timeout.duration)

          Ok(views.html.ml("ML calculations", "ML", result.mat.head._1, finishMLRes))
        }
      )
  }

  def fetchFeed() = Action {
    implicit req => {
      Ok.feed(out &> EventSource()).as("text/event-stream")
    }
  }

  case class MLSetupData(eqT: Double, simT: Double, days: Int, params: List[Double])

  def setupClustering() = IsAuthenticated { username => implicit request =>
    DB.withSession {
          implicit s: scala.slick.session.Session =>
            val parseSets = ParseSetTable.findAllActive.map(_.name)
            Ok(views.html.mlparams("ML parameters", "ML", mlForm.fill(MLParams(0.92, 0.97, 10, 3, "all")), parseSets))
        }

  }

  def scheduleFetch(setName : String, freq: Int) = IsAuthenticated { username => implicit request =>

    scheduledFetch.get() match {
      case Some(canc) => Ok(views.html.index("Fetch is already scheduled","Schedule fetch"))
      case None =>
        val myActor = Akka.system.actorOf(Props[ParserActor], name = "scheduled-parserActor" + requestCounter.incrementAndGet())
        val parseSites = produceParseSites(setName)
        val c = Akka.system().scheduler.schedule(10 millis, freq minutes, myActor, StartNewFetch(parseSites, None, None))
        if (!scheduledFetch.compareAndSet(None, Some(c))) {
          // someone else managed to start fetch
          c.cancel()
          Ok(views.html.index("Scheduled was triggered while processing this request","Schedule fetch"))
        } else
          Ok(views.html.index(s"Scheduled the fetch every $freq minutes","Schedule fetch"))
    }
  }

  def cancelFetch = IsAuthenticated { username => implicit request =>
    scheduledFetch.get() match {
      case c@Some(cancellable) =>
        if (scheduledFetch.compareAndSet(c, None)) {
          cancellable.cancel()
          Ok(views.html.index("Cancelled scheduled fetch","Schedule fetch"))
        }
        else {
          Ok(views.html.index("Cancelling was done from another thread","Schedule fetch"))
        }
      case None =>
        Ok(views.html.index("Fetch is not scheduled","Schedule fetch"))
    }
  }

  /**
   * Mark estate as viewed
   * @return
   */
  def markAsViewed = IsAuthenticated {
    username => implicit request =>
      request.body.asJson.map {
        json =>
          (json \ "guid").asOpt[String].map {
            guid =>
              DB.withSession { implicit s: scala.slick.session.Session =>
                val res = Estates.markAsViewed(guid)
                val status = if (res) "OK" else "NOK"
                Ok(toJson(Map("status" -> status, "guid" -> guid)))
              }
          }.getOrElse {
            BadRequest("Expected JSON data with guid")
          }
      }.getOrElse {
        BadRequest("Expected JSON data")
      }
  }

  def parseLedger = IsAuthenticated { username => implicit request =>
    DB.withSession { implicit s =>
      val parseLedger = ParseLedgerTable.findAll
      Ok(views.html.parseledger("Parse ledger", "Ledger", parseLedger))
    }
  }

}