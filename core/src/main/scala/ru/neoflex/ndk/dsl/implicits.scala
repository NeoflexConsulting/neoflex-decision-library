package ru.neoflex.ndk.dsl

import ru.neoflex.ndk.dsl.dictionary.indexed.SearchConditionOperatorImplicits

object implicits extends TableImplicits with SearchConditionOperatorImplicits with LazyConditionImplicits
