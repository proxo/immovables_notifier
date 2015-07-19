import org.specs2.control.Debug
import org.specs2.mutable.Specification
import org.specs2.specification.Tags
import pl.prxsoft.registry.land.metrics.WordSimilarityCalc

/**
 * Created with IntelliJ IDEA.
 * User: pmasko
 * Date: 06.10.2013
 * Time: 23:01
 */
class WordSimSpec  extends Specification with Tags with Debug {

  "Sim should" should {
    val sim = new Object with WordSimilarityCalc
    val s1 = "Mieszkanie 4-pokojowe, 106,33m2, 9 piętro,Warszawa, MokotÓw, MokotÓw Dolny, ul. Czerniakowska"
    val s2 = "Mieszkanie 4-pokojowe, 85,48m2, 3 piętro,Warszawa, MokotÓw, Sadyba, ul. Limanowskiego"
    val s3 = "Mieszkanie 4-pokojowe, 85,48m2, 3 piętro,Warszawa, MokotÓw, Sadyba"

    val w1 = sim.similarity(s1, s2)
    val w2 = sim.similarity(s2, s3)
    println(w1)
    println("s2 jaccard s3=" + w2)
    println("s2 jaccard s2=" + sim.similarity(s2, s2))
  }


}
