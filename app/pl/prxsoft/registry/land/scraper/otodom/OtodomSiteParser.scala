package pl.prxsoft.registry.land.scraper.otodom

import pl.prxsoft.registry.land.scraper.{EstateItemParser, SiteParser}
import pl.prxsoft.registry.land.ExtLandProperty
import scala.concurrent.{ExecutionContext, Future}
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import ExecutionContext.Implicits.global
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import collection.JavaConversions._

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 17.11.2013
 * Time: 14:08
 *
 */
class OtodomSiteParser(url : String) extends SiteParser {

  val linkPrefix = "http://www.otodom.pl"
  val datePattern = new SimpleDateFormat("dd.MM.yyyy")
  val NA = BigDecimal(-1)

  //piętro: <strong>(\w+)</strong> z (\d+)
  val stockNumPattern = """(\w+)\s+z\s+(\d+)""".r
  val builtYearPattern = """(\w+)\s+z\s+(\d+)""".r

  def extractStock(option: Option[Element]) : (Int, Int) = option match {
    case Some(n) => {
      val text = n.text
      stockNumPattern findFirstMatchIn text match {
        case Some(stockNumPattern(stock, totalStocks)) if stock matches """\d+""" => (stock.toInt, totalStocks.toInt)
        case Some(m) if m.group(1) == "parter" => (0, m.group(2).toInt)
        case _ => (-1, -1)
      }

    }
    case None => (-1, -1)
  }

  def extractBuiltYear(x: Option[Element]) = x match {
    case Some(node) => {
      val text = node.text
      builtYearPattern findFirstMatchIn text match {
        case Some(builtYearPattern(year)) => year.toInt
        case _ => -1
      }
    }
    case None => -1
  }

  def parseEstate(node: Element) : Option[ExtLandProperty] = {
    val aElement = node.select("a").first()
    val a = aElement.attr("href")
    val guid = linkPrefix + a
    val numberNodeOpt = nodeWithClass(node, "div", "od-listing_item-numbers")

    if (numberNodeOpt.isEmpty)
      None
    else {
      val numberNode = numberNodeOpt.get
      val pubDate = firstNode(node, "time").map(s => datePattern.parse(s.text.trim)).getOrElse(new Date)

      val price = nodeWithClass(numberNode, "strong","od-listing_item-price")
        .map(s => BigDecimal(s.text.replaceAll("zł","").replaceAll(" ","").replaceAll(",",".").trim))
        .getOrElse(BigDecimal(-1))

      val area = nthNode(numberNode, "strong", 1)
        .map(n => BigDecimal(n.text.substring(0, n.text.indexOf('m')).trim) )
        .getOrElse(BigDecimal(-1))

      val ulParamsNode = nodeWithClass(node, "ul","od-listing_item-parameters")
      val rooms = ulParamsNode
        .map(n =>  firstNode(n, "strong").map(r => r.text.split(" ")(0).toInt).getOrElse(-1) )
        .getOrElse(-1)

      val (stock, totalStocks) = extractStock(ulParamsNode.flatMap(n => nthNode(n, "li", 1)))
      val builtYear = extractBuiltYear(ulParamsNode.flatMap(n => nthNode(n, "li", 2)))
      val title = nodeWithClass(node, "p", "od-listing_item-summary").map(n => n.text.trim).getOrElse("")

      Some(
        ExtLandProperty(name, guid, title, new java.sql.Date(pubDate.getTime), area, price, rooms, stock, totalStocks, builtYear, None, false)
      )
    }
  }


  def nodeWithClass(node : Element, nodeName: String, className: String) : Option[Element] = {
    Option(node.select(nodeName + "." + className).first())
  }

  def firstNode(node : Element, nodeName: String) = {
    nthNode(node, nodeName, 0)
  }

  def nthNode(node: Element, nodeName: String, num: Int) = {
    val selectedNodes = node.select(nodeName)
    if (num < selectedNodes.size())
      Some(selectedNodes.get(num))
    else
      None
  }

  def run(url : String) : Future[IndexedSeq[ExtLandProperty]] = {
    val fs = SiteParser.getPage(url)

    val estatesF = fs.map { siteStr =>
      val doc =  Jsoup.parse(siteStr)
      val estateItems = doc.body().select("article.od-listing_item")
      val items = for (item <- estateItems)
        yield parseEstate(item)
      items.toIndexedSeq
    }

    val estates = estatesF.map(items => items.filter(_.isDefined).map(_.get))
    estates
  }

  override val name = "otodom"

  def itemParser: Option[EstateItemParser] = None
}
