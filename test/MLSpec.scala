import actors.MLActor
import java.sql.Date
import models.Estates
import org.specs2.mutable.Specification
import pl.prxsoft.registry.land.ExtLandProperty
import play.api.db.slick._
import play.api.test.WithApplication

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 12.09.2013
 * Time: 23:18
 *
 */
class MLSpec extends Specification {

  "SIM calculator" should {
    "should cluster two" in new WithApplication {
      val guid1 = "http://dom.gratka.pl/tresc/397-46792791-mazowieckie-warszawa-mokotOw-sadyba-naleczowska.html"
      val guid2 = "http://dom.gratka.pl/tresc/397-46792569-mazowieckie-warszawa-mokotOw.html"

      DB.withSession {
        implicit s: Session =>
          val e1 = Estates.findByGUID(guid1).map { e => e}
          val e2 = Estates.findByGUID(guid2).map { e => e}

          println("e1 = " + e1)
          val (zDistMatrix, simMatrix) = MLActor.calculateKNN(List(e1.get, e2.get))
          println("dist Matrix")
          println(zDistMatrix.toString)
          println("sim Matrix")
          println(simMatrix.toString)
      }
    }
  }
}