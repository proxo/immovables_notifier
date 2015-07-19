package pl.prxsoft.registry.land.scraper.gratka

import java.util.Date
import pl.prxsoft.registry.land.{ExtLandProperty, LandProperty}
import pl.prxsoft.registry.land.scraper.{EstateItemParser, SiteParser}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import collection.JavaConversions._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 08.07.2013
 * Time: 23:32
 */
class GratkaSiteParser(val urlAddress: String) extends SiteParser {

  var lastRun = new Date

  override val name = "gratka"

  override def toString = {
    s"RSS parser for address: ${urlAddress}"
  }

  override def run(url : String) = {
    val siteStrFuture  = SiteParser.getPage(url)

    for {
      siteStr <- siteStrFuture
    } yield {
      val items = Jsoup.parse(siteStr).select("item")
      items.map(parseItemRss(_)).toIndexedSeq
    }
  }

  private def parseItemRss(node: Element): ExtLandProperty = {
    val title = node.select("title").first()
    val link = node.select("guid").first()
    val desc = node.select("description").first()
    val descText = desc.text
    val pubDate = node.select("pubDate").first()
    val guid = (node.select("guid").first()).text.trim

    val spaceString: String = """<b>Powierzchnia: </b>"""
    val landIdx = descText.indexOf(spaceString)
    val subIdx = descText.indexOf("<sup>", landIdx)
    val landValue = descText.substring(landIdx + spaceString.length, subIdx).trim
    val roomsString = """<b>Liczba pokoi: </b>"""
    val roomsIdx = descText.indexOf(roomsString)
    val roomsValue : Integer =
      if (roomsIdx < 0)
        -1
      else
        Integer.valueOf(descText.substring(roomsIdx + roomsString.length, descText.indexOf("<br", roomsIdx + 1)).trim)

    val landBigValue = BigDecimal(landValue.replace("m", "").replace(",", "."))

    def extractPriceFromText(text: String) = {
      val priceIdx = text.indexOf( """<b>Cena""")
      val priceIdx2 = text.indexOf(">", priceIdx + 3)
      val nextTagIdx = text.indexOf("<", priceIdx2 + 1)
      val priceText = text.substring(priceIdx2 + 1, nextTagIdx - 1)
      val priceValue = priceText.substring(0, priceText.indexOf("PLN")).replaceAll(" ", "").trim().replaceAll(",", ".")
      BigDecimal(priceValue)
    }

    val priceValue = extractPriceFromText(descText)

    val trim: String = title.text.trim.replace("<![CDATA[","").replace("]]>","")
    new LandProperty(guid,
      trim, new java.sql.Date(dateFormat.parse(pubDate.text).getTime), landBigValue,
      priceValue, roomsValue).toExtLandProperty
  }

  def itemParser: Option[EstateItemParser] = Some(new GratkaEstateItemParser)
}
