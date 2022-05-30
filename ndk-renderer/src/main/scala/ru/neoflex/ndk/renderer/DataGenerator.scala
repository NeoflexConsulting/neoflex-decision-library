package ru.neoflex.ndk.renderer

import ru.neoflex.ndk.dsl.FlowOp

import java.time.{
  Instant,
  LocalDate,
  LocalDateTime,
  LocalTime,
  Month,
  MonthDay,
  OffsetDateTime,
  OffsetTime,
  Year,
  YearMonth,
  ZonedDateTime
}
import scala.reflect.api
import scala.reflect.api.{ TypeCreator, Universe }
import scala.reflect.runtime.universe._

class DataGenerator(mirror: Mirror) extends App with (Class[_] => Any) {
  override def apply(clazz: Class[_]): Any = generate(clazz)

  def generate(clazz: Class[_]): Any = {
    val cls = mirror.reflectClass(mirror.classSymbol(clazz))
    generate(cls.symbol.toType)
  }

  def generateAsFlowOp(caseClass: Class[_]): FlowOp = generate(caseClass).asInstanceOf[FlowOp]

  def generateFromCaseClass(caseClassType: Type): Any = {
    val method = caseClassType.companion.decl(TermName("apply")).asMethod
    val params = method.paramLists.head

    var generatorRunNumber = 0
    val args = params.map { param =>
      generatorRunNumber += 1
      generate(param.info, generatorRunNumber)
    }

    val obj = mirror.reflectModule(caseClassType.typeSymbol.companion.asModule).instance
    mirror.reflect(obj).reflectMethod(method)(args: _*)
  }

  private def generate(t: Type, i: Int = 1): Any = t match {
    case t if t <:< typeOf[Enumeration#Value] => chooseEnumValue(convert(t).asInstanceOf[TypeTag[_ <: Enumeration]])
    case t if t =:= typeOf[Byte]              => 0.toByte
    case t if t =:= typeOf[Char]              => '0'
    case t if t =:= typeOf[Int]               => i
    case t if t =:= typeOf[Long]              => i.toLong
    case t if t =:= typeOf[Double]            => i.toDouble
    case t if t =:= typeOf[Float]             => i.toFloat
    case t if t <:< typeOf[Option[_]]         => None
    case t if t =:= typeOf[String]            => s"arbitrary-$i"
    case t if t =:= typeOf[Boolean]           => false
    case t if t =:= typeOf[BigDecimal]        => BigDecimal(0)
    case t if t <:< typeOf[Seq[_]]            => List.empty
    case t if t <:< typeOf[Map[_, _]]         => Map.empty
    case t if t <:< typeOf[LocalDate]         => LocalDate.now()
    case t if t <:< typeOf[OffsetTime]        => OffsetTime.now()
    case t if t <:< typeOf[OffsetDateTime]    => OffsetDateTime.now()
    case t if t <:< typeOf[LocalTime]         => LocalTime.now()
    case t if t <:< typeOf[ZonedDateTime]     => ZonedDateTime.now()
    case t if t <:< typeOf[Month]             => Month.JANUARY
    case t if t <:< typeOf[MonthDay]          => MonthDay.now()
    case t if t <:< typeOf[Year]              => Year.now()
    case t if t <:< typeOf[YearMonth]         => YearMonth.now()
    case t if t <:< typeOf[LocalDateTime]     => LocalDateTime.now()
    case t if t <:< typeOf[Instant]           => Instant.now()
    case t if isCaseClass(t)                  => generateFromCaseClass(convert(t).tpe)
    case t if isSealedTrait(t)                => findFirstSubclassObject(t)
    case t                                    => throw new Exception(s"Generator doesn't support generating $t")
  }

  private def chooseEnumValue[E <: Enumeration: TypeTag]: E#Value = {
    val parentType   = typeOf[E].asInstanceOf[TypeRef].pre
    val valuesMethod = parentType.baseType(typeOf[Enumeration].typeSymbol).decl(TermName("values")).asMethod
    val obj          = mirror.reflectModule(parentType.termSymbol.asModule).instance

    mirror.reflect(obj).reflectMethod(valuesMethod)().asInstanceOf[E#ValueSet].head
  }

  private def convert(tpe: Type): TypeTag[_] = {
    TypeTag.apply(
      runtimeMirror(getClass.getClassLoader),
      new TypeCreator {
        override def apply[U <: Universe with Singleton](m: api.Mirror[U]) = {
          tpe.asInstanceOf[U#Type]
        }
      }
    )
  }

  private def isSealedTrait(t: Type): Boolean = {
    val tps = t.typeSymbol
    tps.isClass && tps.asClass.isTrait && tps.asClass.isSealed
  }

  private def findFirstSubclassObject(t: Type) = {
    t.typeSymbol.asClass.knownDirectSubclasses
      .filter(_.isType)
      .map(_.asType)
      .flatMap { s =>
        val className       = s.fullName
        val objectClassName = className + "$"
        util.Try(Class.forName(objectClassName)).toOption
      }
      .map { c =>
        val moduleField = c.getField("MODULE$")
        moduleField.get(c)
      }
      .headOption
      .get
  }

  private def isCaseClass(t: Type): Boolean = {
    t.companion.decls.exists(_.name.decodedName.toString == "apply") &&
    t.decls.exists(_.name.decodedName.toString == "copy")
  }
}
