sealed trait Nat
case object Zero extends Nat
case class Succ[N <: Nat](n: N) extends Nat

trait LessThan[N1 <: Nat, N2 <: Nat]

object LessThan {
  implicit def zeroLessThanSucc[Rest <: Nat]: LessThan[Zero.type, Succ[Rest]] = null
  implicit def succLessThanSucc[
    RestN1 <: Nat,
    RestN2 <: Nat](
    implicit rec: LessThan[RestN1, RestN2]): LessThan[Succ[RestN1], Succ[RestN2]] = null

  def lessThan[N1 <: Nat, N2 <: Nat](n1: N1, n2: N2)(implicit ev: LessThan[N1, N2]) {}
}
