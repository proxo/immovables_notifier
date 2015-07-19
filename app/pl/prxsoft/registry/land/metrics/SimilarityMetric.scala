package pl.prxsoft.registry.land.metrics

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 24.07.2013
 * Time: 23:18
 */
trait SimilarityMetric {
  def calc(a: Double, b: Double) : Double
}
