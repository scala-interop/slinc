[![pipeline status](https://gitlab.com/mhammons/slinc/badges/master/pipeline.svg)](https://gitlab.com/mhammons/slinc/-/commits/master)
```
s     lin  c
Scala Link C
```

Slinc is a Scala 3 library that allows users to interoperate with C code via Java 17's [foreign api incubator](https://docs.oracle.com/en/java/javase/17/docs/api/jdk.incubator.foreign/jdk/incubator/foreign/package-summary.html).

It's designed to make use of Scala's type system and macros to handle most of the work of making bindings to C from Scala. It provides an effect type name `NativeIO` that attempts to make native operations and book keeping related to method invocation lazy as possible.

A NativeIO is not run and does not produce results until it is compiled. At present, there is only one compiler (`NativeIO.impureCompiler`) provided.

## Binding to C functions

The binding to the `abs` function looks like
```scala
//result type: Int => NativeIO[Int]
val abs = NativeIO.function[Int => Int]("abs") 
```

The binding to the strlen function looks like

```scala
//result type: String => (SegmentAllocator) ?=> NativeIO[Int]
val strlen = NativeIO.function[String => Int]("strlen") 
```
As you can see, some function bindings have a dependency on a `SegmentAllocator` being in scope. This is because said functions allocate native memory (in the above case, `String` must be made into a C String via native allocation). At the moment, the cases where this happens is if there's a `String` in the function parameters, or if the function returns a `Struct`

## Scopes

C interop naturally entails allocation of native memory. This memory is managed via `NativeIO.scope(...)`. The scope provides a `SegmentAllocator`, and frees memory associated with said allocator at the end of the `Scope` block.

## Defining Structs

Creating bindings to structs are fairly simple in slinc.

```scala
type div_t = Struct {
    val quot: int
    val rem: int
}
```

is a binding of 

```C
struct {
    int quot;
    int rem;
} div_t;
```

Notice that the types of `quot` and `rem` are lowercase: `int`. This is a field-type, with get and set methods available for retrieving and setting data.

```scala
val div = NativeIO.function[(Int,Int) => div_t]("div")
NativeIO.scope(
    for
        result <- div(5,2)
        quot = result.quot.get
        rem = result.rem.get
    yield (quot, rem)
) //NativeIO((2, 1)) when compiled
```

## C types to Scala Types

The following table shows how C types map to Scala types in function bindings

|C   | Scala|
|----|------|
|int | Int  |
|float| Float|
|double | Double|
|long | Long |
| const char * | String |
| void | Unit |
| struct | see struct section |


## Unsupported at present

* Union types
* Static arrays
* non-constant strings
* Structs within structs
* Callbacks