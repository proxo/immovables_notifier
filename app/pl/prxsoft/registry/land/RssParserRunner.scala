package pl.prxsoft.registry.land

import _root_.pl.prxsoft.registry.land.metrics.AlphaDistanceCalculator
import java.io.{PrintWriter, FileWriter, File}
import java.text.SimpleDateFormat
import pl.prxsoft.registry.land.metrics.AlphaDistanceCalculator
import pl.prxsoft.registry.land.scraper.gratka.GratkaSiteParser

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 09.07.2013
 * Time: 00:11
 * To change this template use File | Settings | File Templates.
 */
object RssParserRunner {

  def main(args:Array[String]) {
    if (args.isEmpty) {
      println("No destination file given!")
      System.exit(1)
    }

    val urlAddress: String = "http://dom.gratka.pl/mieszkania-sprzedam/lista/rss/,warszawa,mokotow%5Ewilanow,550000,700000,1d,80,dz,co,cd,od,mo.html"
    val runner = new GratkaSiteParser(urlAddress)
//    val items = runner.run.toIndexedSeq
//
//    val stmt = s"Run ended with ${items.length} items"
//    val itemsByPriceDesc = items sortBy (-_.area)
//    itemsByPriceDesc.foreach(println _)
//
//    println(stmt)
//    val outFile = new PrintWriter(args(0), "UTF-8")
//    outFile.write("guid;date;price;area;rooms\n")
//    val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm")
//
//    itemsByPriceDesc foreach { i=>
//      val dateStr = sdf.format(i.pubDate)
//      outFile.write(s"${i.guid};${dateStr};${i.price};${i.area};${i.rooms}\n")
//    }
//
//    val calculator = new AlphaDistanceCalculator(itemsByPriceDesc(0).weighs)
//    outFile.close()
//
//    val distances = calculator.distanceMatrixSimple(itemsByPriceDesc)
////    val rDistances = calculator.distanceMatrixRangeScaled(itemsByPriceDesc)
////    val zDistances = calculator.distanceMatrixZNormScaled(itemsByPriceDesc)
//
//    def printStats(name : String, d: AlphaDistanceCalculator) {
//      println("Distance: " + name)
//      println("len: " + d.matrix.length + " mean:" + d.mean + " sd:" +d.sd + " variance:" + d.variance + " max:" + d.max + " min:" + d.min)
//      println()
//    }
//
//    printStats("Simple", distances)
//    printStats("Range scaled distance", rDistances)
//    printStats("Norm Z scaled distance", zDistances)

//    println("After scale: "+ resArray.length + " cnt: " + count + " mean:" + mean + " sd:" +sd + " variance: " + matrix.var)

//    for (i <- 0 until resArray.length) {
//      for (j <- i until resArray(0).length if resArray(i)(j) > 0.0001) {
//        val sim = resArray(i)(j)
//        val a = itemsByPriceDesc(i).guid
//        val b = itemsByPriceDesc(j).guid
//        if (aboveSD)
//          println(f"[$aboveSD%s] $i%d-$j%d $sim%6.4f $a%s -> $b%s" )
//      }
//    }
//
//    for (i <- 0 until resArray.length) {
//
//    }
  }

}
