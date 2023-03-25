---
title: "Library Modules"
---

Library modules are groupings of functions in Slinc. Each can be configured in a number of ways.

A library module is a Scala `trait` that derives the `Lib` type class. Method declarations within it are used as a template for the bindings to C that the Slinc runtime will generate. 

## Module Declarations

Module declarations are declarations of C library bindings without any entanglement in runtime details. This means that a module declaration only requires the use of the Slinc core library, and does not effect the runtime requirements of users of the module.

### Module naming

The type name of the module does not matter to Slinc. 

### Method naming

The name of method declarations within a library module should reflect the name of the C function you wish to bind to. If a method name is not found in the C namespace, this will cause a runtime error when you try to summon the module implementation.

Example:

```scala
trait L derives Lib:
  def abs(i: CInt): CInt
```

This library module `L` has a binding to `abs` in the C standard library namespace.

#### Naming overrides

You can override the C name lookup for a method declaration on a host dependent basis with the `@NameOverride` annotation. You provide the `@NameOverride` annotation with the alternative name, and an OS and architecture tuple where the name should be used. An example follows:

```scala
trait L derives Lib:
  @NameOverride("_time64", OS.Windows -> Arch.X64)
  def time(timer: Ptr[TimeT]): TimeT
```

This function is helpful when you have a function symbol that should be present on a platform, but has an alternative name for some reason. In the case with Windows, the `time` function in the C standard library is a macro that points to `_time64` on 64-bit platforms, and `_time32` on 32-bit platforms. Since macros do not exist as symbols in the C standard library namespace, this `NameOverride` makes the Slinc platform choose the right function name on Windows X64.

## Summoning Module Implementations

Module declarations have been shown above, but they are not useable without being summoned. Doing so requires the Slinc runtime on your classpath, and makes the JAR and class files generated dependent on a specific JVM. 

To summon a module implementation, you use the `Lib.instance[?]` method as shown in the following example:

```scala
import fr.hammons.slinc.types.CInt
import fr.hammons.slinc.Lib
import fr.hammons.slinc.runtime.given

trait L derives Lib:
  def abs(i: CInt): CInt

val l = Lib.instance[L]

@main def program = println(l.abs(4))
```

Note the assignment of the instance to a `val`. This is not strictly necessary, and `Lib.instance` will always return the same module instance, but re-summoning is more expensive than storing the summoned module implementation.
