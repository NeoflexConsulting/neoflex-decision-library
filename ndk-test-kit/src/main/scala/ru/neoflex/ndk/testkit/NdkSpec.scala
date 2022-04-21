package ru.neoflex.ndk.testkit

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.wordspec.AnyWordSpec

abstract class NdkSpec extends NdkSpecLike

abstract class NdkAnyFlatSpec extends AnyFlatSpec with NdkSpecLike

abstract class NdkAnyWordSpec extends AnyWordSpec with NdkSpecLike
