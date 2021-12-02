[![pipeline status](https://gitlab.com/mhammons/slinc/badges/master/pipeline.svg)](https://gitlab.com/mhammons/slinc/-/commits/master)![Maven Central](https://img.shields.io/maven-central/v/io.gitlab.markehammons/slinc)
```
S     Lin     C
Scala Link to C
```

Slinc is a Scala 3 library that allows users to interoperate with C code via Java 17's [foreign api incubator](https://docs.oracle.com/en/java/javase/17/docs/api/jdk.incubator.foreign/jdk/incubator/foreign/package-summary.html).

It's designed to make use of Scala's type system and macros to handle most of the work of making bindings to C from Scala.

## Binding to C functions

The binding to the `abs` function looks like
```scala
def abs(i: Int): Int = bind
```

The binding to the strlen function looks like

```scala
def strlen(string: String)(using SegmentAllocator): Int = bind
```
As you can see, some function bindings have a dependency on a `SegmentAllocator` being in scope. This is because said functions allocate native memory (in the above case, `String` must be made into a C String via native allocation). At the moment, the cases where this happens is if there's a `String` in the function parameters, or if the function returns a `Struct`

## Scopes

C interop naturally entails allocation of native memory. This memory is managed via `scope(...)`. The scope provides a `SegmentAllocator`, and frees memory associated with said allocator at the end of the `scope` block.

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
def div(num: Int, denom: Int)(using SegmentAllocator): div_t = bind
scope{
    val result = div(5,2)
    (result.quot.get, result.rem.get)
}
```

## int vs Int

In SLinC, int and Int are both types that represent integers. The big difference between the two is where those integers are stored. Ints live on the java heap/stack, but ints live in the native world and data has to be copied either from them `int()` or to them `int() = 5`. Most SLinC types have native counterparts, which are always named in the lowercase fashion.

## C types to Scala Types

The following table shows how C types map to Scala types in function bindings

|C   | Scala|
|----|------|
|int | Int or int  |
|float| Float or float|
|double | Double or double |
|long | Long or long |
| const char * | String or string |
| void | Unit |
| struct | see struct section |


## Unsupported at present

* Union types
* Static arrays
* non-constant strings
* Structs within structs
* Callbacks