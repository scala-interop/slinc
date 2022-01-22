---
title: Binding C Methods
layout: doc-page
---

Slinc's main purpose as a library is to allow Scala programmers to easily create bindings to C methods and call them with as little fuss as possible. Slinc uses three concepts to create bindings to C methods at present: 
* `bind`
* `variadicBind`
* `Library`

## `bind`

`bind` is a macro defined in Slinc, and for 99% of your method bindings, it will be all you need. `bind` works off the type signature of a method, and is fairly easy to use as long as you're aware of the mapping between C types and Scala types in Slinc. Please refer to the [Slinc Types](slinc-types.md) page if you need a refresher, or if you're new to Slinc. 

In C's stdlib.h, there's a method called `qsort`. 

```C
void qsort(void *base, size_t nitems, size_t size, int (*compar)(const void *, const void *))
```
To bind to it in Slinc, one must create a method with the same name, and the following implementation:

```scala
def qsort(base: Ptr[Any], nitems: SizeT, size: SizeT, compar: Ptr[(Ptr[Any], Ptr[Any]) => Int]) = bind[Unit]
```

That is it. `qsort` is now bound, and can be called with the appropriate data. 

One small note: when binding a function it's necessary to use the function's name in C including the capitalization, but the parameter names do not matter in the least. All that matters for `bind` with regards to the parameters is that you've written the appropriate types.

## `variadicBind`

`variadicBind` is a special version of bind that allows you to bind to variadic functions. Because of the limitations of Scala 3 macros at present, its syntax is not as nice as `bind`'s, but it's still not hard to use.

In C's stdio.h, there's a method called `printf`.

```C
int printf(const char *format, ...) 
```

The corresponding binding in Slinc would take the form 

```scala
def printf(format: Ptr[Byte]) = variadicBind[Int](format)
```

One other difference between `variadicBind` and `bind` is that its bindings are curried methods, with the second set of parameters being the variadic arguments. 

Using our `printf` binding therefore looks like:

```scala
scope{
   printf("%d %d %f".encode)(5,3,10.0f)
}
```

## `Library`

`Library` is a trait in Slinc that's used to influence method lookup. So far, all our bindings have been to the C standard library, so we didn't need `Library`. However, if we wanted to bind to a system library, or a library that is positioned relative to our program, or a library that we have the absolute path to, we would need to define our bindings within an object that extends Library appropriately.

```scala
object MySpecialBinding extends Library(Position.Local("libs/myspeciallib.so")):
   //this binding will be made to myspeciallib.so
   def special_binding(i: Int) = bind[Float]
```

The `Library` trait takes one of three values from the `Position` enum:
* Local
* System
* Absolute

`System` indicates lookup should be done on libraries that are on the system path, `Local` indicates lookup should be done relative to the program's current working directory, and `Absolute` indicates lookup using an absolute path.
