sealed trait Nat
case object Zero extends Nat
case class Succ[N <: Nat](n: N) extends Nat

sealed trait MyBoolean
case object MyTrue extends MyBoolean
case object MyFalse extends MyBoolean

trait LessThanValue[N1 <: Nat, N2 <: Nat] {
  type Result <: MyBoolean
  def lessThan(n1: N1, n2: N2): Result
}

trait LessThanValueLowPriority {
  type LessThanValueAux[
    N1 <: Nat,
    N2 <: Nat,
    Res <: MyBoolean] = LessThanValue[N1, N2] { type Result = Res }

  implicit def anythingElse[
    N1 <: Nat,
    N2 <: Nat]: LessThanValueAux[N1, N2, MyFalse.type] = {
    new LessThanValue[N1, N2] {
      type Result = MyFalse.type
      def lessThan(n1: N1, n2: N2): Result = MyFalse
    }
  }
}

object LessThanValue extends LessThanValueLowPriority {
  implicit def zeroSucc[Rest <: Nat]: LessThanValueAux[Zero.type, Succ[Rest], MyTrue.type] = {
    new LessThanValue[Zero.type, Succ[Rest]] {
      type Result = MyTrue.type
      def lessThan(n1: Zero.type, n2: Succ[Rest]): Result = MyTrue
    }
  }

  implicit def succSucc[
    RestN1 <: Nat,
    RestN2 <: Nat,
    RestResult <: MyBoolean](
    implicit rec: LessThanValueAux[RestN1, RestN2, RestResult]):
      LessThanValueAux[Succ[RestN1], Succ[RestN2], RestResult] = {
    new LessThanValue[Succ[RestN1], Succ[RestN2]] {
      type Result = RestResult
      def lessThan(n1: Succ[RestN1], n2: Succ[RestN2]): Result = {
        rec.lessThan(n1.n, n2.n)
      }
    }
  }

  def lessThan[
    N1 <: Nat,
    N2 <: Nat,
    Result <: MyBoolean](n1: N1, n2: N2)(implicit ev: LessThanValueAux[N1, N2, Result]): Result = {
    ev.lessThan(n1, n2)
  }
}
