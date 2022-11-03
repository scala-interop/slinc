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

In Slinc, you are allowed to use most of the primitive types to bind to C functions. 

For example, in the above `StdLib` library definition, we've our `abs` binding says that abs takes a 32-bit integer value, and returns a 32-bit integer value. Not all of these types correspond directly to their C equivalents however, and that's why Slinc has defined a set of types for use in binding C methods: 

|C|Slinc|JVM|
|---|---|---|
|char|CChar|Byte|
|short|CShort|Short|
|int|CInt|Int|
|long|CLong|?|
|long long|CLongLong|Long|
|float|CFloat| Float|
|double|CDouble|Double|
|void| Unit | Unit|

These Slinc types are designed to each correspond to a C primitive type, and so when one is dealing with `abs`, it's much better to define it as `def abs(i: CInt): CInt`, as you'll be certain that the correct data-type has been used for your binding. 

These types are more than just aliases that correspond to the C naming for primitive types. If you look at the table and try to find the corresponding JVM primitive for C's `long`, you will find a question-mark. This is because `long`'s width is host defined.

### Host defined types 

Because C programs are compiled for and run on only one platform and arch at a time, it's not uncommon for them to have types whose width and definition changes based on the platform and arch. Even C's primitive types have this property, with `long` being 64-bits wide on Linux and MacOS x64 systems, but being 32-bits wide on Windows x64 systems. These types do not have a JVM equivalent because to have one would make your program locked to platforms that match the type definitions you've chosen.

It's for this reason that Slinc has added host defined types. When the Slinc runtime is loaded on the host machine, these types will have the appropriate definitions chosen and used, allowing you to write code that is compiled once, but runnable many places. 

Let's see how we use and develop with these types by binding the `labs` function from the C standard library: 

```scala
object StdLib derives Library:
  def abs(i: CInt): CInt = Library.binding 
  def labs(l: CLong): CLong = Library.binding
```

In order to call this `labs` binding, we must pass in a `CLong`, and in order to get one we can use 2 helper extension methods: 

* `extension [A](a: A) def as[B](using Convertible[A,B]): B`
* `extension [A](a: A) def maybeAs[B](using PotentiallyConvertible[A,B]): Option[B]`

`as` is a safe conversion. If `A`, whatever that may be, can be converted to `B` without any data loss, and that's knowable at compile-time, then `as` is available to convert `A` to `B`.

`maybeAs` is a less safe conversion. If `A` might be able to be converted to `B`, but we can't know for sure until runtime, `maybeAs` is available to convert `A` to `B`. `maybeAs` returns `Option[B]`. If the conversion would need truncation, or just can't work, then `None` is returned, otherwise you get `Some`.

Considering the current platform support for Slinc, `CLong` is knowably at least 32-bits wide at compile-time, so `as` exists for `Int` to `CLong`. Likewise, since we know at compile-time that `CLong` is at most 64-bits wide, `as` exists for `CLong` to `Long`. `maybeAs` exists for `Long` to `CLong` and `CLong` to `Int`.

This means that we can invoke our `labs` binding like so: `StdLib.labs(-5.as[CLong]).as[Long]`.

## Pointers

We've seen above how to bind to C functions that use the C primitive types. C however has a concept that's foreign to the JVM, pointers. Pointers are addresses to locations in memory that should contain 0 or more elements of the type in the pointer. C pointer types are written with a type name followed by the `*` symbol. As an example `int*` is a pointer to 0 or more `int` values. 

So how are these represented in Slinc? The aforementioned `int*` is represented by `Ptr[CInt]`. This follows for the rest of the types almost:

| C | Slinc |
|---|-------|
|char*|Ptr[CChar]|
|short*|Ptr[CShort]|
|int*|Ptr[CInt]|
|long*|Ptr[CLong]|
|float*|Ptr[CFloat]|
|double*|Ptr[CDouble]|
|void*|Ptr[Any]|

As you can see, the corresponding Slinc pointer type is the original corresponding type wrapped in the `Ptr` type, except in the case of `void*`. Since `void*` in C is used to pass around a pointer to any type of data, it only makes sense that it would be `Ptr[Any]` in Slinc and not `Ptr[Unit]`.

So, how can we use pointers in Slinc? Let's try it out by binding `malloc` and `free`:

```scala
object StdLib derives Library: 
  def abs(i: CInt): CInt = Library.binding
  def labs(i: CLong): CLong = Library.binding 
  def malloc(s: SizeT): Ptr[Any] = Library.binding
  def free(p: Ptr[Any]): Unit = Library.binding
```

You'll notice `SizeT` here in this binding, a new type we haven't discussed yet. `SizeT` is a type that can contain the maximum size (in bytes) of an array of any type in C. It's also a host defined type, but we can easily get a SizeT by using the `sizeOf` method defined in Slinc. This means that getting a `Ptr[Any]` that would fit a single `CInt` can be done as below:

```scala
val ptr = StdLib.malloc(sizeOf[CInt])
```

With a pointer, there are a few fundamental operations that you can do with it:

* dereference it: `!ptr` returns an element of the type the Ptr is pointing to
* assign something to it: `!ptr = 4` assigns 4 to a `Ptr[Int]`
* cast it: `ptr.castTo[Int]` casts a `Ptr[Any]` to a `Ptr[Int]`

Be aware though that dereferencing and assign is only supported for `Ptr` of supported types. Dereferencing `Ptr[CInt]` will work, but dereferencing `Ptr[String]` will not. How will you know if you can dereference a certain type? You'll get a compile-time error if you try to dereference a `Ptr` of an unsupported type. 

So, now let's put this into practice with our pointer from `malloc`:
```scala
val ptr = StdLib.malloc(sizeOf[CInt])
val intPtr = ptr.castTo[CInt]
!ptr = 4 //doesn't compile, Any is not supported for dereferencing
!intPtr = "hello" //doesn't compile, "hello" is not an `Int`
!intPtr = 4
println(!intPtr) //prints 4
```

Of course, once you're done with a pointer, you need to free it:

```scala
StdLib.free(intPtr) //doesn't work, intPtr is not Ptr[Any]
StdLib.free(ptr) //works, ptr is Ptr[Any]
StdLib.free(intPtr.castTo[Any]) //works
```

You may be thinking to yourself "this casting of pointers is annoying, why can't we just pass any pointer to `Ptr[Any]`?". The reason this is not allowed is because it would rely on subtyping relationships that exist in Scala, but not in C. Because these relationships do not exist in C, making `Ptr` covariant or contravariant to allow for this niceness would open up a lot of footguns when using Slinc. 

However, there is some relief. We used the literal type conversions when we wrote our `malloc` and `free` bindings. There's a nice helper syntax in Slinc for these bindings though. When your C function has a `void*`, you can use a `Ptr[A]` instead.

```scala
object StdLib derives Library: 
  def abs(i: CInt): CInt = Library.binding
  def labs(i: CLong): CLong = Library.binding 
  def malloc[A](s: SizeT): Ptr[A] = Library.binding
  def free[A](p: Ptr[A]): Unit = Library.binding
```

With these new bindings, we don't need to cast half as much as before:

```scala
val ptr = StdLib.malloc[CInt](sizeOf[CInt])
!ptr = 5
println(!ptr)
StdLib.free(ptr)
```

## Scopes

As shown in the previous section, we can define bindings to `malloc` and `free` and allocate native memory for us to use quite easily. However, remembering to `free` said memory can be error prone. Thus, taking from the example of projects like scala-native, we have Scopes. These are regions of your program where memory can be allocated using `Ptr` operations, and said memory will be automatically freed once the scope exits. 

There are currently two user-facing scopes:

* confined - a scope that frees memory upon exit. Memory that's allocated for this scope is only available to the thread that created it
* global - a scope that never frees memory

Let's see how they work:

```scala
Scope.global{
  //allocates enough memory to store a CInt
  val foreverPtr = Ptr.blank[CInt] 
  val res = Scope.confined{
     //allocates memory for a CInt and copies 4 into it
    val boundedPtr = Ptr.copy[CInt](4)

    !boundedPtr
  }

  println(res) //prints 4
}
```

`Ptr` also has helper methods `blankArray[A](size: Int)`


## Structs

The analog for C structs in Slinc are case classes that derive the `Struct` type class. An example analog for the div_t struct in the C standard library is defined as such:

```scala
case class div_t(quot: Int, rem: Int) derives Struct
```

These struct analogs can be composed with any type that has a Send and/or Receive defined for it.