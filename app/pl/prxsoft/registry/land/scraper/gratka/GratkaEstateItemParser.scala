package pl.prxsoft.registry.land.scraper.gratka

import pl.prxsoft.registry.land.scraper.{SiteParser, EstateItemParser}
import pl.prxsoft.registry.land.ExtLandProperty
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 21.08.2013
 * Time: 22:27
 */
class GratkaEstateItemParser extends EstateItemParser {

  val startBasicData = """id="dane-podstawowe""""
  val stockPattern = """<span>Piętro</span>(\w+)<""".r
  val roomsPattern = """<span>Liczba pokoi</span>(\w+)<""".r
  val stockNumPattern = """<span>Liczba pięter</span>(\w+)<""".r
  val builtYearPattern = """<span>Rok budowy</span>(\d+)<""".r
  

  def extractEstate(resOpt: Option[String]): Option[ExtLandProperty] = {
    resOpt match {
      case Some(str) => {
        val divBasicInfo = str.indexOf(startBasicData)

        if (divBasicInfo < 0) {
          None
        } else {
          val matchStr = str.substring(divBasicInfo)

          val rooms = roomsPattern findFirstMatchIn matchStr match {
            case Some(roomsPattern(room)) if room matches """\d+""" => room.trim.toInt
            case _ => -1
          }

          val stock = stockPattern findFirstMatchIn matchStr match {
            case Some(stockPattern(stock)) if stock matches """\d+""" => stock.trim.toInt
            case Some(m) if m.group(1) == "parter" => 0
            case _ => -1
          }

          val stockTotal = stockNumPattern findFirstMatchIn matchStr match {
            case Some(stockNumPattern(stock)) if stock matches """\d+""" => stock.trim.toInt
            case Some(m) if m.group(1) == "parter" => 0
            case _ => -1
          }

          val builtYear = builtYearPattern findFirstMatchIn matchStr match {
            case Some(builtYearPattern(year)) => year.toInt
            case _ => -1
          }

          Some(ExtLandProperty(site, "", "", null, BigDecimal(-1), BigDecimal(-1), rooms, stock, stockTotal, builtYear, None, false))
        }
      }
      case None => None
    }
  }

  def scrapPage(guid: String) = {
    SiteParser.getPage(guid).map { str => extractEstate(Some(str)) }
  }
}
