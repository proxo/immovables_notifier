package actors

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 01.12.2013
 * Time: 19:22
 * To change this template use File | Settings | File Templates.
 */
import akka.actor.{ Actor, DeadLetter, Props }

class Listener extends Actor {
  def receive = {
    case d: DeadLetter ⇒ println(d)
  }
}
