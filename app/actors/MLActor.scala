package actors

import akka.actor.{ActorLogging, Actor}
import java.util.Date
import scala.concurrent.duration.Duration
import models.Estates
import pl.prxsoft.registry.land.ExtLandProperty
import pl.prxsoft.registry.land.metrics.{CalcTrace, AlphaDistanceCalculator}
import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 30.08.2013
 * Time: 22:12
 */

sealed trait MLActorMessage

case class StartKNN(daysBefore: Int, maxItems: Int, equalThreshold: Double, similarThreshold: Double,
                    parseSet: Option[String])
  extends MLActorMessage

case class FinishedKNN(startDate: Date, tookTime: Duration, numItems: IndexedSeq[ExtLandProperty], mat: Vector[(String, AlphaDistanceCalculator)]) extends MLActorMessage
case class StartML(calc: AlphaDistanceCalculator, items: IndexedSeq[ExtLandProperty], eqT : Double, simT : Double, nNeighbours: Int) extends MLActorMessage
case class FinishML(baseSize: Int, tookTime: Duration, calc : AlphaDistanceCalculator, items: Vector[EstateRelation]) extends MLActorMessage

case class EstateRelation(estate: ExtLandProperty, equals: Vector[(Double, ExtLandProperty, List[CalcTrace])], similar: Vector[(Double, ExtLandProperty,List[CalcTrace])])

object MLActor {
  def calculateKNN(properties: List[ExtLandProperty]) = {
      val estates = properties.toIndexedSeq
      val calculator = new AlphaDistanceCalculator(estates(0).weighs)

      val zDistMatrix = calculator.distanceMatrixZNormScaled(estates)
      val simMatrix = zDistMatrix.distToSimMatrix
      (zDistMatrix, simMatrix)
    }
}

class MLActor extends Actor with  ActorLogging  {


  override def receive = {
    case StartKNN(daysBefore, maxItems, eqT, sT, parseSet) => {
      log.info(s"Starting kNN with daysBefore=$daysBefore maxItems=$maxItems")
      val startTime = System.nanoTime()
      val startDate = new Date()

      DB.withSession {
        implicit s: Session =>
          log.info(s"Searching for items from set: $parseSet with age: $daysBefore maxItems: $maxItems")
          val items = parseSet match {
            case Some(set) => Estates.findNewerThanDaysOrMaxWithParseSet(daysBefore, maxItems, set)
            case None => Estates.findNewerThanDaysOrMax(daysBefore, maxItems)
          }
          val itemsViews = items.filter(_.viewed)
          log.info(s"Items viewed: ${itemsViews.size}")
          if (!items.isEmpty) {
            val (dMat, sMat) = MLActor.calculateKNN(items)
            val endTime = (System.nanoTime() - startTime)

            log.info(s"Matrix calculation took: ${endTime / 1e6}")
            //          val res = Vector(("z-norm distance Matrix", dMat),("Similarity Matrix", sMat))
            sender ! FinishedKNN(startDate, Duration.fromNanos(endTime), items.toVector, Vector("z-norm Sim Matrix" -> sMat))
          } else {
            sender ! FinishedKNN(startDate, Duration.Zero, Vector.empty, Vector.empty)
          }
      }
    }

    case StartML(calc, items, eqT, simT, nNeighbours) => {
      val startTime = System.nanoTime()
      log.info(s"Starting unique finder eqT=$eqT simT=$simT")

      val estates = findUniqueEstates(calc, items, eqT, simT, nNeighbours)
      log.info(s"Ended unique finder")
      val endTime = System.nanoTime() - startTime
      sender ! FinishML(items.size, Duration.fromNanos(endTime) ,calc, estates)
    }

  }

  def findUniqueEstates(calc: AlphaDistanceCalculator, items: IndexedSeq[ExtLandProperty], eqT: Double, simT: Double, nNeighbours: Int) = {
    val itemsWithIdx = items.zipWithIndex
    val sortedItems = itemsWithIdx.sortBy(_._1.pubDate.getTime).reverse

    val equalMap = new mutable.HashSet[String]()
    val resultList = new mutable.ArrayBuffer[EstateRelation]()

    for (item <- sortedItems if !equalMap.contains(item._1.guid)) {
      val idx = item._2
      val e = item._1
      // find neighbours
      val neigh = calc.matrix(idx).zipWithIndex
      val sortedNeigh = neigh.sortBy(-_._1).filter { i => items(i._2).guid != e.guid }
      val equals = sortedNeigh.filter(_._1 >= eqT).toIndexedSeq
      val equalsVector = equals.map { i => (i._1, items(i._2), calc.traceMatrix(idx)(i._2))  }

      var took = 0
      var i = 0
      val similars = ArrayBuffer[(Double, Int)]()
      while (i < sortedNeigh.length) {
        val d: Double = sortedNeigh(i)._1
        if (d < eqT && took < nNeighbours) {
           similars += sortedNeigh(i)
           took +=1
        } else if (d < eqT && d >= simT) {
          similars += sortedNeigh(i)
        }
        i += 1
      }

      val simVector = similars map { i => (i._1, items(i._2), calc.traceMatrix(idx)(i._2)) }
      // add to visited set
      equals.foreach { i => equalMap += items(i._2).guid }

      resultList += EstateRelation(e, equalsVector.toVector, simVector.toVector)
    }
    resultList.toVector
  }

}
