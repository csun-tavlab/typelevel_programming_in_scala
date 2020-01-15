sealed trait MyList
case object MyNil extends MyList
case class MyCons[A, Tail <: MyList](head: A, tail: Tail) extends MyList


trait Reverse[Input <: MyList, Accum <: MyList] {
  type Result <: MyList
  def reverse(input: Input, accum: Accum): Result
}

object Reverse {
  type ReverseAux[
    Input <: MyList,
    Accum <: MyList,
    Res <: MyList] = Reverse[Input, Accum] { type Result = Res }

  implicit def nil[Accum <: MyList]: ReverseAux[MyNil.type, Accum, Accum] = {
    new Reverse[MyNil.type, Accum] {
      type Result = Accum
      def reverse(input: MyNil.type, accum: Accum): Result = accum
    }
  }

  implicit def cons[
    Element,
    RestInput <: MyList,
    Accum <: MyList,
    Res <: MyList](
    implicit rec: ReverseAux[RestInput, MyCons[Element, Accum], Res]):
      ReverseAux[MyCons[Element, RestInput], Accum, Res] = {
    new Reverse[MyCons[Element, RestInput], Accum] {
      type Result = Res
      def reverse(input: MyCons[Element, RestInput], accum: Accum): Result = {
        rec.reverse(input.tail, MyCons(input.head, accum))
      }
    }
  }

  def reverse[
    List <: MyList,
    Result <: MyList](list: List)(implicit ev: ReverseAux[List, MyNil.type, Result]): Result = {
    ev.reverse(list, MyNil)
  }
}
