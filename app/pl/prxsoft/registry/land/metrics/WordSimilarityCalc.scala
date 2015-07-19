package pl.prxsoft.registry.land.metrics

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 06.10.2013
 * Time: 22:45
 */
trait WordSimilarityCalc {
  val stopWords = List("ul.", "warszawa")

  def similarity(title1: String, title2: String)  = {
    val p = """(,)(\d{1,2})m2""".r
    val t1 = p replaceFirstIn (title1, ".\2m2")
    val t2 = p replaceFirstIn (title2, ".\2m2")

    val words1 = t1.split(",").map(_.trim.toLowerCase).toVector
    val words2 = t2.split(",").map(_.trim.toLowerCase).toVector

    jaccardCoeff(removeStopWords(words1), removeStopWords(words2))
  }

  def jaccardCoeff(a: Vector[String], b: Vector[String]) = {
    val s1 = a.toSet
    val s2 = b.toSet
    val n = s1 | s2
    val nunion = a.toSet & b.toSet
    (nunion.size).toDouble / n.size.toDouble
  }

  def removeStopWords(words: Vector[String]) = {
      def processStopWords(word: String, _stopWords: List[String]): String = _stopWords match {
        case Nil => word
        case e :: es if word.indexOf(e) >= 0 => processStopWords(word.replaceAll(e, ""), es)
        case e :: es => processStopWords(word, es)
      }

      val items = words.map(processStopWords(_, stopWords)).map(_.trim).filter(!_.isEmpty)
      items
  }

}
