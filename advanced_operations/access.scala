sealed trait Nat
case object Zero extends Nat
case class Succ[N <: Nat](n: N) extends Nat

sealed trait MyList
case object MyNil extends MyList
case class MyCons[A, Tail <: MyList](head: A, tail: Tail) extends MyList

trait Access[List <: MyList, Index <: Nat] {
  type Result
  def access(list: List, index: Index): Result
}

object Access {
  type AccessAux[
    List <: MyList,
    Index <: Nat,
    Res] = Access[List, Index] { type Result = Res }

  implicit def accessZero[
    Element,
    RestList <: MyList]: AccessAux[MyCons[Element, RestList], Zero.type, Element] = {
    new Access[MyCons[Element, RestList], Zero.type] {
      type Result = Element
      def access(list: MyCons[Element, RestList], index: Zero.type): Result = {
        list.head
      }
    }
  }

  implicit def accessSucc[
    FoundElement,
    TargetElement,
    RestList <: MyList,
    RestNat <: Nat](
    implicit rec: AccessAux[RestList, RestNat, TargetElement]):
      AccessAux[MyCons[FoundElement, RestList], Succ[RestNat], TargetElement] = {
    new Access[MyCons[FoundElement, RestList], Succ[RestNat]] {
      type Result = TargetElement
      def access(list: MyCons[FoundElement, RestList], index: Succ[RestNat]): Result = {
        rec.access(list.tail, index.n)
      }
    }
  }

  def access[
    Element,
    List <: MyList,
    Index <: Nat](list: List, index: Index)(implicit ev: AccessAux[List, Index, Element]): Element = {
    ev.access(list, index)
  }
}
