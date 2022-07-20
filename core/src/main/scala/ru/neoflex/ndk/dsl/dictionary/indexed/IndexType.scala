package ru.neoflex.ndk.dsl.dictionary.indexed

sealed trait IndexType
case object EqIndexType   extends IndexType
case object LikeIndexType extends IndexType
