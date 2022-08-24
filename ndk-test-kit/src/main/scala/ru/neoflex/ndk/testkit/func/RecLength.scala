package ru.neoflex.ndk.testkit.func

import shapeless._

trait RecLength[L <: HList] {
  def apply(l: L): Int
}

trait LowPriorityRecLength {
  implicit def hconsRecLength1[V, T <: HList](implicit rl: RecLength[T]): RecLength[V :: T] = (l: V :: T) => {
    1 + rl(l.tail)
  }
}

object RecLength extends LowPriorityRecLength {
  implicit val hnilRecLength: RecLength[HNil] = (_: HNil) => 0

  implicit def hconsRecLength0[V, R <: HList, T <: HList](
    implicit gen: Generic.Aux[V, R],
    rlH: RecLength[R],
    rlT: RecLength[T]
  ): RecLength[V :: T] = (l: V :: T) => {
    rlH(gen.to(l.head)) + rlT(l.tail)
  }

  implicit class ToRecLengthOps[A](val a: A) extends AnyVal {
    def recordLength[L <: HList](implicit gen: Generic.Aux[A, L], rl: RecLength[L]): Int = rl(gen.to(a))
  }
}
