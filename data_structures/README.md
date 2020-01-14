# Data Structures #

At the heart of type-level programming are data structures.
Notably, with type-level programming, we usually need to define data structures a bit differently from how we usually define them.
To illustrate, let's start with the usual definition of natural numbers using [Peano arithmetic](https://en.wikipedia.org/wiki/Peano_axioms):

```scala
sealed trait Nat
// represents 0
case object Zero extends Nat
// represents 1 + n, for some Nat n
case class Succ(n: Nat) extends Nat

// 3, represented as a Nat
val three: Succ = Succ(Succ(Succ(Zero)))
```

The above code creates a `Nat` encoding `3`, storing it in the variable `three`.
Looking at the type of `three`, we can see that it's a `Succ`.
Notably, this is the most specific type we can give it, given the above definitions.
The only thing `Succ` tells us is that the number is _at least_ `1`, but we don't know anything more than that.
For example, `1` and `2` both have the same type as `3` above:

```scala
val one: Succ = Succ(Zero)
val two: Succ = Succ(Succ(Zero))
```

As shown, this datatype definition has already failed us; we lose information about the specific number we are encoding.

## Recovering Type Information ##

The specific point where we lost type information is in the definition of `Succ`, which is repeated below for convenience:

```scala
case class Succ(n: Nat) extends Nat
```

This loses information in two ways:

1. `n` can have a more specific type than `Nat` (e.g., `Succ`), but we don't maintain this more specific type in any way.
   We say that any `Nat` is accepted, and ignore the underlying specific type.
2. This constructor only ever creates values of type `Succ`, no matter which `n` is used.

The usual way to recover this type information is by introducing generics with the same _bound_ as the parameter type.
That is, instead of saying we take an arbitrary `Nat`, we take some `N` where `N <: Nat` (`N` is a subtype of `Nat`).
Additionally, instead of giving us back just a `Succ`, we get back a generic `Succ`, _parameterized by_ `N` (e.g., `Succ[N]`).
At compile time, `N` can be a more specific type than just `Nat`, so we gain more information this way.
A modified definition is below, which is more informative (also defined in `nat.scala`):

```scala
sealed trait Nat
case object Zero extends Nat
case class Succ[N <: Nat](n: N) extends Nat
```

We can use the above definition to make `one`, `two`, and `three` again:

```scala
val one: Succ[Zero.type] = Succ(Zero)
val two: Succ[Succ[Zero.type]] = Succ(Succ(Zero))
val three: Succ[Succ[Succ[Zero.type]]] = Succ(Succ(Succ(Zero)))
```

Now `one`, `two`, and `three` have different types, which is exactly what we wanted.
Looking at the types, the types mirror the actual values we have.

Note that Scala's type inference is smart enough to figure this all out, so we don't need to put explicit types on `one`, `two`, and `three` to get the same effect.
Explicit types are used here just for exposition.

## Another Example ##

As another example, let's consider lists, which are already generic.
The usual definition of singly-linked lists is below, using the prefix `My` to avoid any overlap with Scala's existing lists:

```scala
sealed trait MyList[A]
case class MyNil[A]() extends MyList[A]
case class MyCons[A](head: A, tail: MyList[A]) extends MyList[A]
```

(This definition doesn't make use of covariance and `Nothing`, unlike Scala's `List` definition; this has been omitted for simplicity.)
While this definition is generic (enabling different kinds of lists), this still loses information.
Notably, consider the following examples:

```scala
val first: MyNil[Int] = MyNil()
val second: MyCons[Int] = MyCons(1, MyNil())
val third: MyCons[Int] = MyCons(1, MyCons(2, MyNil()))
```

As with `Nat`, `second` and `third` have the same type as before, even though they encode different lists.
For typical linked lists, this is ok.
However, for our purposes, we want to be more specific.

Herein lies a question: how do we want to be more specific?
We could, for instance, merely encode the length of the list in the list.
This can be done with a definition like the following:

```scala
sealed trait MyList[A]
case class MyNil[A]() extends MyList[A]
case class MyCons[A, Tail <: MyList[A]](head: A, tail: Tail) extends MyList[A]

val first: MyNil[Int] = MyNil()
val second: MyCons[Int, MyNil[Int]] = MyCons(1, MyNil())
val third: MyCons[Int, MyCons[Int, MyNil[Int]]] = MyCons(1, MyCons(2, MyNil()))
```

Now `second` and `third` have a different type associated with them.
This difference in types encodes how many `MyCons` cells were used in the list, effectively encoding the list length.

The repetition of `Int` in `second` and `third` is a little bit quirky.
These are required to be repeats according to this `MyList` definition, ultimately because we enforce everything present to be a subtype of `List[A]` for some _single_ `A`.
If we so choose, we can lift this restriction so that individual list elements may be of different types.
This is shown below (also defined in `hlist.scala`):

```scala
sealed trait MyList
case object MyNil extends MyList
case class MyCons[A, Tail <: MyList](head: A, tail: Tail) extends MyList

val first: MyNil.type = MyNil
val second: MyCons[Int, MyNil.type] = MyCons(0, MyNil)
val third: MyCons[String, MyCons[Int, MyNil.type]] = MyCons("foo", MyCons(1, MyNil))
```

We now permit each list element to have its own type, so we can make lists containing both integers and strings.
Because of the way we've constructed our lists, we don't have some useless `List[Any]`, but rather a list type which tells us precisely what the type of each element is, along with its position in the list.
This type of list is known as a [heterogeneous list](https://wiki.haskell.org/Heterogenous_collections), because each element does not have to be the same.

## Summary ##

In summary, before we can do any type-level programming, we need to be sure we are working with data structures which preserve all the type information we need.
Generally, this means adding generics on types that we usually don't think of as generic, were the added generic types effectively save type information for us.
