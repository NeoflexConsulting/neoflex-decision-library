package ru.neoflex.ndk.strategy.domain

final case class SalesPoint(products: Seq[Product])
final case class Product(productFamily: String, productFamilyType: String)
