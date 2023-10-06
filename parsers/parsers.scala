sealed trait HList
case object HNil extends HList
case class HCons[Head, Tail <: HList](head: Head, tail: Tail) extends HList

// And[Elem[A.type], Elem[B.type]]
sealed trait PreParser
case class Elem[A](a: A) extends PreParser
case class And[P1 <: PreParser, P2 <: PreParser](p1: P1, p2: P2) extends PreParser
case class Or[P1 <: PreParser, P2 <: PreParser](p1: P1, p2: P2) extends PreParser

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

object Test {
  import Parser._
  def main(args: Array[String]) {
    println(parse(Elem(A), HCons(A, HNil)))
    println(parse(And(Elem(A), Elem(B)), HCons(A, HCons(B, HNil))))
    println(parse(Or(Elem(A), Elem(B)), HCons(A, HNil)))
    println(parse(Or(Elem(A), Elem(B)), HCons(B, HNil)))
  }
}
