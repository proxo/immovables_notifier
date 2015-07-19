package models

import play.api.db.slick.Config.driver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 11.09.2013
 * Time: 23:07
 *
 */
case class User(email: String, password: String)

object User extends Table[User]("users") {
  implicit val getEstateResults =
    GetResult({
      r => User(r.nextString, r.nextString)
    })

  def email = column[String]("email", O.PrimaryKey)


  def password = column[String]("password")

  def * = email  ~ password <>(User.apply _, User.unapply _)

  /**
   * Retrieve all users.
   */
  def findAll(implicit session: Session) = Q.queryNA[User]("select * from users").list()

  /**
   * Authenticate a User.
   */
  def authenticate(email: String, password: String)(implicit session: Session) : Option[User] = {
    import Q.interpolation
    sql"select * from users where email = $email and password = $password".as[User].firstOption
  }
}