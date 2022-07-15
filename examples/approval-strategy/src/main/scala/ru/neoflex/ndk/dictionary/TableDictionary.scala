package ru.neoflex.ndk.dictionary

abstract class TableDictionary(dictionaryName: String) {
  protected lazy val table: Map[String, Map[String, Map[String, String]]] = DictionaryLoader.load(dictionaryName)

  def apply[T](keys: Any*): T = ???
}
