---
title: "Slinc Types"
layout: doc-page
---

Slinc is a highly type-based library, so one of the most fundamental parts of understanding how it works is how Scala's types map to C types in Slinc.

## The Primitive Types

Most primitive types in Scala are supported by Slinc for C interop out of the box:

| Scala Type | C Type |
| ---------- | ------ |
| Byte       | char   |
| Short      | short  |
| Int        | int    |
| Long       | long   |
| Float      | float  |
| Double     | double |
| Unit       | void   |


You'll notice the absence of `Char` from this list. That's because a `Char` in Scala is actually a 16-bit value, and some supported characters cannot map to C's 8-bit `char`. 

In order to overcome this, I have provided two extension methods on `Char`: 
* asAscii: Option[AsciiChar]
* asAsciiOrFail: AsciiChar

The `AsciiChar` type is an opaque subtype of `Char` that is guaranteed to be less than 8-bits, either by the `Option` type, or by throwing an exception at the conversion site.

## Structs

Structs in Slinc are case classes that have derived the `Struct` typeclass. 

`Foo` here is recognizable as a Struct, and can be used as a parameter in a C binding.
```scala 
case class Foo(i: Int) derives Struct
```

`Bar` on the other hand is not a Struct, as it doesn't derive `Struct`.
```scala
case class Bar(f: Float)
```

You can derive a `Struct` instance for a case class after the fact though.

```scala
given Struct[Bar] = Struct.derived
```

Structs in Slinc can hold all other C compatible types inside of them, including other structs.

```scala
case class Baz(b: Byte, s: Short, f: Float, d: Double, l: Long, foo: Foo) derives Struct
```


## Static Arrays

Static arrays in Slinc are much less useful than they are in C. Since they can only be passed to a C binding via a struct, they are mainly useful for struct definitions. Say for instance you have the following C struct:

```c
struct my_struct {
   int[3] x;
   float y;
};
```

You can create this type via usage of the `StaticArray` type of Slinc.

`StaticArray` takes two type parameters
* The data type `T`
* A static size type `Size`

In the above case, the appropriate Slinc definition would be:
```scala
case class my_struct(x: StaticArray[Int, 3], y: Float) derives Struct
```

The `Size` type is very important in `StaticArray`, as it's used in conjunction with the `T` type to determine how much memory is taken by the array at compile time.

## Pointers

Pointers are supported in Slinc through the `Ptr[?]` class. This class acts as a typed wrapper around a memory address, allowing you to copy data out of native memory or write data into it.

### Dereferencing and Encoding
Pointer's main operator is `deref` or `!`. If you have a `Ptr[Int]` named `a`, `!a` will copy the `Int` at the internal memory address and return it as a Scala value. Please note the word "copy". Dereferencing a `Ptr` in Slinc doesn't return you a type that changes as native memory changes, but rather an immutable Scala value. 

The reverse is true too. To get a pointer from a C compatible type, you can call the `.encode` extension method. As an example: 

```scala
val iPtr: Ptr[Int] = 4.encode
```

The `.encode` method automatically exists for all C compatible types, and it will copy data from Scala into native memory. Again note that this will be a copy. `encode` does not generate references to Scala data.

One last caveat for the `.encode` method is that it requires a `scope`, since it allocates data in native memory. Please check the [scope documentation](scopes.md) for further information.

You can also write C compatible values to existing pointers. 

```scala
val x: Ptr[Int] = 4.encode
!x = 5
```

After the following code, the content of `x` is now 5.

### Ptr and Slinc's C compatible types

In general, pointer types in Slinc correspond to their C compatible types. An integer is an `Int`. A pointer to an integer is a `Ptr[Int]`. A double is a `Double`. A pointer to a double is a `Ptr[Double]`. A unique exception is with regards to C's `void*` type. While the `void` type maps to `Unit` in Scala, `void*` is used as a pointer to anything in C, so it's corresponding Slinc type is `Ptr[Any]`.


## Arrays, Strings, and Functions

Arrays, Strings, and Functions are special cases in Slinc, because they cannot be passed by value to C. That is, C doesn't support these types as anything but pointers, so in order to pass them into a C function, you must use the `.encode` extension method. 

| Scala type     | C compatible type   | C type                   |
| -------------- | ------------------- | ------------------------ |
| `Array[Int]`   | `Ptr[Int]`          | `int*`                   |
| `String`       | `Ptr[Byte]`         | `char*` or `const char*` |
| `Int => Float` | `Ptr[Int => Float]` | `float (*fn)(int)`       |

`Ptr[Int]` is the C compatible type for an `Array[Int]`, meaning that when you get an array of integers back from C, it will be it `Ptr[Int]`. A convenience method has been added to `Ptr` called `toArray(size: Int)` that will copy `size` pointer elements into a Scala Array, allowing you to easily pass this data type between C and Scala. 

`Ptr[Byte]` is the C compatible type for `String`, so a `.mkString` method has been added to this specific type of `Ptr` in order to facilitate passing strings to and from C. 

Finally, we have functions. A function can be encoded to a `Ptr`, and fetched from one quite easily.

```scala
val fn = (a: Int) => a + 1
val fnPtr: Ptr[Int => Int] = fn.encode
val retrieved = !fnPtr

retrieved(1) //2
fn(1) //2
```

However, due to an oddity in how panama's foreign works, while we can quite easily allocate a function pointer in native memory, we cannot write said function to memory directly like other C compatible types. In other words the following code will not compile.

```scala
val fn = (a: Int) => a + 1
val fnPtr: Ptr[Int => Int] = fn.encode
!fnPtr = (a: Int) => a*a
```

## Platform dependent types

C has a number of types whose definitions change depending on the platform you're on at present. One such type is `time_t`, whose type is equivalent to `Int` on MidnightBSD for the i386 architecture, but is defined as `Long` on Linux i386. I have provided a number of path dependent types, such as `time_t`, in a platform independent manner. Since their type definition shifts, you cannot know their real type at compile time. However, they remain C compatible, and Integral operations and comparisons (as well as conversions) have been provided for all of these types. Furthermore, Scala camel case style type aliases have been provided.

An example: 
```scala
val x = SizeT.fromIntOrFail(5) //throws if Int can't fit into SizeT on the current platform
val y = SizeT.fromInt(10) //None if Int can't fit into SizeT on the current platform, Some otherwise
y.map(_ + x).toInt
```