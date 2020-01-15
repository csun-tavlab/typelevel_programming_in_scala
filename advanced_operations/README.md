# Advanced Operations #

For our purposes, advanced operations combine multiple data structures, and they can also involve multiple operations into one.

## Length ##

See `length.scala`.
Examples follow:

```
scala> import Length._
import Length._

scala> length(MyCons("alpha", MyCons(2, MyCons(true, MyNil))))
res0: Succ[Succ[Succ[Zero.type]]] = Succ(Succ(Succ(Zero)))
```

## Access ##

This accesses an element at a given index in a 0-indexed list.
Does not compile if the requested index is out of bounds.
See `access.scala`.
Examples follow:

```
scala> import Access._
import Access._

scala> val list = MyCons("foo", MyCons(1, MyCons(true, MyNil)))
list: MyCons[String,MyCons[Int,MyCons[Boolean,MyNil.type]]] = MyCons(foo,MyCons(1,MyCons(true,MyNil)))

scala> access(list, Zero)
res0: String = foo

scala> access(list, Succ(Zero))
res1: Int = 1

scala> access(list, Succ(Succ(Zero)))
res2: Boolean = true

scala> access(list, Succ(Succ(Succ(Zero))))
<console>:16: error: could not find implicit value for parameter ev: Access.AccessAux[MyCons[String,MyCons[Int,MyCons[Boolean,MyNil.type]]],Succ[Succ[Succ[Zero.type]]],Element]
       access(list, Succ(Succ(Succ(Zero))))
             ^
```

### Append ###

Appends two heterogeneous lists together.
This effectively implements the following value-level algorithm:

```scala
def append[A](l1: List[A], l2: List[A]): List[A] = {
  l1 match {
    case Nil => l2
    case Cons(a, as) => Cons(a, as.append(l2))
  }
}
```

See `append.scala` for the code.
Examples follow:

```
scala> import Append._
import Append._

scala> append(MyCons(1, MyCons(2, MyNil)), MyCons("alpha", MyCons("beta", MyCons(true, MyNil))))
res0: MyCons[Int,MyCons[Int,MyCons[String,MyCons[String,MyCons[Boolean,MyNil.type]]]]] = MyCons(1,MyCons(2,MyCons(alpha,MyCons(beta,MyCons(true,MyNil)))))
```

## Reverse ##

This is based on the following value-based implementation:

```scala
def reverse[A](list: List[A], accum: List[A]): List[A] = {
  list match {
    case Nil => accum
    case Cons(head, tail) => reverse(tail, Cons(head, accum))
  }
}

def reverse[A](list: List[A]): List[A] = {
  reverse(list, Nil)
}
```

See `reverse.scala`.
Examples follow:

```
scala> import Reverse._
import Reverse._

scala> reverse(MyCons(1, MyCons(2, MyCons(3, MyNil))))
res0: MyCons[Int,MyCons[Int,MyCons[Int,MyNil.type]]] = MyCons(3,MyCons(2,MyCons(1,MyNil)))

scala> reverse(MyCons("foo", MyCons(1, MyNil)))
res1: MyCons[Int,MyCons[String,MyNil.type]] = MyCons(1,MyCons(foo,MyNil))
```

## Sum ##

This gets the sum of the elements of a heterogenous list.
Requires that all elements are natural numbers.
Makes use of addition from the basic operations; this combines one operation for addition, and one operation to compute the sum.
This is based on the following value-based implementation:

```scala
def sum(list: List[Int]): Int = {
  list match {
    case Nil => 0
    case Cons(head, tail) => head + sum(tail)
  }
}
```

See `sum.scala`.
Examples follow:

```
scala> import Sum._
import Sum._

scala> sum(MyNil)
res0: Zero.type = Zero

scala> sum(MyCons(Zero, MyNil))
res1: Zero.type = Zero

scala> sum(MyCons(Succ(Zero), MyCons(Succ(Succ(Zero)), MyCons(Succ(Succ(Succ(Zero))), MyNil))))
res2: Succ[Succ[Succ[Succ[Succ[Succ[Zero.type]]]]]] = Succ(Succ(Succ(Succ(Succ(Succ(Zero))))))
```

## Lookup ##

Introduces hetrogeneous _maps_.
These are basically just heterogeneous lists, where each element is a key/value pair.
Lookup specifically looks up a particular key in the map.
Note that the lookup is based on the type level, which is why natural numbers are used as keys below (each has its own unique type).
Examples follow:

```
scala> import Lookup._
import Lookup._

scala> val map = HNonEmptyMap(Zero, "alpha", HNonEmptyMap(Succ(Zero), 1, HNonEmptyMap(Succ(Succ(Zero)), true, HEmptyMap)))
map: HNonEmptyMap[Zero.type,String,HNonEmptyMap[Succ[Zero.type],Int,HNonEmptyMap[Succ[Succ[Zero.type]],Boolean,HEmptyMap.type]]] = HNonEmptyMap(Zero,alpha,HNonEmptyMap(Succ(Zero),1,HNonEmptyMap(Succ(Succ(Zero)),true,HEmptyMap)))

scala> lookup(map, Zero)
res0: String = alpha

scala> lookup(map, Succ(Zero))
res1: Int = 1

scala> lookup(map, Succ(Succ(Zero)))
res2: Boolean = true

scala> lookup(map, Succ(Succ(Succ(Zero))))
<console>:16: error: could not find implicit value for parameter ev: Lookup.LookupAux[HNonEmptyMap[Zero.type,String,HNonEmptyMap[Succ[Zero.type],Int,HNonEmptyMap[Succ[Succ[Zero.type]],Boolean,HEmptyMap.type]]],Succ[Succ[Succ[Zero.type]]],Value]
       lookup(map, Succ(Succ(Succ(Zero))))
             ^
```
