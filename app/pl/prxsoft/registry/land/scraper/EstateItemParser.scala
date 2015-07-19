package pl.prxsoft.registry.land.scraper

import pl.prxsoft.registry.land.ExtLandProperty
import scala.concurrent.{Promise, Future}
import com.ning.http.client.{AsyncCompletionHandler, Response, AsyncHttpClient}

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 03.11.2013
 * Time: 21:41
 */
trait EstateItemParser {
  def scrapPage(guid : String) : Future[Option[ExtLandProperty]]
  def site = "gratka"
}