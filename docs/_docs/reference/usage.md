---
title: Usage
---

## Introduction to Library Definitions

All bindings by Slinc take place in library traits, groups of like methods that reflect C functions. When defining a library trait, name is not particularly important. One merely defines an object that derives `Lib`. Method bindings are done via a method binding with a name matching the C function in question, and parameters with types that match the C function in question.

```scala
trait StdLib derives Lib:
  def abs(i: CInt): CInt
```

The above defines a binding to the C standard library's `abs` method. Since the C definition of `abs` is defined like `int abs(int i)`, the scala version of the method is defined with a single input that has the type `CInt` and the return type `CInt`.

## Types

In Slinc, you are allowed to use most of the primitive types to bind to C functions. These types should be used to represent Integer or Floating-point types of specific widths. Refer to the following table to see the corresponding byte-widths:

|JDK Primitive|Width|Kind|
|---|---|---|
|Byte|1 byte|Integer|
|Short|2 bytes|Integer|
|Int|4 bytes|Integer|
|Long|8 bytes|Integer|
|Float|4 bytes|Floating Point|
|Double|8 bytes|Floating Point|

When creating a binding, you should make sure the bit-width and kind of type matches the C function you're wanting to bind to.

## Host dependent types

Using the built in JVM primitives for a binding is quick and easy if you know the platform you're targeting well, but they will not result in a platform independent binding. It's for this reason you should use host dependent types. These types will map to the current platform wherever they're used, if you're running on a Slinc supported platform. Below is a table of host dependent types and their corresponding C types.

|Slinc|C|
|-----|-|
|CChar|char|
|CShort|short|
|CInt|int|
|CLong|long|
|CLongLong|long long|
|CFloat|float|
|CDouble|double|
|SizeT|size_t|
|TimeT|time_t|


Since these types are only guaranteed to be defined at runtime, interacting with them can be difficult.

## Pointers

Pointers are represented in Slinc with the `Ptr` type. For example, `Ptr[Int]` is a pointer to native memory that should be readable as a JVM Int.

The pointer class' operations are powered by the `DescriptorOf` typeclass.

The list of operations available on a `Ptr[A]`:

* `apply(n: Bytes)` - offsets the pointer by `n` bytes
* `to[A]` - casts the pointer to type `A`
* `!ptr` - dereferences a pointer (requires `Receive[A]` be defined)
* `!ptr = 5` - copy data into a pointer  (requires `Send[A]` be defined)
* `Ptr.blank[A](n: Int)` - create a blank space in native memory that can store `n` consecutive instances of `A` (requires `LayoutOf[A]` be defined)
* `Ptr.copy[A](a: A)` - create a copy of `a` in native memory (requires `Send[A]`)
* `Ptr.asArray(size: Int)` - attempts to copy the data at the pointer into an Array of size `size` (requires `Receive[A]`). This is a very dangerous operation that can crash your program if you don't have all the data you need. 

## Structs

The analog for C structs in Slinc are case classes that derive the `Struct` type class. An example analog for the div_t struct in the C standard library is defined as such:

```scala
case class div_t(quot: Int, rem: Int) derives Struct
```

These struct analogs can be composed with any type that has a `DescriptorOf` defined for it.

## va_list

The `va_list` type for C is supported via the `VarArgs` and `VarArgsBuilder` types. 

A C function that takes a va_list parameter like below 

```c
int pass_va_list(int count, va_list args);
```

Can be bound to with code like the following:

```scala
import fr.hammons.slinc.*

trait Test derives FSet:
  def pass_va_list(count: CInt, args: VarArgs): CInt
```

In order to allocate and send VarArgs to this function, one uses the `VarArgsBuilder` type:

```scala
import fr.hammons.slinc.runtime.{*,given}
val test = FSet.instance[Test]

Scope.confined{
  val varArgs = VarArgsBuilder(5,2f).build

  test.pass_va_list(2, varArgs)
}
```

More documentation on usage of `VarArgs` can be found [here](./va_list.md).