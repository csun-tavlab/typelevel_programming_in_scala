sealed trait HMap
case object HEmptyMap extends HMap
case class HNonEmptyMap[Key, Value, Rest <: HMap](key: Key, value: Value, rest: Rest) extends HMap

trait Lookup[Input <: HMap, Key] {
  type Result
  def lookup(input: Input, key: Key): Result
}

trait LookupLowPriority {
  type LookupAux[
    Input <: HMap,
    Key,
    Res] = Lookup[Input, Key] { type Result = Res }

  implicit def nonHit[
    FoundKey,
    FoundValue,
    NeedKey,
    NeedValue,
    RestMap <: HMap](
    implicit rec: LookupAux[RestMap, NeedKey, NeedValue]):
      LookupAux[HNonEmptyMap[FoundKey, FoundValue, RestMap],
                NeedKey,
                NeedValue] = {
    new Lookup[HNonEmptyMap[FoundKey, FoundValue, RestMap], NeedKey] {
      type Result = NeedValue
      def lookup(input: HNonEmptyMap[FoundKey, FoundValue, RestMap], key: NeedKey): Result = {
        rec.lookup(input.rest, key)
      }
    }
  }
}

object Lookup extends LookupLowPriority {
  implicit def hit[
    Key,
    Value,
    RestMap <: HMap]: LookupAux[HNonEmptyMap[Key, Value, RestMap], Key, Value] = {
    new Lookup[HNonEmptyMap[Key, Value, RestMap], Key] {
      type Result = Value
      def lookup(input: HNonEmptyMap[Key, Value, RestMap], key: Key): Result = {
        input.value
      }
    }
  }

  def lookup[
    Input <: HMap,
    Key,
    Value](input: Input, key: Key)(implicit ev: LookupAux[Input, Key, Value]): Value = {
    ev.lookup(input, key)
  }
}
