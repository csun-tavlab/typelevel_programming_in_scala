sealed trait MyList
case object MyNil extends MyList
case class MyCons[A, Tail <: MyList](head: A, tail: Tail) extends MyList

trait Append[First <: MyList, Second <: MyList] {
  type Result <: MyList
  def append(first: First, second: Second): Result
}

object Append {
  type AppendAux[
    First <: MyList,
    Second <: MyList,
    Res <: MyList] = Append[First, Second] { type Result = Res }

  implicit def nil[Second <: MyList]: AppendAux[MyNil.type, Second, Second] = {
    new Append[MyNil.type, Second] {
      type Result = Second
      def append(first: MyNil.type, second: Second): Result = second
    }
  }

  implicit def cons[
    Head,
    Rest <: MyList,
    OtherList <: MyList,
    RestAppend <: MyList](
    implicit rec: AppendAux[Rest, OtherList, RestAppend]):
      AppendAux[MyCons[Head, Rest], OtherList, MyCons[Head, RestAppend]] = {
    new Append[MyCons[Head, Rest], OtherList] {
      type Result = MyCons[Head, RestAppend]
      def append(first: MyCons[Head, Rest], second: OtherList): Result = {
        MyCons(first.head, rec.append(first.tail, second))
      }
    }
  }

  def append[
    First <: MyList,
    Second <: MyList,
    Result <: MyList](first: First, second: Second)(implicit ev: AppendAux[First, Second, Result]): Result = {
    ev.append(first, second)
  }
}
