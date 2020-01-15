sealed trait Nat
case object Zero extends Nat
case class Succ[N <: Nat](n: N) extends Nat

trait Add[N1 <: Nat, N2 <: Nat] {
  type Result <: Nat
  def add(n1: N1, n2: N2): Result
}

object Add {
  type AddAux[
    N1 <: Nat,
    N2 <: Nat,
    Res <: Nat] = Add[N1, N2] { type Result = Res }

  implicit def zeroPlusN[N <: Nat]: AddAux[Zero.type, N, N] = {
    new Add[Zero.type, N] {
      type Result = N
      def add(n1: Zero.type, n2: N): Result = n2
    }
  }

  implicit def succPlusN[
    N <: Nat,
    M <: Nat,
    Res <: Nat](
    implicit rec: AddAux[N, M, Res]):
      AddAux[Succ[N], M, Succ[Res]] = {
    new Add[Succ[N], M] {
      type Result = Succ[Res]
      def add(n1: Succ[N], n2: M): Result = {
        Succ(rec.add(n1.n, n2))
      }
    }
  }

  def add[
    N1 <: Nat,
    N2 <: Nat,
    Res <: Nat](n1: N1, n2: N2)(implicit ev: AddAux[N1, N2, Res]): Res = {
    ev.add(n1, n2)
  }
}

