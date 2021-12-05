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

Creating struct descriptions are fairly simple in SLinC

```scala
case class div_t(quot: Int, rem: Int) derives Struct
```

is the equivalent of 

```C
struct {
    int quot;
    int rem;
} div_t;
```

```scala
def div(num: Int, denom: Int)(using SegmentAllocator): div_t = bind
val result = scope{
    div(5,2)
}

assertEquals(result.quot, 2)
assertEquals(result.rem, 1)
```

Please note that case classes sent to/received from a function are pass by value. These values are immutable and copied into the JVM heap from the native world.

## Pointers

Pointers can be generated from types via the `.serialize` method. This method is added to all compatible types, including types that derive `Struct`, via importing all from SLinC via `import io.gitlab.mhammons.slinc.*`. 

Pointers are dereferencable by the unary ! operator: 
```scala
case class div_t(quot: Int, rem: Int) derives Struct

val ptr: Ptr[div_t] = div_t(2,1).serialize
assertEquals(!ptr, div_t(2, 1))
!ptr = div_t(3,7)
assertEquals(!ptr, div_t(3, 7))
```

Please note that this dereferencing operation involves copying data to and from the jvm into and out of the native world, and can consequently be costly. If you only wish to access or modify a small piece of a struct that is stored in the native heap, you can use the `.partial` method on pointers:

```scala
!ptr.quot //copies the entire div_t from ptr into the jvm, then accesses quot
!ptr.partial.quot //copies only quot into the jvm
!ptr = div_t(5,6) //updating ptr normally requires copying in an entire new div_n
!ptr.partial.quot = 5 //only copies 5 from the jvm, and only writes it to the memory for quot

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