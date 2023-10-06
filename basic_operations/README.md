# Basic Operations #

In this lesson, we will cover the use of basic type-level operations.
For our purposes, "basic" means:

- There is only one operation in play
- The data structures in play have at most one generic

We'll show this primarily by example.

## Natural Numbers ##
Our first examples concern natural numbers, which are presented below as they were in the Data Structures lesson:

```scala
sealed trait Nat
case object Zero extends Nat
case class Succ[N <: Nat](n: N) extends Nat
```

From here, we will define some operations that work with natural numbers.

### Less Than (`<`) ###

As a first operation, we'll define what it means for one natural number to be less than another.
From Peano arithmetic, we know:

- `0 < succ(n)`, for any natural number `n`
- `succ(n1) < succ(n2)`, as long as `n1 < n2`

From here, we can start to define this operation.
Before getting into how we can do this at the type level, let's first think of how we may implement this at the usual value level.
We might define something like the following:

```scala
def lessThan(n1: Nat, n2: Nat) {
  (n1, n2) match {
    case (Zero, Succ(_)) => ()
    case (Succ(newN1), Succ(newN2)) => lessThan(newN1, newN2)
    case _ => {
      assert(false)
      ()
    }
  }
}
```

This should follow directly from the prior premises, though with a twist: instead of treating this like a usual predicate (returning a boolean value), we instead treat this as a function that either halts successfully (if `n1 < n2`), or crashes (if `!(n1 < n2)`).
While this looks silly at the value level, at the type level this makes sense: we can determine, at compile time, that `n1 < n2`.
As such, we instead take the behavior that we successfully compile if `n1 < n2`, and fail to compile otherwise.
This matches up with how one may want to use type-level programming, e.g., to verify that something holds at compile time.
(And if this doesn't match up with what you want, the next example covers how to make this behave more like a normal predicate; we intentionally don't start with this because it's strictly more complicated.)
From here, with a decent chunk of work, we can make this operate at the type level.

First of all, to make this work at the type level, we effectively need a way to inform the typechecker about how this operation relates inputs to outputs.
This is what normal `def`s do at the value level, but now we need to do this at the type level.
The usual way of lifting this information to the type level is by introducing the operation itself _as a type_, where the inputs become type variables.
(Outputs should generally be handled slightly differently; the next example covers that.)
With this in mind, let's introduce a new type talking about the inputs:

```scala
trait LessThan[N1 <: Nat, N2 <: Nat]
```

This trait is currently empty, which may look strange.
Keep in mind that any operations on the trait operate at the value level, but here we are concerned only with the type level.
As such, we don't need any operations on the trait itself.

Once we have the inputs mapped to a type, we can start to talk about what it _means_ to have a value of a given operation type.
For example, if we have `LessThan[Zero.type, Succ[Zero.type]]`, this effectively says that `0 < 1`.
Similarly, if we have `LessThan[Zero.type, Zero.type]`, this effectively says `0 < 0`, which is clearly undesired.
Our goal is to define implicit `def`s which will _construct_ `LessThan` objects which make sense, while avoiding those which don't make sense (e.g., `0 < 0`).
Towards defining these implicits, typically each case in a pattern match becomes a separate `def`.
With this in mind, let's define code handling the first case:

```scala
object LessThan {
  implicit def zeroLessThanSucc[Rest <: Nat]: LessThan[Zero.type, Succ[Rest]] = null
}
```

As before, Scala doesn't allow `implicit`s to be defined at the toplevel, hence we put this `implicit` in an object.
The fact that this is an object, and similarly its name, is irrelevant; an `object` is used just to stash these somewhere, and `LessThan` is used just as an indicator that this is related to the `LessThan` trait.

The implicit def is the interesting part.
To understand this, it makes more sense to start at the return type of the `def`, namely: `LessThan[Zero.type, Succ[Rest]]`.
That is, this `def` says how to introduce the information that `0 < succ(Rest)`.
From here, we can look at the generics defined on the `def`, namely that `Rest <: Nat`.
This says that `Rest` must itself be a natural number, which makes sense in the context of `0 < succ(Rest)`.
The righthand side of the `def` (`= null`) says how to actually introduce something of that type.
This probably is the most counterintuitive part, given that we're usually trained to fear `null`.
`null` is used in the code because the object _value_ itself is irelevant; it has no methods on it, and thus can't really be used for much of anything.
However, the object _type_ is the important part, hence the `LessThan[Zero.type, Succ[Rest]]` part.
If we really wanted to, we could return an object by replacing `null` with something like `new LessThan[Zero.type, Succ[Rest]] {}`; this doesn't effectively change anything other than allocating an unnecessary object.
As for the name of the `def` (`zeroLessThanSucc`), this name is only for human consumption, and describes what it does.
The compiler does it's job entirely with the return type.

From here, we need the case for two `succ` values, which ends up entailing recursion.
This can be done as follows (copying over the same `zeroLessThanSucc` from before):

```scala
object LessThan {
  implicit def zeroLessThanSucc[Rest <: Nat]: LessThan[Zero.type, Succ[Rest]] = null
  implicit def succLessThanSucc[
    RestN1 <: Nat,
    RestN2 <: Nat](
    implicit rec: LessThan[RestN1, RestN2]): LessThan[Succ[RestN1], Succ[RestN2]] = null
}
```

Here, we start again from the return type: `LessThan[Succ[RestN1], Succ[RestN2]]`.
This explicitly says what it means for one `Succ` to be less than another; this is effectively `succ(RestN1) < succ(RestN2)`.
Looking at the generics, `RestN1` and `RestN2` are themselves natural numbers.
The recursive call is where things get interesting.
We want to say that `RestN1` needs to be less than `RestN2`.
This is accomplished with the implicit parameter: `implicit rec: LessThan[RestN1, RestN2]`.
This implicit parameter effectively says `RestN1 < RestN2`.

Putting this all together, you can think of operations in the following way:

- The returned "value" (really, type) of the operation goes where the return type is
- Any calls (recursive or otherwise) are implicit parameters
- Any (type) parameters go on the generics for the implicit def

With the above implicits in place, we can define an entry point for less than:

```scala
object LessThan {
  // Same code as above
  
  def lessThan[N1 <: Nat, N2 <: Nat](n1: N1, n2: N2)(implicit ev: LessThan[N1, N2]) {}
}
```

This code is defined in `less_than.scala`.
The `lessThan` function above takes two natural numbers, preserving their underlying type with type parameters.
We then ask the compiler to effectively _prove_ that `N1` is less than `N2` with the implicit parameter.
The parameter `ev` acts as this proof; "ev" is short for "evidence" (this name is common in Scala type-level code).
If `N1 < N2`, then this will compile; otherwise, this will fail to compile.
In this case, the return value is unimportant, hence this returns `Unit`.

We can test this code out by compiling it and loading up a REPL:

```
import LessThan._
scala> lessThan(Zero, Succ(Zero))

scala> lessThan(Zero, Succ(Succ(Zero)))

scala> lessThan(Succ(Zero), Zero)
<console>:15: error: could not find implicit value for parameter ev: LessThan[Succ[Zero.type],Zero.type]
       lessThan(Succ(Zero), Zero)
               ^
```

As shown, `0 < 1` and `0 < 2` both compile and run.
However, `1 < 0` does not, and it fails with the error message that an implicit value could not be found.
This follows from the fact that our implicit defs were not able to show that `1 < 0`.
We don't _want_ this to compile, and it doesn't.
For once, a compilation error means success!

### Less Than With Return Value (version 1) ###

For this example, we'll revisit the `LessThan` operation from before.
However, instead of treating the result as compiles/does not compile, we will instead make it return a boolean value.
With this in mind, we'll instead use the following value-based implementation as a basis:

```scala
def lessThanValue(n1: Nat, n2: Nat): Boolean = {
  (n1, n2) match {
    case (Zero, Succ(_)) => true
    case (Succ(_), Zero) => false
    case (Zero, Zero) => false
    case (Succ(newN1), Succ(newN2)) => lessThan(newN1, newN2)
  }
}
```

We intentionally introduce two specific patterns handling `false` instead of a catch-all `_`; the next example will show a version that does this.
Other than that, this should be straightforward.

First, we need to introduce a new type for booleans.
The issue is that Scala's `Boolean` loses type information.
Specifically, `true` and `false` do not have a specific type associated with them; both are of type `Boolean`.
However, we'll need a separate type for this.
As such, we introduce our own boolean type:

```scala
sealed trait MyBoolean
case object MyTrue extends MyBoolean
case object MyFalse extends MyBoolean
```

The `My` prefix is to avoid any overlap with Scala libraries.
As for the natural numbers, we'll use the same representation as before.

From here, we can define a `trait` corresponding to the operation.
As before, we'll have two inputs which are natural numbers.
However, we now have an _output_, which should be a `MyBoolean`.
Outputs _can_ be represented with additional generic type parameters, though it's better to represent these instead with [abstract type members](https://docs.scala-lang.org/tour/abstract-type-members.html).
For our purposes, abstract type members are effectively generic types which we don't explicitly write.
We use them here because the compiler tends to have an easier time finding implicit values when the inputs are generic types, and the outputs are abstract type members.
(Note: I have no idea why, and this doesn't seem universally true.)
With all this in mind, we will define a `trait` specific to this version of less than:

```scala
trait LessThanValue[N1 <: Nat, N2 <: Nat] {
  type Result <: MyBoolean
  def lessThan(n1: N1, n2: N2): Result
}
```

This object now has a lot more going on than before.
We now have an abstract type member, namely `Result`.
We also have a method which takes our inputs and produces our outputs.
This method is needed if we ever want to have the specific output value available at _runtime_.
That is, we can now have values which are accessible at compile time but not runtime (kind of the opposite of the usual situation).

The one issue with introducing abstract type members is that it's difficult to refer to them explicitly.
In some cases we can get away from this, but in general, we cannot.
As such, we usually also introduce a type alias which makes the output of an operation instead an explicit type parameter.
This is shown below:

```scala
// everything needs to be in an object, as before
object LessThanValue {
  type LessThanValueAux[
    N1 <: Nat,
    N2 <: Nat,
    Res <: MyBoolean] = LessThanValue[N1, N2] { type Result = Res }
}
```

The type alias `LessThanValueAux` makes it easy to refer to a `LessThanValue` with a specific abstract type member.
It's customary to use `Aux` for this purpose; this is commonly called the [aux pattern](https://gigiigig.github.io/posts/2015/09/13/aux-pattern.html), even though its more of a hack than a proper design pattern.

From here, we can define implicits for our cases.
We'll start with the case of `0 < succ(n)`, for some natural number `n`:

```scala
object LessThanValue {
  // same LessThanValueAux as before
  implicit def zeroSucc[Rest <: Nat]: LessThanValueAux[Zero.type, Succ[Rest], MyTrue.type] = {
    new LessThanValue[Zero.type, Succ[Rest]] {
      type Result = MyTrue.type
      def lessThan(n1: Zero.type, n2: Succ[Rest]): Result = MyTrue
    }
  }
}
```

This behaves similarly to the code before, but now:

- We explicitly say that this should have a result of `MyTrue` (we return `LessThanValueAux[Zero.type, Succ[Rest], MyTrue.type]`)
- We actually create a `LessThanValue` object.
  Since these objects are no longer empty, we can't get away with returning `null` anymore.
- The body of the `lessThan` method only needs to return `MyTrue`.
  No actual work is required; the compiler did the hard part in figuring out that we have `0 < succ(n)`.

The rest of the non-recursive cases should be straightforward:

```scala
object LessThanValue {
  // same code as before
  implicit def succZero[Rest <: Nat]: LessThanValueAux[Succ[Rest], Zero.type, MyFalse.type] = {
    new LessThanValue[Succ[Rest], Zero.type] {
      type Result = MyFalse.type
      def lessThan(n1: Succ[Rest], n2: Zero.type): Result = MyFalse
    }
  }

  implicit def zeroZero: LessThanValueAux[Zero.type, Zero.type, MyFalse.type] = {
    new LessThanValue[Zero.type, Zero.type] {
      type Result = MyFalse.type
      def lessThan(n1: Zero.type, n2: Zero.type): Result = MyFalse
    }
  }
}
```

Now for the recursive case:

```scala
object LessThanValue {
  // same code as before
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
}
```

This one is a bit more interesting.
Notably:

- We need to introduce a generic type for the result (`RestResult`)
- We need to return `RestResult`
- In defining the `lessThan` method, we need to call the implicit parameter (`rec`)
- We can use `n1.n` and `n2.n` directly; the types have already told us they are specifically `Succ` instances, so there is no need for casting or pattern matching

From here, we can define the same sort of entry point as before:

```scala
object LessThanValue {
  // same code as before
  def lessThan[
    N1 <: Nat,
    N2 <: Nat,
    Result <: MyBoolean](n1: N1, n2: N2)(implicit ev: LessThanValueAux[N1, N2, Result]): Result = {
    ev.lessThan(n1, n2)
  }
}
```

In this case, we need to call the `lessThan` method on `ev` with our explicitly provided parameters.

All of this code is in `less_than_value_v1.scala`.
Once this is compiled, we can try it out at the REPL:

```
scala> import LessThanValue._
import LessThanValue._

scala> lessThan(Succ(Zero), Succ(Succ(Zero)))
res0: MyTrue.type = MyTrue

scala> lessThan(Succ(Succ(Zero)), Succ(Zero))
res1: MyFalse.type = MyFalse
```

The first call to `lessThan` effectively asks `1 < 2`, and the second is effectively `2 < 1`.
As shown, we get the expected results of `MyTrue` and `MyFalse`.
More importantly, look at the type of `res0` and `res1`.
These aren't `MyBoolean`, but rather the more specific `MyTrue.type` and `MyFalse.type`: the compiler knew exactly the types of the expected values!

### Less Than With Return Value (version 2) ###

We revisit the same example as before.
This time, we want to merge together our two `false` cases, yielding the following:

```scala
def lessThanValue(n1: Nat, n2: Nat): Boolean = {
  (n1, n2) match {
    case (Zero, Succ(_)) => true
    case (Succ(newN1), Succ(newN2)) => lessThan(newN1, newN2)
    case _ => false
  }
}
```

The catch-all case will handle all possible cases.
With pattern matching, this isn't an issue, since we'll only hit the catch-all case if we don't hit the other cases first.
When doing type-level programming, this is an issue, because we don't have the same sort of control over the order in which `implicit`s are attempted.
To show this, consider the following code, reusing components from before:

```scala
object LessThanValueBROKEN {
  type LessThanValueAux[
    N1 <: Nat,
    N2 <: Nat,
    Res <: MyBoolean] = LessThanValue[N1, N2] { type Result = Res }

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

  implicit def anythingElse[
    N1 <: Nat,
    N2 <: Nat]: LessThanValueAux[N1, N2, MyFalse.type] = {
    new LessThanValue[N1, N2] {
      type Result = MyFalse.type
      def lessThan(n1: N1, n2: N2): Result = MyFalse
    }
  }

  def lessThan[
    N1 <: Nat,
    N2 <: Nat,
    Result <: MyBoolean](n1: N1, n2: N2)(implicit ev: LessThanValueAux[N1, N2, Result]): Result = {
    ev.lessThan(n1, n2)
  }
}
```

If we go to compile and run this, we'll quickly find that it doesn't work as intended:

```
scala> import LessThanValue._
import LessThanValue._

scala> lessThan(Succ(Zero), Zero)
res0: MyFalse.type = MyFalse

scala> lessThan(Succ(Zero), Succ(Succ(Zero)))
res1: MyFalse.type = MyFalse
```

While it correctly says that `1 < 0` is false, it now says that `1 < 2` is also false.
The issue here is that we have multiple implicits that can apply, and the compiler happens to be choosing one we don't want.
(In some circumstances, the Scala compiler will reject this code altogether and say that it's ambiguous which implicit to use, but this isn't happening here for reasons I don't understand.)

We do have some control over the order in which implicits are applied.
Specifically, if multiple implicits are in scope, it will try those which are immediately available first, and then it will try those which are in any parent classes.
In this manner, we can impose a total ordering on when implicits are attempted if we so choose, recovering pattern matching semantics (albeit, in an awkward way).
This is shown in the code below, held in `less_than_value_v2.scala`:

```scala
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
```

Since we want to try the catch-all case (`anythingElse`) last, we intentionally put this in a parent class of `LessThanValue`.
(It's idiomatic to mark such classes as being `LowPriority` as has been done here, though this isn't necessary.)
We then put the more specific cases to try directly in `LessThanValue`.
Compiling and running this works:

```
scala> import LessThanValue._
import LessThanValue._

scala> lessThan(Succ(Zero), Zero)
res0: MyFalse.type = MyFalse

scala> lessThan(Succ(Zero), Succ(Succ(Zero)))
res1: MyTrue.type = MyTrue
```

### Addition ###

In Peano arithmetic, addition can be implemented as follows:

- `0 + n = n`
- `succ(n) + m = succ(n + m)`

A more traditional value-based representation follows:

```scala
def add(n1: Nat, n2: Nat): Nat = {
  n1 match {
    case Zero => n2
    case Succ(n) => Succ(add(n, n2))
  }
}
```
  
Code implementing this is in `addition.scala`.
This does not introduce anything new, but serves as another example.
Some examples follow:

```
scala> import Add._
import Add._

scala> add(Succ(Zero), Zero)
res1: Succ[Zero.type] = Succ(Zero)

scala> add(Succ(Zero), Succ(Zero))
res2: Succ[Succ[Zero.type]] = Succ(Succ(Zero))

scala> add(Succ(Succ(Succ(Zero))), Succ(Succ(Zero)))
res3: Succ[Succ[Succ[Succ[Succ[Zero.type]]]]] = Succ(Succ(Succ(Succ(Succ(Zero)))))
```

