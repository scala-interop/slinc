[![pipeline status](https://gitlab.com/mhammons/slinc/badges/master/pipeline.svg)](https://gitlab.com/mhammons/slinc/-/commits/master)![Maven Central](https://img.shields.io/maven-central/v/io.gitlab.markehammons/slinc)
```
S     Lin     C
Scala Link to C
```

SLinC is a Scala 3 library that allows users to interoperate with C code via Java 17's [foreign api incubator](https://docs.oracle.com/en/java/javase/17/docs/api/jdk.incubator.foreign/jdk/incubator/foreign/package-summary.html).

It's designed to make use of Scala's type system and macros to handle most of the work of making bindings to C from Scala.

This library has a set of helper syntax and extension methods, accesible via the following import `import io.gitlab.mhammons.slinc.*`

## Binding to C functions

Binding to a C function involves using the `bind` macro. The macro looks at the attached method name, method parameter types, and method return type in order to generate the binding glue.

The binding to the `abs` function looks like
```scala
def abs(i: Int): Int = bind
```

The binding to the strlen function looks like

```scala
def strlen(string: String): Int = bind
```

### Basic types mapping (pass by value)

|C      |Scala  |
|-------|-------|
|int    |Int    |
|float  |Float  |
|double |Double |
|long   |Long   |
|void   |Unit   |
|char   |Char   |
|char   |Boolean|
|char   |Byte   |
|short  |Short  |
|struct |Product|

## Structs

In SLinC, structs are represented by `Product` types who have a `Struct` typeclass instanced for them. The easy way to do this is while defining a case class.

```scala
case class div_t(quot: Int, rem: Int) derives Struct
```

One can also derive the typeclass for other product types like so:

```scala
given Struct[(Int, Int)] = Struct.derived
```

Once this typeclass is defined for a type, it can be used in method bindings.

```scala
def div(num: Int, denom: Int): div_t = bind
```

### Static Arrays

Static arrays are typically defined in C like thus: 

```c
int primes[4] = {2,3,5,7};
```

These arrays are really only usable within `Struct`s in SLinC. Since they can only be passed by reference in C, they aren't supported as input parameters or returns for bindings. 

Static arrays are declared via the following code:

```scala
StaticArray[Int, 5]
```

This indicated an array of size 5 of integers.

## Pass by Value vs Pass by Reference

In general, C methods have two ways to pass data: by reference and by value. The type signatures shown above are all pass by value. That is, the data they represent is copied into the native world, and modifications to said data by the C world are not reflected back in Scala. Likewise, a pass by value return will be copied from the native heap into the java heap, and any changes made on the native heap will not be reflected in the JVM.

In order to properly share data between a scala program and C, one must use pass by reference. These inputs are represented by standard native compatible types wrapped in the `Ptr` higher kinded type (ie: `Ptr[Int]` for a pass by reference `Int`)

### Scopes

Creation of Ptr types involves allocating space in the native heap, and possibly copying data into said space. Both allocation and freeing of native heap space is managed by the `scope` function. This function takes a block of code that needs to allocate native heap space, provides the methods to do so, and frees allocated space once the block ends.

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
```

## Unsupported at present

* Union types
* Static arrays
* non-constant strings
* Structs within structs
* Callbacks