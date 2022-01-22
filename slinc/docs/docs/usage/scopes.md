---
title: Scopes
layout: doc-page
---

Scopes are the mechanism of memory management in Slinc. They control when memory allocated via Slinc methods, such as `.encode`, is freed, as well as providing the necessary machinery to allocate memory in the first place. 

There are three scopes available in Slinc: 

* `scope`
* `globalScope`
* `lazyScope`

## `scope`

`scope` is the safest scope, and probably the one you'll use most in your code. It takes a block of code that potentially needs to allocate memory as an argument, runs it, then return the final result of the block and frees any memory that was allocated using it. 

```scala
scope{
   val intPtr = 5.encode
   !intPtr = 10
   !intPtr
} //return value: 10
```

Here, `scope` was used to copy 5 into native memory, and when the scope ended, the memory 5 was originally copied into was freed. 

Since Ptr values that are allocated inside a scope are invalid outside of it, there's a weak guard on `scope` preventing it from returning a `Ptr` value.

## `globalScope`

`globalScope` behaves like `scope` above in that it's used by certain methods to allocate native memory, but unlike `scope`, it never frees memory. If you need memory that is guaranteed to exist for the life of your program, use `globalScope`

## `lazyScope`

`lazyScope` is a bit more like `scope` than `globalScope` in that it frees memory it allocated, but `lazyScope` is lazy and only frees when the pointers it created are garbage collected. This can make it very dangerous, as it may not free native memory fast enough and you can run into a situation where you can no longer allocate it. Prefer `scope` to it unless you know what you're doing. 