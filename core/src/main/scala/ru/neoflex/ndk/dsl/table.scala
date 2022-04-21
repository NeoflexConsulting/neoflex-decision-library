package ru.neoflex.ndk.dsl
import ru.neoflex.ndk.dsl.Table._

import scala.reflect.ClassTag

abstract class Table(override val id: String, override val name: Option[String], initializer: => TableBuilder) extends TableOp {
  def this(id: String, initializer: => TableBuilder) = {
    this(id, None, initializer)
  }

  private val t = initializer

  val expressions: List[Expression]     = t.expressionsList
  val actions: List[ActionDef]          = t.actionsList
  val conditions: List[Table.Condition] = t.conditionsList

  val actionsByName: Map[String, ActionDef] = actions.map(a => (a.name, a)).toMap
}

final case class SealedTable(
  override val id: String,
  override val name: Option[String],
  override val expressions: List[Expression],
  override val actions: List[ActionDef],
  override val conditions: List[Table.Condition])
    extends Table(id, name, TableBuilder(expressions, actions, conditions))

object Table {
  final case class Expression(f: () => Any, name: String = "")

  sealed trait ActionDef {
    def name: String
    def argsCount: Int
  }
  final case class Action0(name: String, f: () => Unit) extends ActionDef {
    override def argsCount: Int = 0
  }
  final case class Action1(name: String, f: Any => Unit) extends ActionDef {
    override def argsCount: Int = 1
  }
  final case class Action2(name: String, f: (Any, Any) => Unit) extends ActionDef {
    override def argsCount: Int = 2
  }
  final case class Action3(name: String, f: (Any, Any, Any) => Unit) extends ActionDef {
    override def argsCount: Int = 3
  }
  final case class Action4(name: String, f: (Any, Any, Any, Any) => Unit) extends ActionDef {
    override def argsCount: Int = 4
  }
  final case class Action5(name: String, f: (Any, Any, Any, Any, Any) => Unit) extends ActionDef {
    override def argsCount: Int = 5
  }
  final case class Action6(name: String, f: (Any, Any, Any, Any, Any, Any) => Unit) extends ActionDef {
    override def argsCount: Int = 6
  }
  final case class Action7(name: String, f: (Any, Any, Any, Any, Any, Any, Any) => Unit) extends ActionDef {
    override def argsCount: Int = 7
  }
  final case class Action8(name: String, f: (Any, Any, Any, Any, Any, Any, Any, Any) => Unit) extends ActionDef {
    override def argsCount: Int = 8
  }
  final case class Action9(name: String, f: (Any, Any, Any, Any, Any, Any, Any, Any, Any) => Unit) extends ActionDef {
    override def argsCount: Int = 9
  }
  final case class Action10(name: String, f: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) => Unit)
      extends ActionDef {
    override def argsCount: Int = 10
  }

  final case class Args(value: List[Any]) {
    def count: Int = value.length
    def apply(index: Int): Any = value(index)
  }

  sealed trait CallableAction
  final case class ActionRef(name: String, args: Args)            extends CallableAction
  final case class SealedAction(f: () => Unit, name: String = "") extends CallableAction
  sealed trait Operator                                           extends (Any => Boolean)
  final case class Condition(operators: Seq[Operator], callableAction: CallableAction)

  final case class TableBuilder(
    expressionsList: List[Expression],
    actionsList: List[ActionDef],
    conditionsList: List[Condition]) {
    def apply(exprs: Expression*): TableBuilder = copy(expressionsList = exprs.toList)

    def withActions(actions: ActionDef*): TableBuilder = copy(actionsList = actions.toList)

    def andConditions(conditions: Condition*): TableBuilder = copy(conditionsList = conditions.toList)
  }

  trait Operators {
    trait OrderingOperator {
      def compare[T: Ordering: ClassTag](o: Any, v: T)(cmp: Ordering[T] => (T, T) => Boolean): Boolean = o match {
        case x: T => cmp(implicitly[Ordering[T]])(x, v)
      }
    }
    case class eqv[T](v: T) extends Operator {
      override def apply(o: Any): Boolean = o == v
    }
    case class neq[T](v: T) extends Operator {
      override def apply(o: Any): Boolean = o != v
    }
    case class gt[T: Ordering: ClassTag](v: T) extends Operator with OrderingOperator {
      override def apply(o: Any): Boolean = compare(o, v)(_.gt)
    }
    case class gte[T: Ordering: ClassTag](v: T) extends Operator with OrderingOperator {
      override def apply(o: Any): Boolean = compare(o, v)(_.gteq)
    }
    case class lt[T: Ordering: ClassTag](v: T) extends Operator with OrderingOperator {
      override def apply(o: Any): Boolean = compare(o, v)(_.lt)
    }
    case class lte[T: Ordering: ClassTag](v: T) extends Operator with OrderingOperator {
      override def apply(o: Any): Boolean = compare(o, v)(_.lteq)
    }
    case class empty() extends Operator {
      override def apply(v: Any): Boolean = true
    }
    case class nonEmpty() extends Operator {
      override def apply(v: Any): Boolean = true
    }
    case class any() extends Operator {
      override def apply(v: Any): Boolean = true
    }
  }
}

trait TableSyntax extends Operators {
  def table(id: String, name: Option[String] = None)(builder: => TableBuilder): SealedTable = {
    val tb = builder
    SealedTable(id, name, tb.expressionsList, tb.actionsList, tb.conditionsList)
  }

  def expressions: TableBuilder = TableBuilder(List.empty, List.empty, List.empty)

  def row(operators: Operator*): CallableAction => Table.Condition = action => Table.Condition(operators, action)
}

trait TableImplicits {
  implicit def functionToExpression[T](f: => T): Expression = Expression(() => f)

  implicit def functionToSealedAction(f: => Unit): SealedAction = SealedAction(() => f)

  implicit class NamingExpression[T](name: String) {
    def expr(f: => T): Expression = Expression(() => f, name)
  }

  implicit class NamingActionDef(name: String) {
    def action(f: => Unit): ActionDef                                                    = Action0(name, () => f)
    def action(f: Any => Unit): ActionDef                                                = Action1(name, f)
    def action(f: (Any, Any) => Unit): ActionDef                                         = Action2(name, f)
    def action(f: (Any, Any, Any) => Unit): ActionDef                                    = Action3(name, f)
    def action(f: (Any, Any, Any, Any) => Unit): ActionDef                               = Action4(name, f)
    def action(f: (Any, Any, Any, Any, Any) => Unit): ActionDef                          = Action5(name, f)
    def action(f: (Any, Any, Any, Any, Any, Any) => Unit): ActionDef                     = Action6(name, f)
    def action(f: (Any, Any, Any, Any, Any, Any, Any) => Unit): ActionDef                = Action7(name, f)
    def action(f: (Any, Any, Any, Any, Any, Any, Any, Any) => Unit): ActionDef           = Action8(name, f)
    def action(f: (Any, Any, Any, Any, Any, Any, Any, Any, Any) => Unit): ActionDef      = Action9(name, f)
    def action(f: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) => Unit): ActionDef = Action10(name, f)
  }

  implicit class NamingActionRef(name: String) {
    def withArgs(args: Any*): ActionRef = ActionRef(name, Args(args.toList))
  }
}
