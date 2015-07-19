import org.specs2.control.Debug
import org.specs2.mutable.Specification
import org.specs2.specification.Tags
import pl.prxsoft.registry.land.scraper.gratka.{GratkaEstateItemParser, GratkaSiteParser}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global


/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 18.08.2013
 * Time: 12:00
 */
class RssParserSpec extends Specification with Tags with Debug {
  val sampleData ="""<div>
                    |
                    |                  <h4>Oferta</h4>
                    |
                    |          <ul>
                    |
                    |              <li><span>Numer oferty</span>gratka-SOL-MS-69531-49</li>
                    |
                    |              <li><span>Cena</span>650 000 PLN (6 989 PLN/m<sup>2</sup>)&nbsp;&nbsp;<a class="podgladDeaktywuj" data-tracker="/onclick/karta-ogloszenia/zakladka-opis/negocjuj-cene/" href="#formularz-kontakt">Negocjuj cenę</a></li>
                    |                                      <li class="reklamaRata">
                    |                <span>Kredyt</span>
                    |                <script type="text/javascript">
                    |                  GR("wstawReklame","button_NxN4_tekstowy","","","","","b");
                    |                </script>
                    |              </li>
                    |                      </ul>
                    |                                <h4>Mieszkanie</h4>
                    |
                    |          <ul>
                    |
                    |              <li><span>Powierzchnia</span>93 m<sup>2</sup></li>
                    |
                    |              <li><span>Piętro</span>parter</li>
                    |
                    |              <li><span>Liczba pokoi</span>3</li>
                    |
                    |              <li><span>Liczba poziomów</span>jednopoziomowe</li>
                    |                                  </ul>
                    |                                <h4>Budynek</h4>
                    |
                    |          <ul>
                    |
                    |              <li><span>Typ budynku</span>kamienica</li>
                    |
                    |              <li><span>Liczba pięter</span>4</li>
                    |
                    |              <li><span>Materiał</span>cegła</li>
                    |
                    |              <li><span>Rok budowy</span>1938</li>
                    |                                  </ul>
                    |                                </div>
                    |      <div class="clearOver textP linki hide-for-small">
                    |    <b>Więcej o ofercie:</b>
                    |    <ul class="linkiDynamiczne">
                    |""".stripMargin

  val sampleData2WithBuiltYear =
    """</ul>
      |
      |  <div id="dane-podstawowe" class="boks listaInformacje">
      |    <h3 class="show-for-small">Dane podstawowe</h3>
      |    <div>
      |
      |                  <h4>Oferta</h4>
      |
      |          <ul>
      |
      |              <li><span>Numer oferty</span>gratka-72285</li>
      |
      |              <li><span>Cena</span>595 000 PLN (7 169 PLN/m<sup>2</sup>)&nbsp;&nbsp;<a class="podgladDeaktywuj" data-tracker="/onclick/karta-ogloszenia/zakladka-opis/negocjuj-cene/" href="#formularz-kontakt">Negocjuj cenę</a></li>
      |                                      <li class="reklamaRata">
      |                <span>Rata</span>
      |                <script type="text/javascript">
      |                  GR("wstawReklame","button_NxN4_tekstowy","","","","","b");
      |                </script>
      |              </li>
      |                      </ul>
      |                                <h4>Mieszkanie</h4>
      |
      |          <ul>
      |
      |              <li><span>Powierzchnia</span>83 m<sup>2</sup></li>
      |
      |              <li><span>Piętro</span>2</li>
      |
      |              <li><span>Liczba pokoi</span>4</li>
      |
      |              <li><span>Liczba poziomów</span>jednopoziomowe</li>
      |
      |              <li><span>Stan mieszkania</span>do odświeżenia</li>
      |
      |              <li><span>Głośność</span>ciche</li>
      |
      |              <li><span>Powierzchnia dodatkowa</span>balkon, piwnica</li>
      |                                  </ul>
      |                                <h4>Budynek</h4>
      |
      |          <ul>
      |
      |              <li><span>Typ budynku</span>blok</li>
      |
      |              <li><span>Liczba pięter</span>2</li>
      |
      |              <li><span>Materiał</span>rama H</li>
      |
      |              <li><span>Rok budowy</span>1980</li>
      |                                  </ul>
      |                                <h4>Dodatkowe zalety</h4>
      |                  <p>internet</p>
      |                                </div>
      |      <div class="clearOver textP linki hide-for-small">
      |""".stripMargin


  val sampleDataWithStockZero =
    """
      |
      |  <div id="dane-podstawowe" class="boks listaInformacje">
      |    <h3 class="show-for-small">Dane podstawowe</h3>
      |    <div>
      |
      |                  <h4>Oferta</h4>
      |
      |          <ul>
      |
      |              <li><span>Numer oferty</span>gratka-BRZ-MS-100812-86</li>
      |
      |              <li><span>Cena</span>660 000 PLN (8 049 PLN/m<sup>2</sup>)&nbsp;&nbsp;<a class="podgladDeaktywuj" data-tracker="/onclick/karta-ogloszenia/zakladka-opis/negocjuj-cene/" href="#formularz-kontakt">Negocjuj cenę</a></li>
      |                                      <li class="reklamaRata">
      |                <span>Rata</span>
      |                <script type="text/javascript">
      |                  GR("wstawReklame","button_NxN4_tekstowy","","","","","b");
      |                </script>
      |              </li>
      |                      </ul>
      |                                <h4>Mieszkanie</h4>
      |
      |          <ul>
      |
      |              <li><span>Forma własności</span>spółdzielcze własnościowe z KW</li>
      |
      |              <li><span>Powierzchnia</span>82 m<sup>2</sup></li>
      |
      |              <li><span>Piętro</span>parter</li>
      |
      |              <li><span>Liczba pokoi</span>4</li>
      |
      |              <li><span>Liczba poziomów</span>jednopoziomowe</li>
      |
      |              <li><span>Stan mieszkania</span>do remontu</li>
      |
      |              <li><span>Głośność</span>ciche</li>
      |
      |              <li><span>Kuchnia</span>oddzielna</li>
      |
      |              <li><span>Stan instalacji</span>częściowo wymieniona</li>
      |
      |              <li><span>Powierzchnia dodatkowa</span>loggia, piwnica</li>
      |                                  </ul>
      |                                <h4>Budynek</h4>
      |
      |          <ul>
      |
      |              <li><span>Typ budynku</span>blok</li>
      |
    """.stripMargin

  val parser = new GratkaSiteParser("http://dom.gratka.pl/mieszkania-sprzedam/lista/rss/,warszawa,mokotow%5Ewilanow,550000,700000,1d,80,dz,co,cd,od,mo.html")
  var itemParser = new GratkaEstateItemParser
  val link = "http://dom.gratka.pl/mieszkania-sprzedam/lista/rss/,warszawa,mokotow%5Ewilanow,550000,700000,1d,80,dz,co,cd,od,mo.html"

  "Gratka parser should parse link and return not emptt" should {
    parser.run(link) must not be empty
  }

  "Return not empty result" should {
    tag("scrapper")
    val res = itemParser.extractEstate(Some(sampleData))
    res.get must not be None
  }

  "Estate item scraper parses" should {
    "stock number and returns it" in {
      itemParser.extractEstate(Some(sampleData)) match {
        case Some(e) => e.stock must be_==(0)
        case None => failure("None returned")
      }
    }

    "scrapp total stock number and return it" in {
      itemParser.extractEstate(Some(sampleData)) match {
        case Some(e) => e.totalStocks must be_==(3) and e.builtYear === -1
        case None => failure("None returned")
      }
    }

    "scrapp Built Year and return" in {
      itemParser.extractEstate(Some(sampleData2WithBuiltYear)) map {
        e => e.pp.builtYear === 1980
      } getOrElse failure("Parsing date failed")
    }


    "scrapp 0 stock level and replace string" in {
      itemParser.extractEstate(Some(sampleDataWithStockZero)) map {
        e => e.pp.stock must be equalTo (0)
      } getOrElse failure("Parsing 'parter' failed")
    }
  }

  "Site scrapper" should {
    "Fetch and scrap gratka.pl" in {
      val url = "http://dom.gratka.pl/tresc/397-47681968-mazowieckie-warszawa-mokotow.html"
      val f = itemParser.scrapPage(url)
      f.onComplete {
        case Success(s) => {
          s.get.pp.builtYear must be_==(1938)
          s.get.pp.stock === 0
        }
        case Failure(f) =>  failure("no estate returned by future")
      }
    }
  }
}
