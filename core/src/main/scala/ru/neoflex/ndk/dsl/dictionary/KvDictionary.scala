package ru.neoflex.ndk.dsl.dictionary

import io.circe.Decoder

abstract class KvDictionary[V: Decoder](dictionaryName: String, eagerLoad: Boolean = true) {
  private lazy val table = DictionaryLoader.loadDictionary[Map[String, V]](dictionaryName, "table")

  if (eagerLoad) {
    getTable
  }

  private def getTable: Map[String, V] = table.fold(e => throw e.toThrowable, x => x)

  def apply(key: String): DictionaryValue[V] = DictionaryValue(dictionaryName) {
    table.map(_.get(key))
  }

  def get(key: String): Option[V] = getTable.get(key)
}
