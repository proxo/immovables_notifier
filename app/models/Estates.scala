package models

import pl.prxsoft.registry.land.ExtLandProperty
import play.api.db.slick.Config.driver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 18.08.2013
 * Time: 23:08
 */
object Estates extends Table[ExtLandProperty]("estates") {

  implicit val getEstateResults =
    GetResult({
      r => r.nextInt;
        ExtLandProperty(r.nextString,
          r.nextString,
          r.nextString,
          r.nextDate,
          r.nextBigDecimal, r.nextBigDecimal, r.nextInt, r.nextInt, r.nextInt, r.nextInt,
        { r.nextTimestamp(); r.nextTimestamp(); r.nextStringOption() },
          r.nextBoolean
        )
    }
    )

  def id = column[Int]("id", O.PrimaryKey)

  def site = column[String]("site")

  def guid = column[String]("guid")

  def title = column[String]("title")

  def pubDate = column[java.sql.Date]("pub_date")

  def area = column[BigDecimal]("area")

  def price = column[BigDecimal]("price")

  def rooms = column[Int]("rooms")

  def stock = column[Int]("stock")

  def totalStocks = column[Int]("total_stocks")

  def builtYear = column[Int]("built_year")

  def parseSet = column[Option[String]]("set_name")

  def viewed = column[Boolean]("viewed")

  def * = site ~ guid ~ title ~ pubDate ~ area ~ price ~
    rooms ~ stock ~ totalStocks ~ builtYear ~
    parseSet ~ viewed <> (ExtLandProperty.apply _, ExtLandProperty.unapply _)

  def findByGUID(guidIn: String)(implicit session: Session): Option[ExtLandProperty] = {
    Q.queryNA[ExtLandProperty](s"select * from estates where guid = '$guidIn'").firstOption
  }

  def findNewerThanDays(daysBefore: Int)(implicit session: Session) = {
    Q.queryNA[ExtLandProperty]("select * from estates where pub_date > current_date-" + daysBefore + " order by pub_date desc").list()
  }

  def findNewerThanDaysOrMax(daysBefore: Int, maxRows: Int)(implicit session: Session) = {
      Q.queryNA[ExtLandProperty](
        "select * from estates where pub_date > current_date - " + daysBefore + " LIMIT " + maxRows)
        .list()
  }

  def findNewerThanDaysOrMaxWithParseSet(daysBefore: Int, maxRows: Int, parseSet: String)(implicit session: Session) = {
      Q.queryNA[ExtLandProperty](
        "select * from estates where set_name = '" + parseSet + "' and pub_date > current_date - " + daysBefore + " LIMIT " + maxRows)
        .list()
  }

  def markAsViewed(guid: String)(implicit session: Session): Boolean = {
    Q.updateNA(s"update estates set viewed = TRUE where guid='$guid'").first() > 0
  }

}

