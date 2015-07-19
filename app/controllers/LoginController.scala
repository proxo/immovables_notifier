package controllers

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import models.User

import play.api.db.slick.DB
import play.api.Play.current

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 12.09.2013
 * Time: 22:35
 */
object   LoginController extends Controller {

  trait Secured {
    private def username(request: RequestHeader) = request.session.get("email")

    private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.LoginController.login)

    def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) {
      user =>
        Action(request => f(user)(request))
    }
  }

  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    )
  )

  /**
   * Login page.
   */
  def login = Action {
    implicit request =>
      Ok(views.html.login(loginForm))
  }

  /**
   * Handle login form submission.
   */
  def authenticate = Action {
    implicit request =>
      loginForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.login(formWithErrors)),
        user => {
          DB.withSession {
            implicit s: scala.slick.session.Session =>
              println("authenticating")
              User.authenticate(user._1, user._2).map {
                u =>
                  println("redirecting")
                  Redirect(routes.Application.index).withSession("email" -> u.email)
              }.getOrElse(BadRequest(views.html.login(loginForm)))
          }

        }

      )
  }

  /**
   * Logout and clean the session.
   */
  def logout = Action {
    Redirect(routes.LoginController.login()).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

}
