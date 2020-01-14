# Implicits #

This lesson covers `implicit` in Scala, at least as it relates to type-level programming in Scala.
If you're already familiar with `implicit`, you should be able to skip this lesson.
I cover it because knowledge of `implicit` is crucial for type-level programming in Scala, and it's a feature that's atypically covered and understood.

[Implicit parameters](https://docs.scala-lang.org/tour/implicit-parameters.html) are simultaneously one of Scala's most powerful and most scary features.
As the name suggests, these are parameters which we (usually) do not pass explicitly, but are rather passed _implicitly_ (AKA automatically) for us.
Like regular parameters, these are passed at compile time, even though we do not (usually) write them.
Unlike regular parameters, the compiler tries to automatically figure out what the right implicit parameter is, and will automatically pass it for us.

## Basic Implicit Motivation and Usage ##
Let's say we want to define a `sum` method on a normal (non-heterogeneous) list:

```scala
sealed trait MyList[A] {
  def sum: A
}
// ...some more code...
MyList(1, 2, 3).sum // should return 6
```

While `sum` makes sense as a list method, it only makes sense for certain `A` types.
For example, it doesn't make sense to get the sum of `List[String]`, though it does make sense to get the sum of `List[Int]` or `List[Double]`.
What we _want_ here is a way to add a method specifically to `List[Int]` and `List[Double]`, but not to `List[String]`.
However, we cannot do this directly.

Towards implementing `sum`, let's think about what `sum` really needs from its internal `A` values.
It should need:

1. A way of knowing how to add two values of type `A`.
2. A way of knowing what `0` is for the given `A` type (assuming the sum of an empty list is `0`).

Since `sum` itself doesn't have a way of reasoning about `A`, it's up to the caller to provide this information.
With this in mind, we can redefine `sum` to be something like the following:

```scala
sealed trait MyList[A] {
  def sum(zero: A, add: (A, A) => A): A
}
```

This way, we can produce the `sum` of an arbitrary list, as long as the caller tells us what `zero` and `add` is.
With this in mind, we can do something like:

```scala
MyList(1, 2, 3).sum(0, (a: Int, b: Int) => a + b)
MyList(1.1, 2.2, 3.3).sum(0.0, (a: Double, b: Double) => a + b)
```

This works, but it's obnoxious, as we keep needing to pass these parameters to `sum`.
Additionally, the compiler seems to have enough information to know what `zero` and `add` values need to be used.
For example, `0` should be used for MyList[Int]`, and `0.0` should be used for `MyList[Double]`.
this information stays the same no matter which `MyList[Int]` and `MyList[Double]` we are referring to.

We can make this situation a little bit better by introducing some objects which put together `zero` and `add`.
A modified variant of `sum` combining these components is shown below:

```scala
sealed trait SumMaker[A] {
  def zero: A
  def add(a: A, b: A): A
}
object IntSum extends SumMaker[Int] {
  def zero: Int = 0
  def add(a: Int, b: Int): Int = a + b
}
object DoubleSum extends SumMaker[Double] {
  def zero: Double = 0.0
  def add(a: Double, b: Double): Double = a + b
}
sealed trait MyList[A] {
  def sum(maker: SumMaker[A]): A
}
// more code
MyList(1, 2, 3).sum(IntSum)
MyList(1.1, 2.2., 3.3).sum(DoubleSum)
```

At this point, we've cut down on the number of parameters needed to `sum`, though we still need to pass a `SumMaker` instance.
If we try to pass the wrong `SumMaker` instance, the compiler will at least give us a type error, even though the compiler should have enough information to just figure this out.

### Enter Implicits ###
We can rectify this situation via the use of `implicit`.
Notably, we need to add in the following information:

1. Tell the compiler that the `maker` parameter to `sum` should be passed implicitly.
   In other words, we tell the compiler to just figure out what the parameter should be.
2. Mark the `IntSum` and `DoubleSum` objects as being usable as `implicit` parameters.
   The compiler will only consider things explicitly marked as being usable as `implicit` parameters for implicit parameter resolution.

We can do these things like so (also defined in `implicits.scala`):

```scala
sealed trait SumMaker[A] {
  def zero: A
  def add(a: A, b: A): A
}

object SumMaker {
  implicit object IntSum extends SumMaker[Int] {
    def zero: Int = 0
    def add(a: Int, b: Int): Int = a + b
  }
  implicit object DoubleSum extends SumMaker[Double] {
    def zero: Double = 0.0
    def add(a: Double, b: Double): Double = a + b
  }
}

sealed trait MyList[A] {
  def sum(implicit maker: SumMaker[A]): A
}
case class MyNil[A]() extends MyList[A] {
  def sum(implicit maker: SumMaker[A]): A  = maker.zero
}
case class MyCons[A](head: A, tail: MyList[A]) extends MyList[A] {
  def sum(implicit maker: SumMaker[A]): A = maker.add(head, tail.sum)
}

object Examples {
  import SumMaker._

  def main(args: Array[String]) {
    println(MyCons(1, MyCons(2, MyCons(3, MyNil[Int]()))).sum)
    println(MyCons(1.1, MyCons(2.2, MyCons(3.3, MyNil[Double]()))).sum)
  }
}
```

As shown, the `IntSum` and `DoubleSum` objects are marked with `implicit`, making them available as implicit parameters.
`implicit` objects are not permitted to be at the toplevel, so we put them in a new `SumMaker` object.
The parameter to `sum` is marked as `implicit`, allowing the compiler to figure out what to pass.
Finally, in the examples themselves, we import the objects marked with `implicit` via `import SumMaker._`; this makes them available at the downstream calls to `sum`.
Because the parameter to `sum` is being passed implicitly, we no longer explicitly give any parameters to `sum`.

Note the call to `tail.sum` in `MyCons`.
This call also needs a `maker` parameter, though it is not passed explicitly.
In this case, the call uses the passed `maker` parameter for this task.
To understand why, observe:

- `tail.sum` needs an implicit parameter of type `SumMaker[A]`
- `maker` is in scope, is of type `SumMaker[A]`, and is marked `implicit`

As such, the passed `maker` parameter is itself passed to `tail.sum`; the types line up, it itself is marked `implicit`, and so it is passed along too.


## Beyond `object` ##
`object`s aren't the only thing that can be marked with `implicit`; `def` can also be marked as `implicit`.
This is shown below (also defined in `implicits_def.scala`):

```scala
sealed trait SumMaker[A] {
  def zero: A
  def add(a: A, b: A): A
}

sealed trait MyList[A] {
  def sum(implicit maker: SumMaker[A]): A
}
case class MyNil[A]() extends MyList[A] {
  def sum(implicit maker: SumMaker[A]): A  = maker.zero
}
case class MyCons[A](head: A, tail: MyList[A]) extends MyList[A] {
  def sum(implicit maker: SumMaker[A]): A = maker.add(head, tail.sum)
}

object Examples {
  implicit def makerInt: SumMaker[Int] = {
    new SumMaker[Int] {
      def zero: Int = 0
      def add(a: Int, b: Int): Int = a + b
    }
  }

  implicit def makerDouble: SumMaker[Double] = {
    new SumMaker[Double] {
      def zero: Double = 0.0
      def add(a: Double, b: Double): Double = a + b
    }
  }

  def main(args: Array[String]) {
    println(MyCons(1, MyCons(2, MyCons(3, MyNil[Int]()))).sum)
    println(MyCons(1.1, MyCons(2.2, MyCons(3.3, MyNil[Double]()))).sum)
  }
}
```

Externally, this example does exactly the same thing as the prior example.
However, internally, it's behaving differently.
Each call in the code to `sum` ends up implicitly calling either `makerInt` or `makerDouble`.
This is very important, because this means that arbitrary code can be executed (at runtime) when implicit parameters are passed.
Additionally, `def`s themselves can have `implicit` parameters (as our `sum` definition does), **even implicitly-defined `def`s**.
This is what gives us the capability to not just define type-level data structures, but perform computation on them: the act of finding an appropriate implicitly-passed value can itself trigger further search for implicity-passed values, and this search can **even be recursive**.
This gives us a Turing-complete programming mechanism that operates at Scala's compile time.
This mechanism is discussed more in the next lesson.
