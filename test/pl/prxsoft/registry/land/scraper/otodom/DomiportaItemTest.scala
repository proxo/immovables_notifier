package pl.prxsoft.registry.land.scraper.otodom

import org.specs2.control.Debug
import org.specs2.specification.Tags
import org.specs2.mutable.Specification
import pl.prxsoft.registry.land.scraper.SiteParser
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 01.12.2013
 * Time: 14:59
 *
 */
class DomiportaItemTest extends Specification with Tags with Debug {

  "Domiporta site scrapper must parse" should {
    "Price and area" in {
      tag("itemparser")

      // given
      val link = "http://warszawa.domiporta.pl/nieruchomosci/sprzedam-mieszkanie-warszawa-ursynow-al-komisji-edukacji-narodowej-180m2/131196373"
      val estateItemParser = new pl.prxsoft.registry.land.scraper.domiporta.DomiportaItemParser()

      // when
      val futureResult = estateItemParser.scrapPage(link)

      // then

      futureResult.onComplete {
        case Success(Some(landProperty)) =>
          landProperty must not be null
          landProperty.guid.pp("guid=") must be equalTo link
          landProperty.price.pp("price=") must be equalTo BigDecimal(1235000)
          landProperty.area.pp("area=") must be equalTo BigDecimal(180.0)
          landProperty.stock must be equalTo 5
          landProperty.totalStocks must be equalTo 5
          landProperty.builtYear must be equalTo 2000
        case Failure(f) =>
          f.printStackTrace()
          failure(s"Exception occcured: $f")
      }
    }
  }

}
