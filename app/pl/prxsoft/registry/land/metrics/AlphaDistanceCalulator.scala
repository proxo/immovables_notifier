package pl.prxsoft.registry.land.metrics

import scala.Array._
import pl.prxsoft.registry.land.ExtLandProperty

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 14.07.2013
 * Time: 23:11
 */
case class CalcTrace(val name: String, v1: Double, v2: Double, sv1 : Double, sv2 : Double, dist : Double, sim: Double, alpha : Double) {
   override def toString = s"v1=$v1 v2=$v2 sv1=$sv1 sv2=$sv2 d=$dist alpha=$alpha"
}

class AlphaDistanceCalculator(val alphaFactors: IndexedSeq[Double], var matrix: Array[Array[Double]], var traceMatrix: Array[Array[List[CalcTrace]]])
  extends DisSimCalculator[ExtLandProperty] with WordSimilarityCalc {

  case class CalcData(a : Double, b: Double, sf: AlphaDistanceCalculator#ScaleStrategy, alpha: Double, name: String) {}

  def this(alphaFactors: IndexedSeq[Double]) = this(alphaFactors, ofDim[Double](0, 0), ofDim[List[CalcTrace]](0, 0))

  def calculate(a: ExtLandProperty, b: ExtLandProperty, scaleFunctions: Array[ScaleStrategy])(distFunc: (Double, Double) => Double) = {
    val v1 = a.toVector
    val v2 = b.toVector
    val sf = (0 until v1.size).map(scaleFunctions(_)).toVector
    val names = a.toVectorDesc

    val calcData = (0 until v1.length).map { idx =>
      CalcData(v1(idx), v2(idx),sf(idx),a.weighs(idx),names(idx))
    }
    // find all non negative i.e. not missing values
    val nonNegativeValues = calcData.filter {
      e => e.a >= 0.0 && e.b >= 0.0
    }

    val alphaSum = nonNegativeValues.foldLeft(0.0) { (t, v) => t + v.alpha} + a.titleWeight

    // sum distance
    val (sum, traceList) = nonNegativeValues.foldLeft((0.0, List[CalcTrace]())) {
      (t, v) =>
        val (total, items) = t
        val (sa, sb) = (v.sf.scale(v.a), v.sf.scale(v.b))
        val d = distFunc(sa, sb)
        val alphaW = v.alpha * alphaSum
        (total + (d * alphaW), CalcTrace(v.name, v.a, v.b, sa, sb, d, 0.0, alphaW) :: items)
    }

    val titleDist = 1.0 - similarity(a.title, b.title)
    val titleAlpha = alphaSum  * a.titleWeight
    val trace: CalcTrace = CalcTrace("title", titleDist, titleDist, titleDist, titleDist, titleDist, 0.0, titleAlpha)
    (sum + titleDist * titleAlpha, (trace :: traceList).reverse)
  }

  trait ScaleStrategy {
    def scale(d: Double): Double
  }

  case class ZNormScale(val mean: Double, val sd: Double) extends ScaleStrategy {
    def scale(d: Double) = (d - mean) / sd
  }

  case class RangeScale(val min: Double, val max: Double) extends ScaleStrategy {
    def scale(d: Double) = (d - min) / (max - min)
  }

  object IdentityScale extends ScaleStrategy {
    def scale(d: Double): Double = d
  }

  private def scaleFactors(landProperties: Seq[ExtLandProperty])(extFunc: ExtLandProperty => Double): ZNormScale = {
    val size: Int = landProperties.size
    val mean = landProperties.foldLeft(0.0) {
      _ + extFunc(_)
    } / size
    val variance = landProperties.foldLeft(0.0) {
      (t, v) => t + math.pow(extFunc(v) - mean, 2)
    } / (size - 1)
    val sd = math.sqrt(variance)
    ZNormScale(mean, sd)
  }

  def gaussian(dist: Double, sigma: Double = 10.0) = math.exp(math.pow(-dist, 2) / (2 * math.pow(sigma, 2)))

  implicit def identityScale(lands: IndexedSeq[ExtLandProperty]): Array[ScaleStrategy] = {
    (0 until lands(0).toVector.size).map(_ => IdentityScale).toArray
  }

  def normZScale(lands: IndexedSeq[ExtLandProperty]): Array[ScaleStrategy] = {
    val priceScaleF = scaleFactors(lands)(_.price.toDouble)

    val areaScaleF = scaleFactors(lands)(_.area.toDouble)
    val roomsScaleF = scaleFactors(lands)(_.rooms.toDouble)
    val stockScaleF = scaleFactors(lands)(_.stock.toDouble)
    val tStockScaleF = scaleFactors(lands)(_.totalStocks.toDouble)
    val builtYearF = scaleFactors(lands)(_.builtYear.toDouble)

    Array(areaScaleF, priceScaleF, roomsScaleF, stockScaleF, tStockScaleF, builtYearF)
  }

  def rangeScale(lands: IndexedSeq[ExtLandProperty]): Array[ScaleStrategy] = {
    val priceScaleF = RangeScale(lands.maxBy(_.price).price.toDouble, lands.minBy(_.price).price.toDouble)
    val areaScaleF = RangeScale(lands.maxBy(_.area).area.toDouble, lands.minBy(_.area).area.toDouble)
    val roomsScaleF = RangeScale(lands.maxBy(_.rooms).rooms.toDouble, lands.minBy(_.rooms).rooms.toDouble)
    val stockScaleF = RangeScale(lands.maxBy(_.stock).stock.toDouble, lands.minBy(_.stock).stock.toDouble)
    val tStockScaleF = RangeScale(lands.maxBy(_.totalStocks).totalStocks.toDouble, lands.minBy(_.totalStocks).totalStocks.toDouble)
    val builtYearF = RangeScale(lands.maxBy(_.builtYear).builtYear.toDouble, lands.minBy(_.builtYear).builtYear.toDouble)

    Array(areaScaleF, priceScaleF, roomsScaleF, stockScaleF, tStockScaleF, builtYearF)
  }

  def distanceMatrixZNormScaled(lands: IndexedSeq[ExtLandProperty]) = distanceMatrix(lands)(normZScale)

  def distanceMatrixRangeScaled(lands: IndexedSeq[ExtLandProperty]) = distanceMatrix(lands)(rangeScale)

  def distanceMatrixSimple(lands: IndexedSeq[ExtLandProperty]) = distanceMatrix(lands)(identityScale)

  def distanceMatrix(lands: IndexedSeq[ExtLandProperty])(implicit scaleFun: IndexedSeq[ExtLandProperty] => Array[ScaleStrategy]) = {
    val len = lands.length
    val array = ofDim[Double](len, len)
    val trace = ofDim[List[CalcTrace]](len, len)

    // calculate scale functions
    val scaleFunctions = scaleFun(lands)
    // TODO: Implement rescaling and euclides metrics and alpha params
    // TODO: kNN means using weighs - k nears average with  gaussian weight
    for (i <- 0 until len; j <- 0 until len) {
      val (metric, traceList) = calculate(lands(i), lands(j), scaleFunctions)((x, y) => math.abs(x - y))
      array(i)(j) = metric
      trace(i)(j) = traceList
    }
    this.matrix = array
    this.traceMatrix = trace
    new AlphaDistanceCalculator(alphaFactors, matrix, traceMatrix)
  }

  protected[this] def applyToMatrix(cellFunc: (DisSimCalculator[ExtLandProperty], Double) => Double): AlphaDistanceCalculator = {
    val len = matrix.length
    val newMatrix = ofDim[Double](len, len)

    for (i <- 0 until len; j <- 0 until len) {
      newMatrix(i)(j) = cellFunc(this, matrix(i)(j))
    }
    new AlphaDistanceCalculator(alphaFactors, newMatrix, traceMatrix)
  }

  def distToSimMatrix = applyToMatrix((ctx, d) => 1.0 / (1.0 + d))

  def scaleToMatrix = applyToMatrix((ctx, d) => (d - ctx.mean) / ctx.sd)

  lazy val mean = matrix.foldLeft(0.0) {
    (t, a) => t + a.sum
  } / (matrix.length * matrix.length)
  lazy val sd = math.sqrt(variance)
  lazy val variance = {
    var c = 0
    var sum = 0.0
    for (i <- 0 until matrix.length; j <- i + 1 until matrix.length) {
      sum += math.pow(matrix(i)(j) - mean, 2)
      c += 1
    }
    if (c > 0) sum / c else 0.0
  }

  lazy val min = matrix.foldLeft(Double.MaxValue) {
    (t, a) => math.min(t, a.min)
  }

  lazy val max = matrix.foldLeft(Double.MinValue) {
    (t, a) => math.max(t, a.max)
  }

  override val toString = {
    val len = matrix.length
    val sb = new StringBuilder
    for (i <- 0 until len) {
      for (j <- 0 until len) {
        sb append (f"${matrix(i)(j)}%4.2f")
      }
      sb append ("\n")
    }
    sb toString
  }
}
