---
title: Other Helpful Functions
layout: doc-page
---

## `allocate`

The `allocate` method can be used within a scope to allocate a block of n chunks of data based on the data given to it. For example to allocate 2 integers in native memory, one would write `allocate[Int](2)`

## `sizeOf`

`sizeOf` behaves like its C counterpart, except it's not a macro. Give it a type, and it will give you the size in bytes in `SizeT` format.


## `partial`

`partial` is an extension method macro on `Ptr`s that can enrich them with data regarding a structure they contain, and allow you to partially dereference the pointer. As an example:

```scala 
case class div_t(quot: Int, rem: Int) derives Struct
val x: Ptr[div_t] = div_t(3,4).encode
val y: Int = !x.partial.quot
```

## `castTo`

`castTo` is a method on `Ptr` that changes what it's perceived as pointing to. This is especially helpful if you've received a `Ptr[Any]` and need it change it to another type to actually use it.
