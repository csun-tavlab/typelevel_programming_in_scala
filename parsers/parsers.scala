sealed trait Nat {
  def asInteger: Int
}
case object Zero extends Nat {
  def asInteger: Int = 0
}
case class Succ[N <: Nat](n: N) extends Nat {
  def asInteger: Int = n.asInteger + 1
}

sealed trait HList
case object HNil extends HList
case class HCons[Head, Tail <: HList](head: Head, tail: Tail) extends HList

trait Func[A, B] {
  def apply(a: A): B
}

sealed trait PreParser
case class Elem[A](a: A) extends PreParser
case class And[P1 <: PreParser, P2 <: PreParser](p1: P1, p2: P2) extends PreParser
case class Or[P1 <: PreParser, P2 <: PreParser](p1: P1, p2: P2) extends PreParser
case class MapP[PP <: PreParser, B](p: PP) extends PreParser

trait MakeMap[B] {
  def apply[PP <: PreParser](p: PP): MapP[PP, B]
}

trait Parser[PP <: PreParser, InputTokens <: HList] {
  type OutputTokens <: HList
  type Parsed
  def parse(input: InputTokens): (OutputTokens, Parsed)
}

object Parser {
  type ParserAux[
    PP <: PreParser,
    InputTokens <: HList,
    Out <: HList,
    P] =
    Parser[PP, InputTokens] {
      type OutputTokens = Out
      type Parsed = P
    }

  def map[B]: MakeMap[B] = {
    new MakeMap[B] {
      def apply[PP <: PreParser](p: PP): MapP[PP, B] = MapP(p)
    }
  }

  implicit def elem[
    A,
    Rest <: HList]: ParserAux[Elem[A], HCons[A, Rest], Rest, A] = {
    new Parser[Elem[A], HCons[A, Rest]] {
      type OutputTokens = Rest
      type Parsed = A
      def parse(input: HCons[A, Rest]): (Rest, A) = {
        (input.tail, input.head)
      }
    }
  }

  implicit def and[
    P1 <: PreParser,
    P2 <: PreParser,
    List1 <: HList,
    List2 <: HList,
    List3 <: HList,
    A,
    B
  ](implicit ev1: ParserAux[P1, List1, List2, A],
    ev2: ParserAux[P2, List2, List3, B]): ParserAux[And[P1, P2], List1, List3, (A, B)] = {
    new Parser[And[P1, P2], List1] {
      type OutputTokens = List3
      type Parsed = (A, B)
      def parse(list1: List1): (List3, (A, B)) = {
        val (list2, a) = ev1.parse(list1)
        val (list3, b) = ev2.parse(list2)
        (list3, (a, b))
      }
    }
  }

  implicit def orLeft[
    P1 <: PreParser,
    P2 <: PreParser,
    InputList <: HList,
    OutputList <: HList,
    A
  ](implicit ev: ParserAux[P1, InputList, OutputList, A]): ParserAux[Or[P1, P2], InputList, OutputList, A] = {
    new Parser[Or[P1, P2], InputList] {
      type OutputTokens = OutputList
      type Parsed = A
      def parse(input: InputList): (OutputList, A) = {
        ev.parse(input)
      }
    }
  }

  implicit def orRight[
    P1 <: PreParser,
    P2 <: PreParser,
    InputList <: HList,
    OutputList <: HList,
    A
  ](implicit ev: ParserAux[P2, InputList, OutputList, A]): ParserAux[Or[P1, P2], InputList, OutputList, A] = {
    new Parser[Or[P1, P2], InputList] {
      type OutputTokens = OutputList
      type Parsed = A
      def parse(input: InputList): (OutputList, A) = {
        ev.parse(input)
      }
    }
  }

  implicit def mapP[
    PP <: PreParser,
    A,
    B,
    InputList <: HList,
    OutputList <: HList
  ](implicit ev: ParserAux[PP, InputList, OutputList, A], f: Func[A, B]): ParserAux[MapP[PP, B], InputList, OutputList, B] = {
    new Parser[MapP[PP, B], InputList] {
      type OutputTokens = OutputList
      type Parsed = B
      def parse(input: InputList): (OutputList, B) = {
        val (output, a) = ev.parse(input)
        (output, f.apply(a))
      }
    }
  }

  def parse[
    PP <: PreParser,
    InputTokens <: HList,
    Result
  ](pp: PP, input: InputTokens)(implicit ev: ParserAux[PP, InputTokens, HNil.type, Result]): Result = {
    ev.parse(input)._2
  }
}

object A {}
object B {}
object C {}
case class Variable[Id <: Nat](id: Id)

object Test {
  import Parser._
  def main(args: Array[String]) {
    println(parse(Elem(A), HCons(A, HNil)))
    println(parse(And(Elem(A), Elem(B)), HCons(A, HCons(B, HNil))))
    println(parse(Or(Elem(A), Elem(B)), HCons(A, HNil)))
    println(parse(Or(Elem(A), Elem(B)), HCons(B, HNil)))
    implicit def varToInt[N <: Nat]: Func[Variable[N], Int] = {
      new Func[Variable[N], Int] {
        def apply(x: Variable[N]): Int = x.id.asInteger
      }
    }

    val x = Variable(Succ(Succ(Zero)))
    println(parse(map[Int](Elem(x)), HCons(x, HNil)))
  }
}
