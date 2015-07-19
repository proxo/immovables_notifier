package models

import play.api.db.slick.Config.driver.simple._
import scala.slick.jdbc.{StaticQuery => Q}
import scala.slick.jdbc.GetResult
import java.sql.Date

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 08.01.2014
 * Time: 22:19
 *
 */
case class ParseSet(name: String, desc: String, active: Boolean, createdAt: Date)
case class ParseSite(name : String, url: String, setName: String, orderNo: Option[Int], createdAt: Date)

/**
 *
 */
object ParseSetTable extends Table[ParseSet]("parse_sets") {
  implicit val getParseSiteResult =
      GetResult({r => r.nextInt; ParseSet(r.nextString,r.nextString, r.nextBoolean, r.nextDate)})

  def id = column[Int] ("id", O.PrimaryKey)
  def name = column[String] ("name")
  def desc = column[String] ("description")
  def active = column[Boolean]("active")
  def createdAt = column[Date]("created_at")

  def * = name ~ desc  ~ active ~ createdAt <> (ParseSet.apply _, ParseSet.unapply _)
  def findAllActive(implicit session: Session) : List[ParseSet] = {
    Q.queryNA[ParseSet](s"select * from PARSE_SETS where active='y' order by created_at").list()
  }
}

object ParseSiteTable extends Table[ParseSite]("parse_sites") {
  implicit val getParseSiteResult =
    GetResult({r => r.nextInt; ParseSite(r.nextString,r.nextString, r.nextString(), r.nextIntOption, r.nextDate)})

  def id = column[Int]("id", O.PrimaryKey)
  def name = column[String]("name")
  def url = column[String]("url")
  def setName = column[String]("set_name")
  def orderNo = column[Option[Int]]("order_no")
  def createdAt = column[Date]("created_at")

  def * = name ~ url ~ setName ~ orderNo ~ createdAt <> (ParseSite.apply _, ParseSite.unapply _)

  def findBySetName(set_name: String)(implicit session: Session): List[ParseSite] = {
    Q.queryNA[ParseSite](s"select * from parse_sites where set_name = '$set_name'").list()
  }
}

