---
title: Usage
---

## Introduction to Library Definitions

All bindings by Slinc take place in library objects, groups of like methods that reflect C functions. When defining a library object, name is not particularly important. One merely defines an object that derives `Library`. Method bindings are done via a method binding with a name matching the C function in question, and parameters with types that match the C function in question.

```scala
object StdLib derives Library:
  def abs(i: Int): Int = Library.binding
```

The above defines a binding to the C standard library's `abs` method. Since the C definition of `abs` is defined like `int abs(int i)`, the scala version of the method is defined with a single input that has the type `Int` and the return type `Int`. The method is defined with `Library.binding` which is a macro that connects the inputs and outputs to the C world.

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


Since these types are only guaranteed to be defined at runtime, interacting with them can be difficult. There are three ways at present to interact with these types:

* Assured Conversion
* Potential Conversion
* Platform focus 

### Assured Conversion 

Assured conversion is done via the `.as` extension method on a compatible type, or to a compatible type. This method is generally available when there's some baseline guarantee about the kind and width of the type in question. For example, `CLong` is at minimum a 32-bit wide integer type, so `Int.as[CLong]` exists. It is also (for now) at maximum a 64-bit wide integer type, so `CLong.as[Long]` exists.

### Potential Conversion

Potential conversion is generally more available than the assured conversions. Potential conversion is done via the `maybeAs` extension method that will return an `Option` result indicating whether the conversion is valid on the current host.

As an example, `Long.maybeAs[CLong]` will return `Some(l: CLong)` on X64 Linux, but `None` on X64 Windows. 

### Platform Focus 

The `platformFocus` method takes a platform instance and a section of code where the host dependent types have automatic conversions to and from their definitions on the host platform. As an example:

```scala
def labs(l: CLong): CLong = Library.binding 

platformFocus(x64.Linux){
  //this line works here
  labs(-13l) == -13l //true
}

//this line doesn't work here, cause outside of the above zone
// CLong isn't equivalent to Long
labs(-13l) == -13l
```

The `platformFocus` method returns `Option[A]` where `A` is the return type of the platform focus zone. The method returns `None` if the platform selected for the zone doesn't match the host. This allows you to chain together platform specific code via `.orElse`.

The C types are meant to be analogues to the primitive types defined for C. In the table above, a number have equivalents to JVM types right now, but that may change in future versions of Slinc. If your wish is to write platform independent bindings to C libraries, then you should use the C types and forgo the standard JVM primitives. Usage of the standard JVM primitives will make your bindings brittle and platform specific at some point.

## Pointers

Pointers are represented in Slinc with the `Ptr` type. For example, `Ptr[Int]` is a pointer to native memory that should be readable as a JVM Int.

The pointer class' operations are powered by three type classes:

* `LayoutOf` - this type class provides layout information about the type in question, if it exists.
* `Send` - this type class shows how to copy data of type `A` from the JVM into native memory.
* `Receive` - this type class shows how to copy data of type `A` from native memory into the JVM heap.

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

These struct analogs can be composed with any type that has a Send and/or Receive defined for it.