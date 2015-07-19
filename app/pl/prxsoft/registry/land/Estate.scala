package pl.prxsoft.registry.land

import java.sql.Date

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 08.07.2013
 * Time: 23:24
 */
sealed trait Estate {
  val guid: String
  val title: String
  val pubDate: Date
  val area: BigDecimal
  val price: BigDecimal
  val rooms: Int
  val toVector: Vector[Double]
}

case class LandProperty(guid: String, title: String, pubDate: Date, area: BigDecimal, price: BigDecimal, rooms: Int) extends Estate {
  override def toString = {
    s"[${pubDate}: price: ${price} area:${area} rooms: ${rooms}] title: ${title}"
  }

  def toExtLandProperty = {
    ExtLandProperty(null, guid, title, pubDate, area, price, rooms, -1,-1,-1, Some(""), false)
  }

  val toVector: Vector[Double] = Vector(area.toDouble, price.toDouble, rooms)
}

trait Weighted {
  val weighs: Vector[Double]
}

case class ExtLandProperty(site: String, guid: String, title: String, pubDate: Date,
                           area: BigDecimal, price: BigDecimal, rooms: Int,
                           stock: Int, totalStocks: Int, builtYear: Int, parseSet: Option[String], viewed: Boolean) extends Estate with Weighted {


  val toVector: Vector[Double] = Vector(area.toDouble, price.toDouble, rooms, stock, totalStocks, builtYear)
  val toVectorDesc = Vector("area", "price", "rooms", "stock", "total stocks", "built year")
  val weighs = Vector(0.2, 0.15, 0.25, 0.175, 0.125, 0.10)

  val titleWeight = 0.00

  require(weighs.sum + titleWeight - 1.0 < 0.001)

  def this(site: String, guid: String, price: BigDecimal) = this(site, guid, null, null, BigDecimal(0), price, -1, -1, -1, -1, None, false)

  private def fne(s1: String, s2: String) = if (s1 == null || s1.isEmpty) s2 else s1

  private def fne(b1: BigDecimal, b2: BigDecimal) = if (b1 != null && BigDecimal(-1) == b1) b2 else b1

  private def fne(b1: Int, b2: Int) = if (-1 == b1) b2 else b1

  private def fne(d1: Date, d2: Date) = if (d1 == null) d2 else d1

  def merge(f: ExtLandProperty): ExtLandProperty = {
    f.copy(fne(this.site, f.site), fne(this.guid, f.guid), fne(this.title, f.title), fne(this.pubDate, f.pubDate),
      fne(this.area, f.area), fne(this.price, f.price), fne(this.rooms, f.rooms), fne(this.stock, f.stock), fne(this.totalStocks, f.totalStocks),
      fne(this.builtYear, f.builtYear))
  }
}



