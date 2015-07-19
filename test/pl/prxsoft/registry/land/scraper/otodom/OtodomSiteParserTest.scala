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
class OtodomSiteParserTest extends Specification with Tags with Debug {

  "Otodom parser should download and scrap items on page" should {
      tag("siteparser")
      // given
      val link = "http://otodom.pl/index.php?mod=listing&source=context&objSearchQuery.OfferType=sell&objSearchQuery.ObjectName=Flat&objSearchQuery.Country.ID=1&objSearchQuery.Province.ID=7&objSearchQuery.District.ID=197&objSearchQuery.CityName=Warszawa&objSearchQuery.Distance=0&objSearchQuery.QuarterName=Mokot%C3%B3w%2C+wilan%C3%B3w&objSearchQuery.StreetName=&objSearchQuery.LatFrom=0&objSearchQuery.LatTo=0&objSearchQuery.LngFrom=0&objSearchQuery.LngTo=0&objSearchQuery.PriceFrom=&objSearchQuery.PriceTo=760+000&objSearchQuery.PriceCurrency.ID=1&objSearchQuery.PriceM2From=&objSearchQuery.PriceM2To=&objSearchQuery.PriceM2Currency.ID=1&objSearchQuery.AreaFrom=82&objSearchQuery.AreaTo=&objSearchQuery.FlatRoomsNumFrom=3&objSearchQuery.FlatRoomsNumTo=4&objSearchQuery.FlatFloorFrom=&objSearchQuery.FlatFloorTo=&objSearchQuery.FlatBuildingType=&objSearchQuery.BuildingMaterial=&objSearchQuery.FlatFloorsNoFrom=&objSearchQuery.FlatFloorsNoTo=&objSearchQuery.BuildingYearFrom=&objSearchQuery.BuildingYearTo=&objSearchQuery.MarketType=&objSearchQuery.CreationDate=1&objSearchQuery.Description=&objSearchQuery.offerId=&objSearchQuery.Orderby=default&resultsPerPage=100&Search=Search&Location="
      val parser = SiteParser("otodom", link)
      // when

      val estateItems = parser.run(link)
      // then

      estateItems.onComplete({
        case Success(items) => items must be not empty
        case Failure(f) => failure(s"Exception occcured: $f")
      })
    }

  "Otodom parser should download and scrap items on page returning all items" should {
      tag("siteparser")
      // given
      val link = "http://otodom.pl/index.php?mod=listing&source=context&objSearchQuery.OfferType=sell&objSearchQuery.ObjectName=Flat&objSearchQuery.Country.ID=1&objSearchQuery.Province.ID=7&objSearchQuery.District.ID=197&objSearchQuery.CityName=Warszawa&objSearchQuery.Distance=0&objSearchQuery.QuarterName=Mokot%C3%B3w%2C+wilan%C3%B3w&objSearchQuery.StreetName=&objSearchQuery.LatFrom=0&objSearchQuery.LatTo=0&objSearchQuery.LngFrom=0&objSearchQuery.LngTo=0&objSearchQuery.PriceFrom=&objSearchQuery.PriceTo=760+000&objSearchQuery.PriceCurrency.ID=1&objSearchQuery.PriceM2From=&objSearchQuery.PriceM2To=&objSearchQuery.PriceM2Currency.ID=1&objSearchQuery.AreaFrom=82&objSearchQuery.AreaTo=&objSearchQuery.FlatRoomsNumFrom=3&objSearchQuery.FlatRoomsNumTo=4&objSearchQuery.FlatFloorFrom=&objSearchQuery.FlatFloorTo=&objSearchQuery.FlatBuildingType=&objSearchQuery.BuildingMaterial=&objSearchQuery.FlatFloorsNoFrom=&objSearchQuery.FlatFloorsNoTo=&objSearchQuery.BuildingYearFrom=&objSearchQuery.BuildingYearTo=&objSearchQuery.MarketType=&objSearchQuery.CreationDate=1&objSearchQuery.Description=&objSearchQuery.offerId=&objSearchQuery.Orderby=default&resultsPerPage=100&Search=Search&Location="
      val parser = SiteParser("otodom", link)
      // when

      val estateItems = parser.run(link)
      // then

      estateItems.onComplete({
        case Success(items) => items must have size(34)
        case Failure(f) => failure(s"Exception occcured: $f")
      })
    }

}
