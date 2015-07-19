/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Time: 20:33
 */
package test

import org.specs2.mutable._

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.test._
import play.api.test.Helpers._
import models._
import pl.prxsoft.registry.land.ExtLandProperty
import java.sql.Date

/**
  * test the kitty cat database
  */
class DBSpec extends Specification {

  "DB" should {
    "work as expected" in new WithApplication {

      DB.withSession{ implicit s:Session =>
        s.withTransaction {
          val testEstates = Seq(
            new ExtLandProperty("some1", "full house1", BigDecimal(0)),
            new ExtLandProperty("some2", "full house2", BigDecimal(1))
          )

          Estates.insertAll(testEstates: _*)
          Query(Estates).list.size must equalTo(2)
          s.rollback
        }
      }
    }

    "findByPK should return single element" in new WithApplication() {
      DB.withSession { implicit s: Session =>
        val testEstates = Seq(
          new ExtLandProperty("some1", "full house1", BigDecimal(0)),
          new ExtLandProperty("some2", "full house2", BigDecimal(1))
        )

        Estates.insertAll(testEstates: _*)
        val res = Estates.findByGUID("some1")
        res must not be null
        res.get.guid must equalTo("some1")
      }

    }

    "findAll newer than 2 days" in new WithApplication() {
      DB.withSession { implicit s: Session =>
        val estates = Estates.findNewerThanDays(4)

        estates.size must be equalTo(3)
      }

    }


    "select the correct testing db settings by default" in new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      DB.withSession{ implicit s:Session =>
        s.conn.getMetaData.getURL must startWith("jdbc:h2:mem:play-test")
      }
    }


    "use the default db settings when no other possible options are available" in new WithApplication {
      DB.withSession{ implicit s:Session =>
        s.conn.getMetaData.getURL must equalTo("jdbc:postgresql://localhost:5432/stocks")
      }
    }
  }

}
