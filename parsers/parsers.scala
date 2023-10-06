sealed trait HList
case object HNil extends HList
case class HCons[Head, Tail <: HList](head: Head, tail: Tail) extends HList

// what you really want to say is at compile time, you know
// what Parsed will be for some input token prefix

// trait Parser[HCons[Token1, HCons[Token2, Rest]],
//              Rest, A]
trait Parser[InputTokens <: HList] {
  type OutputTokens <: HList
  type Parsed
  def parse(input: InputTokens): (OutputTokens, Parsed)
}

object Parser {
  type ParserAux[
    InputTokens <: HList,
    Out <: HList,
    P] =
    Parser[InputTokens] {
      type OutputTokens = Out
      type Parsed = P
    }

  implicit def elem[
    Elem,
    Rest <: HList]: ParserAux[HCons[Elem, Rest], Rest, Elem] = {
    new Parser[HCons[Elem, Rest]] {
      type OutputTokens = Rest
      type Parsed = P
      def parse(input: HCons[Elem, Rest]): (Rest, Elem) = {
        (input.tail, input.head)
      }
    }
  }

  def elem[
    Elem,
    P,
    Rest <: HList](f: Elem => P): ParserAux[HCons[Elem, Rest], Rest, P] = {
    new Parser[HCons[Elem, Rest]] {
      type OutputTokens = Rest
      type Parsed = P
      def parse(input: HCons[Elem, Rest]): (Rest, P) = {
        (input.tail, f(input.head))
      }
    }
  }

  def and[
    List1 <: HList,
    List2 <: HList,
    List3 <: HList,
    A,
    B
  ](p1: ParserAux[List1, List2, A], p2: ParserAux[List2, List3, B]): ParserAux[List1, List3, (A, B)] = {
    new Parser[List1] {
      type OutputTokens = List3
      type Parsed = (A, B)
      def parse(input: List1): (List3, (A, B)) = {
        val (list2, a) = p1.parse(input)
        val (list3, b) = p2.parse(list2)
        (list3, (a, b))
      }
    }
  }

  def map[
    List1 <: HList,
    List2 <: HList,
    A,
    B](p: ParserAux[List1, List2, A], f: A => B): ParserAux[List1, List2, B] = {
    new Parser[List1] {
      type OutputTokens = List2
      type Parsed = B
      def parse(input: List1): (List2, B) = {
        val (list2, a) = p.parse(input)
        (list2, f(a))
      }
    }
  }
}

// exp ::= a b c
object A {}
object B {}
object C {}

object Test {
  import Parser._
  val test: Int = and(elem((a: A.type) => a), elem((b: B.type) => b))

  // val test = and(elem((a: A.type) => a),
  //   and(elem((b: B.type) => b),
  //     elem((c: C.type) => c)))
}
