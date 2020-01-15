sealed trait Nat
case object Zero extends Nat
case class Succ[N <: Nat](n: N) extends Nat

sealed trait MyList
case object MyNil extends MyList
case class MyCons[A, Tail <: MyList](head: A, tail: Tail) extends MyList

trait Length[Input <: MyList] {
  type Result <: Nat
  def length(input: Input): Result
}

object Length {
  type LengthAux[
    Input <: MyList,
    Res <: Nat] = Length[Input] { type Result = Res }

  implicit def nil: LengthAux[MyNil.type, Zero.type] = {
    new Length[MyNil.type] {
      type Result = Zero.type
      def length(input: MyNil.type): Result = Zero
    }
  }

  implicit def cons[
    Element,
    RestList <: MyList,
    RestLength <: Nat](
    implicit rec: LengthAux[RestList, RestLength]):
      LengthAux[MyCons[Element, RestList], Succ[RestLength]] = {
    new Length[MyCons[Element, RestList]] {
      type Result = Succ[RestLength]
      def length(input: MyCons[Element, RestList]): Result = {
        Succ(rec.length(input.tail))
      }
    }
  }

  def length[
    TheList <: MyList,
    Res <: Nat](input: TheList)(implicit ev: LengthAux[TheList, Res]): Res = {
    ev.length(input)
  }
}
