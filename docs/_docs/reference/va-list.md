---
title: "VarArgs"
---

In C, the `va_list` type can be used to allow callbacks to be variadic. This type is supported in Slinc via the `VarArgs` and `VarArgsBuilder` types.

`VarArgs` is the Scala analog of `va_list`, so bindings to C functions and types that contain `va_list` should use `VarArgs` in Slinc.

## Allocating VarArgs

In order to receive a `VarArgs` value, one must build it from the `VarArgsBuilder` within a `Scope`.

```scala
import fr.hammons.slinc.*
import fr.hammons.slinc.runtime.{given, *}

Scope.confined{
  val varArgs: VarArgs = VarArgsBuilder(1, 2: Byte, 3: Short, 4l, 5f, 6d).build
}
```

The `VarArgs` data will be deallocated at the end of the `Scope` if said Scope deallocates memory it creates, so make certain that the function you pass a `VarArgs` to uses it before the `Scope` ends.

## Getting data from VarArgs

The `.get[A]` method is used on `VarArgs` to extract a value from a `VarArgs`. After each `.get`, an element is removed from the head of the `VarArgs`. `A` should be any native compatible type, such as `Int`, `CInt`, structs, pointers, etc.

## Skipping data from VarArgs

The `.skip[A]` method skips an element in the `VarArgs`. `A` should be any native compatible type, such as `Int`, `CInt`, structs, pointers, etc.

## Copying the VarArgs

The `.copy` method allows you to copy a `VarArgs`, making sure that functions you pass the `VarArgs` to don't change what each other sees in from the data-type.

## Scopes

Be very careful with `VarArgs`. They are usually on the stack, especially if they've come from C. That means that their scope can be much more limited than most things in Slinc. The data backing a `VarArgs` can cease to exist after a function finishes, rather than when a Scope ends.

## Java 17 support

`VarArgs` on java 17 is not well supported. If you need to use `VarArgs` for a project, please choose [one of the other JVMs Slinc supports](./jdk-support.md).
