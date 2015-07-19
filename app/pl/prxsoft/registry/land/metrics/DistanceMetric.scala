package pl.prxsoft.registry.land.metrics

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 14.07.2013
 * Time: 23:16
 */
trait DisSimCalculator[T] {
  val mean : Double
  val sd : Double
  val variance : Double
  val max : Double
  val min : Double
}
