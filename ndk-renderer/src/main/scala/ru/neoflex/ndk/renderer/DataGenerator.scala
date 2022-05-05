package ru.neoflex.ndk.renderer

import ru.neoflex.ndk.dsl.FlowOp

import scala.reflect.api
import scala.reflect.api.{ TypeCreator, Universe }
import scala.reflect.runtime.universe._

class DataGenerator(mirror: Mirror) extends App {
  def generate(caseClass: Class[_]): FlowOp = {
    generate(mirror.classSymbol(caseClass).toType).asInstanceOf[FlowOp]
  }

  def generate(caseClassType: Type): Any = {
    val method = caseClassType.companion.decl(TermName("apply")).asMethod
    val params = method.paramLists.head

    var generatorRunNumber = 0
    val args = params.map { param =>
      generatorRunNumber += 1
      param.info match {
        case t if t <:< typeOf[Enumeration#Value] => chooseEnumValue(convert(t).asInstanceOf[TypeTag[_ <: Enumeration]])
        case t if t =:= typeOf[Byte]              => 0
        case t if t =:= typeOf[Char]              => '0'
        case t if t =:= typeOf[Int]               => generatorRunNumber
        case t if t =:= typeOf[Long]              => generatorRunNumber
        case t if t <:< typeOf[Option[_]]  => None
        case t if t =:= typeOf[String]     => s"arbitrary-$generatorRunNumber"
        case t if t =:= typeOf[Boolean]    => false
        case t if t =:= typeOf[BigDecimal] => BigDecimal(0)
        case t if t <:< typeOf[Seq[_]]     => List.empty
        case t if t <:< typeOf[Map[_, _]]  => Map.empty
        case t if isCaseClass(t)           => generate(convert(t).tpe)
        case t if isSealedTrait(t)         => findFirstSubclassObject(t)
        case t                             => throw new Exception(s"Generator doesn't support generating $t")
      }
    }

    val obj = mirror.reflectModule(caseClassType.typeSymbol.companion.asModule).instance
    mirror.reflect(obj).reflectMethod(method)(args: _*)
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
