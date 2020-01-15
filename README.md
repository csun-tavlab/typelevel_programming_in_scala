# Type-level Programming in Scala #

This is a short guide on type-level programming in Scala.
This is intended to cover some basics, along with some lessons learned from the marginal experience I have.
As a disclaimer, I am **not** an expert on the subject, and there are many cases where I flat-out don't understand something.
I'm writing this up because I recently worked on a project doing type-level Scala things, and I wanted to write down the lessons learned before they inevitably disappeared from my brain.

## Introduction - What is Type-level Programming? ##

Consider the following code snippet:

```scala
val x: Int = 7
```

At the type level, the compiler knows that `x` is of type `Int`; i.e., `x` is an integer.
However, the compiler doesn't specifically know _which_ integer `x` is, only that `x` is _an_ integer.
This prevents us from doing certain sorts of reasoning at compile time.
For example, consider the following code:

```scala
val x: Int = 7
val arr: Array[String] = Array("foo", "bar", "baz")
val value: String = arr(x)
```

The above code compiles, even though `arr` has no index `7`.
This is because we've lost some important information; we don't know specifically _which_ integer `x` is, only that `x` is an integer and that array access works for _some_ integers.
The compiler effectively optimistically assumes that `x` is a valid index, and the JVM will check this at runtime.
In this case, the code compiles, but it gives us a runtime error, precisely because we didn't have enough information to reason about this at compile time.

Type-level programming addresses this sort of problem by allowing us to reason about specific values _at compile time_, not just runtime.
With type-level programming, we can effectively encode `7` itself _as a type_.
In other words, we don't just say that `7` is an `Int`, but rather that `7` is an `Int` with value `7`.
Similarly, we don't just create an array of values, we create an array of length 3 holding `"foo"`, `"bar"`, and `"baz"`, where all this information is available _at the type level_.
This allows us to write much more specific type sigantures, because we can talk about specific values which are permitted.

### Limitations ###

While this sounds pretty neat, this is not a panacea, and there are some real limits.
Perhaps the greatest limit is that this sort of reasoning only works on data available at compile time.
For example, if we read in an input from the user at runtime, this means we don't know which specific value was input at compile time.
If the user inputs an integer, then at compile time we only know that it is an `Int`.
The repercussion of this is that anything this user input touches needs to be able to work with a generic `Int`, at least without having some code around it to recover type information (e.g., we look at the input at runtime, and break it into fixed types which are known at compile time).

Other limitations are more technical in nature.
It can be very awkward to write some of this type-level programming stuff in Scala, as the language isn't exactly designed for this.
We effectively push the bounds of Scala, and are trying to make it behave like a [dependently typed language](https://en.wikipedia.org/wiki/Dependent_type).
Not only can this make writing code unwieldy, this also leads to issues where the code you write can be _correct_, but the compiler nonetheless can't reason about it.
From personal experience, this doesn't come up in practice with simple problems, but I've had it come up with more complex ones (namely, trying to implement type inference at the type level using type-level unification...this was a fun project up until a dead stop).
Where Scala will fail here is that the type inferencer won't be able to figure something out, and it needs us to hold its hand.
Unfortunately, this is much easier said than done.

### Strengths ###

While the lack of support for reasoning about runtime values is major, you can still do a lot of things at compile time.
Notably, if you've written your code in a way that much of it is based on statically-known data, then anything operating on this statically-known data is eligible for type-level reasoning.
From personal experience, this is most advantageous when making an internal deeply-embedded DSLs in Scala.
This is a fancy way of saying "I'm making a programming language where the [abstract syntax tree (AST)](https://en.wikipedia.org/wiki/Abstract_syntax_tree) is itself a Scala data structure".
Normally, when constructing ASTs, we only have Scala's typechecking rules available to do so.
This makes it easy to construct an AST which encodes an ill-typed program.
We can use type-level programming in Scala to reject such ASTs _at Scala's compile time_.

## Rest of this Guide #

This guide is divided into lessons, with one directory per lesson.
This is intended to be followed in the following order:

1. Data Structures (`data_structures`)
2. Implicits (`implicits`)
3. Basic Operations (`basic_operations`)
4. Advanced Operations (`advanced_operations`)

I'm using Scala 2.12.6 for all code.
