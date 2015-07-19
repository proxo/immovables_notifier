package pl.prxsoft.registry.land.scraper.domiporta

import pl.prxsoft.registry.land.scraper.{EstateItemParser, SiteParser}
import scala.concurrent.Future
import pl.prxsoft.registry.land.ExtLandProperty
import org.jsoup.Jsoup
import collection.JavaConversions._
import org.jsoup.nodes.Element
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import play.Logger

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 15.03.2014
 *
 * Time: 22:57
 */
class DomiportaSiteParser extends SiteParser {

  def run(url: String): Future[IndexedSeq[ExtLandProperty]] = {
    val siteStrFuture  = SiteParser.getPage(url)
        for {
          siteStr <- siteStrFuture
        } yield {
          val items = Jsoup.parse(siteStr).select("item")
          items.map(parseItemRss).toVector
        }
  }

  private def parseItemRss(node: Element): ExtLandProperty = {
    val title = node.select("title").first().text()
    val guid = node.select("guid").first().text().trim()
    val pubDateText = node.select("pubDate").first().text()
    val pubDate = new java.sql.Date(dateFormat.parse(pubDateText).getTime)
    Logger.warn(s"Domiporta parsing guid: $guid")
    new ExtLandProperty(name, guid, title, pubDate,UndefinedValue, UndefinedValue, -1, -1, -1,-1,None,false)
  }

  override val itemParser: Option[EstateItemParser] = Some(new DomiportaItemParser)
  override val name = "domiporta"
}
