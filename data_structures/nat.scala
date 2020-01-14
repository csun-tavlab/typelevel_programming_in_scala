sealed trait Nat
case object Zero extends Nat
case class Succ[N <: Nat](n: N) extends Nat
