package pl.prxsoft.registry.land.scraper.domiporta

import pl.prxsoft.registry.land.scraper.{SiteParser, EstateItemParser}
import scala.concurrent.Future
import pl.prxsoft.registry.land.ExtLandProperty
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.jsoup.nodes.Element
import collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import play.Logger

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 15.03.2014
 * Time: 23:04
 */
class DomiportaItemParser extends EstateItemParser {

  def parsePrice(priceText: String) = {
    val nbsp = "\u00A0"
    val num: String = priceText.replaceAll(" ", "").replaceAll("&nbsp;", "").replaceAll(nbsp,"").trim
    try {
      BigDecimal(num)
    } catch {
      case e: Exception =>
        Logger.error(s"Parse error for value: $num")
        e.printStackTrace()
        throw e
    }

  }

  val UnknownValue: BigDecimal = BigDecimal(-1)

  val elemOpisText = (elem : Element) => elem.select("div.Opis").first().text().trim
  val elemOpisToInt = (elem: Element) => {
    val text = elemOpisText(elem)
    if (text == "parter") 0 else text.toInt
  }

  def parseDataItem(elem: Element, property: ExtLandProperty): ExtLandProperty = {
    val select: Elements = elem.select("div.Tytul")
    val title = if (select != null) Some(select.text.toLowerCase.trim) else None

    title match {
      case Some("liczba pokoi:") =>
        val rooms = elemOpisToInt(elem)
        property.copy(rooms = rooms)
      case Some("piętro:") =>
        val stock = elemOpisToInt(elem)
        property.copy(stock = stock)
      case Some("ilość pięter w budynku:") =>
        property.copy(totalStocks = elemOpisToInt(elem))
      case Some("rok budowy:") =>
        property.copy(builtYear = elemOpisToInt(elem))
      case Some("powierzchnia całkowita:") =>
        val text = elemOpisText(elem)
        val area = text.split(" ")(0).replaceAll(",",".").trim
        property.copy(area = BigDecimal(area))
      case Some(_) =>
        property
      case None =>
        property
    }
  }

  def extractEstate(guid: String, some: String) : Option[ExtLandProperty]= {
    val siteSoup = Jsoup.parse(some)
    val priceSpan = siteSoup.select("div.Cena span").first()
    val price = if (priceSpan != null) parsePrice(priceSpan.text()) else UnknownValue
    var landProperty = new ExtLandProperty(site, guid, price)
    val basicData = siteSoup.select("div.InformacjePodstawoweDane").select("div.InformacjePodstawoweDaneElement")
    for (data <- basicData) {
      landProperty = parseDataItem(data, landProperty)
    }
    Some(landProperty)
  }

  override val site = "domiporta"

  def scrapPage(guid: String): Future[Option[ExtLandProperty]] = {
    SiteParser.getPage(guid).map { str => extractEstate(guid, str) }
  }

}
