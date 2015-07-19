package pl.prxsoft.registry.land.scraper

import pl.prxsoft.registry.land.ExtLandProperty
import pl.prxsoft.registry.land.scraper.gratka.GratkaSiteParser
import scala.concurrent.{Promise, Future}
import pl.prxsoft.registry.land.scraper.otodom.OtodomSiteParser
import com.ning.http.client.{AsyncCompletionHandler, AsyncHttpClient, Response}
import java.text.SimpleDateFormat
import java.util.Locale
import pl.prxsoft.registry.land.scraper.domiporta.DomiportaSiteParser

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 03.11.2013
 * Time: 13:37
 */
trait SiteParser {
  val dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
  val UndefinedValue = BigDecimal(-1)

  def run(url : String) : Future[IndexedSeq[ExtLandProperty]]

  def name : String

  def needsItemParsing: Boolean = itemParser match {
    case Some(x) => true
    case _ => false
  }

  def itemParser: Option[EstateItemParser]
}


object SiteParser {
  val client = new AsyncHttpClient()


  def build(siteName: String, url : String): SiteParser = siteName match {
    case "gratka" => new GratkaSiteParser(url)
    case "otodom" => new OtodomSiteParser(url)
    case "domiporta" => new DomiportaSiteParser
  }

  def apply(siteName: String, url : String) = build(siteName, url)

  implicit def block2completionHandler[T](block: Response => T) = new AsyncCompletionHandler[T]() {
     def onCompleted(response: Response) = block(response)
   }

  def getPage(url: String) = {
     val p = Promise[String]()
     client.prepareGet(url).execute {
       response: Response =>
         p.success(response.getResponseBody("UTF-8"))
     }
     p.future
   }
}