## Introduction to Library Definitions

All bindings by Slinc take place in library objects, groups of like methods that reflect C functions. When defining a library object, name is not particularly important. One merely defines an object that derives `Library`. Method bindings are done via a method binding with a name matching the C function in question, and parameters with types that match the C function in question.

```scala
object StdLib derives Library:
  def abs(i: Int): Int = Library.binding
```

The above defines a binding to the C standard library's `abs` method. Since the C definition of `abs` is defined like `int abs(int i)`, the scala version of the method is defined with a single input that has the type `Int` and the return type `Int`. The method is defined with `Library.binding` which is a macro that connects the inputs and outputs to the C world.

## Types

In Slinc, you are allowed to use most of the primitive types to bind to C functions. However, these types don't all match C types. In fact, while the primitive types work the same on all platforms within Java, they do not work the same on all platforms in C. 

For example, take the `labs` method from the standard library: `long labs(long l)`

Looking at this function, one might be tempted to write the following binding:

```scala
object StdLib derives Library:
  def labs(l: Long): Long = Library.binding
```

However, while `Long` is always 64-bit on the JDK, it's 32-bit in C on Windows x64, meaning that this binding will fail to work on Windows. It is for this reason that Slinc defines the C types:

|Slinc|C|JVM|
|-----|-|----|
|CChar|char|Byte|
|CShort|short|Short|
|CInt|int|Int|
|CLong|long|?|
|CLongLong|long long|Long|
|CFloat|float|Float|
|CDouble|double|Double|

These base C types are meant to mirror C, and currently most have JVM equivalents.

Now with with table, we have a much more appropriate binding for `labs`

```scala
object StdLib derives Library:
  def labs(l: CLong): CLong = Library.binding
```

Now that we have the binding, let's try to use it: `StdLib.labs(???)`

Looking at the table, the equivalent of `CLong` on the JVM is `?`. That is, it doesn't actually have an equivalent primitive type. On Windows x64, `CLong` is 32-bits wide, making it the equivalent of `Int`, but on Linux and Mac, it's 64-bits wide and is the equivalent of `Long`. In order to use `CLong` we need to convert from a Java primitive. The `as` and `maybeAs` extension methods on the primitives serve this purpose.

* `4.as[CLong]` - The result type is `CLong`, and the conversion is certain to succeed.
* `4.maybeAs[CLong]` - The result type is `Option[CLong]` and the conversion may fail (indicated by `None`).

Since the C standard says that `long` is at least 32-bits long, `as` is available for `Int`, `Short`, and `Byte` types. All other primitive integer types can convert to `CLong` via the `maybeAs` method.

The C types are meant to be analogues to the primitive types defined for C. In the table above, a number have equivalents to JVM types right now, but that may change in future versions of Slinc. If your wish is to write platform independent bindings to C libraries, then you should use the C types and forgo the standard JVM primitives. Usage of the standard JVM primitives will make your bindings brittle and platform specific at some point.

## Structs

In C, structs are a common construct kind of like classes in the JVM. 