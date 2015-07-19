package pl.prxsoft.registry.land

import _root_.pl.prxsoft.registry.land.metrics.WordSimilarityCalc


object Solution  extends App with WordSimilarityCalc {

  val s1 = "Mieszkanie 4-pokojowe, 106,33m2, 9 piętro,Warszawa, MokotÓw, MokotÓw Dolny, ul. Czerniakowska"
  val s2 = "Mieszkanie 4-pokojowe, 85,48m2, 3 piętro,Warszawa, MokotÓw, Sadyba, ul. Limanowskiego"

  print(this.similarity(s1, s2))
}
