---
title: Binding C Methods
layout: doc-page
---

Slinc's main purpose as a library is to allow Scala programmers to easily create bindings to C methods and call them with as little fuss as possible. Slinc uses three concepts to create bindings to C methods at present:
* `CLibrary` and `nativeAccess`
* Method naming
* `LocalLocation`, `AbsoluteLocation`, and `SystemLibrary`
* `WithPrefix`
* `accessNativeVariadic`
  
## `CLibrary` and `nativeAccess`

`CLibrary` and `nativeAccess` are the ways binding and using C functions works in Slinc. In order to bind to a library, one must define a class or object that derives the `CLibrary` trait. All methods in a class or object that derives said trait will have C bindings matching their shape derived for them. `nativeAccess` is a macro that defines the methods in question as using the automatically generated bindings. As a practical example, say you wanted to bind to `qsort` from the C standard library. The following code would work for you.

```scala
object MyBinding derives CLibrary:
   def qsort(base: Ptr[Any], nitems: SizeT, size: SizeT, compar: Ptr[(Ptr[Any], Ptr[Any]) => Int]) = nativeAccess[Unit]
```

The name of the object does not matter, just the name and signature of the methods within it. The method name should reflect the C name, but underscores should be replaced by standard scala camel-casing. Please read the following section for more details on method naming. The `CLibrary` typeclass, when derived for `MyBinding`, will generate the method handles and other machinery needed for the foreign API to work, while the `nativeAccess` macro provides a method definition that uses said machinery. As for the method signature needed for the bindings, please refer to [the Slinc types](slinc-types.md) page for more information. In the above case, the C definition of `qsort` is as follows:

```C
void qsort(void *base, size_t nitems, size_t size, int (*compar)(const void *, const void *))
```

Following the C signature, an input of a `void` pointer, two `size_t`s and a function pointer input that needs a function that takes two `void` pointers and returns an `int`, it follows that our Scala `qsort` binding should take `Ptr[Any]`, two `SizeT`s, and a `Ptr[(Ptr[Any], Ptr[Any]) => Int]` as input, since those are the corresponding Slinc types.

## Method Naming and the `RawNaming` trait

C methods tend to have snake casing, which is jarring looking at within a Scala code base. It's for this reason that Slinc automatically converts your camel case method names to snake casing when creating bindings. Therefore if you would want to bind to the C method `i_am_a_method`, you would create a method in an object or class with the name `iAmAMethod`. If you want to override this behavior in an object or class, extend the `RawNaming` trait. Extending this trait will make the binding generator bind `i_am_a_method` in Scala to `i_am_a_method` in C.

## `WithPrefix`

It is not uncommon for C libraries to prefix their method names with the library name. In order to reduce the noise related to this, the `WithPrefix` mixin trait has been provided. An object extending `WithPrefix["cblas"]` will have all the methods bind to c methods like `cblas_${methodName}`. For an example, look to the next section.

## `LocalLocation`, `AbsoluteLocation`, and `SystemLibrary`

These three types are meant to be extended by your binding object or class in order to indicate the specific library location or name for non-standard C libraries. When one of these types is seen while deriving the `CLibrary` typeclass, a special loader is used in order to fetch from the requisite library. As an example, take cblas:

```scala
object OpenBlas extends SystemLibrary("cblas"), WithPrefix["cblas"] derives CLibrary:
   type CblasIndex = Long
   //binds to `cblas_ddot` thanks to the WithPrefix trait
   def ddot(
       n: CblasIndex,
       dx: Ptr[Double],
       incx: Int,
       dy: Ptr[Double],
       incy: Int
   ): Double = accessNative[Double]
```

Here `SystemLibrary` is used since `libcblas.so` is on the library path. If you want to instead point to the absolute location, you can use `AbsoluteLocation` and pass in the location of the `.so` file as a `String`. Finally, if you want to bind to a library whose location is relative to the current working directory of your program, you can pass in a relative path to `LocalLocation`

## `accessNativeVariadic`

`accessNativeVariadic` is a special version of `accessNative` that allows you to use variadic function. Because of the limitations of Scala 3 macros at present, its syntax is not as nice as `accessNative`'s, but it's still not hard to use.

In C's stdio.h, there's a method called `printf`.

```C
int printf(const char *format, ...) 
```

The corresponding binding in Slinc would take the form 

```scala
object MyLib derives CLibrary: 
   def printf(format: Ptr[Byte]) = accessNativeVariadic[Int](format)
```

One other difference between `accessNativeVariadic` and `accessNative` is that its bindings are curried methods, with the second set of parameters being the variadic arguments. 

Using our `printf` binding therefore looks like:

```scala
scope{
   MyLib.printf("%d %d %f".encode)(5,3,10.0f)
}
```