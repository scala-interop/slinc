---
title: "Function Sets"
---

Function sets are groupings of functions in Slinc. Each can be configured in a number of ways.

A function set is a Scala `trait` that derives the `FSet` type class. Method declarations within it are used as a template for the bindings to C that the Slinc runtime will generate. 

## FSet declarations

FSet declarations are declarations of C library bindings without any entanglement in runtime details. This means that an fset declaration only requires the use of the Slinc core library, and does not effect the runtime requirements of users of the module.

### FSet naming

The type name of the module does not matter to Slinc. 

### Method naming

The name of method declarations within an fset module should reflect the name of the C function you wish to bind to. If a method name is not found in the C namespace, this will cause a runtime error when you try to summon the module implementation.

Example:

```scala
trait L derives FSet:
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

## Dependencies

FSets can be declared that depend on C libraries in a number of different ways:

* libraries stored in resources
* libraries at a relative location compared to the program
* libraries on the system path
* libraries at an absolute location
* C files located in resources

### Libraries in resources

A library can be stored in the jar for the program, and an FSet can depend on it with the `@NeedsResource` annotation.

```scala
import fr.hammons.slinc.annotations.*
import fr.hammons.slinc.types.*
import fr.hammons.slinc.*

@NeedsResource("my_lib.so")
trait L derives FSet:
  def my_fun(i: CInt): Unit
```

This indicates that the FSet named `L` needs to load `/native/my_lib.so` from the jar resources. This dependency declaration is rather platform specific, since it depends on a specific .so file. 

#### Platform dependent resource dependencies

If you just provide the name of the library, not a specific .so file, Slinc will look in the jar resources for a library with that kind of name but with a library suffix and architecture tag based on the platform the program is run on. For example:

```scala
import fr.hammons.slinc.annotations.*
import fr.hammons.slinc.types.*
import fr.hammons.slinc.*

@NeedsResource("my_lib")
trait L derives FSet:
  def my_fun(i: CInt): Unit 
```

The slinc runtime will look for `/native/my_lib_x64.so` on x86_64 linux, and `/native/my_lib_x64.dll` on x86_64 Windows.

### Libraries on the system path

If the library is on the system path that the JVM is aware of, you can declare an FSet's dependence on it with the `Needs` annotation. 

```scala
import fr.hammons.slinc.annotations.*
import fr.hammons.slinc.types.*
import fr.hammons.slinc.* 

@Needs("z")
trait L derives FSet:
  def zlibVersion(): Ptr[CChar] 
```

This declaration is for a binding to zlib.

### Libraries in the filesystem

If the library dependency is located on the filesystem, you can use an absolute or relative path with the `@NeedsFile` annotation.

```scala 
import fr.hammons.slinc.annotations.*
import fr.hammons.slinc.types.*
import fr.hammons.slinc.*

//relative path
@NeedsFile("my_lib.so")
trait A derives FSet:
  def my_fn(): Unit 

//absolute path 
@NeedsFile("/tmp/my_lib.so")
trait B derives FSet:
  def my_fn(): Unit
```

#### Platform dependent filesystem dependencies 

If you do not provide an absolute file name (file ending with .so or .dll), Slinc will use the base file name provided along with the appropriate library suffix for the OS and a tag based on the architecture.

For example, on Windows x86_64, `@NeedsFile("my_lib")` will look for `.\my_lib_x64.dll`.

The architecture tags for the architectures follows:

|arch |tags |
|-----|-----|
|x86_64| x64, amd64, x86_64|

### C file jar resources 

If a C file is placed in a jar, under the `/native` you can use the `@NeedsResource` annotation to have Slinc compile and load the file at runtime. In order for this to work, one must have clang installed on the target system.

```scala
import fr.hammons.slinc.annotations.*
import fr.hammons.slinc.types.* 
import fr.hammons.slinc.* 

@NeedsResource("my_lib.c")
trait L derives FSet:
  def my_fn(): Unit
```

## Summoning FSet Implementations

FSet declarations have been shown above, but they are not useable without being summoned. Doing so requires the Slinc runtime on your classpath, and makes the JAR and class files generated dependent on a specific JVM. 

To summon an fset implementation, you use the `FSet.instance[?]` method as shown in the following example:

```scala
import fr.hammons.slinc.types.CInt
import fr.hammons.slinc.FSet
import fr.hammons.slinc.runtime.given

trait L derives FSet:
  def abs(i: CInt): CInt

val l = FSet.instance[L]

@main def program = println(l.abs(4))
```

Note the assignment of the instance to a `val`. This is not strictly necessary, and `FSet.instance` will always return the same module instance, but re-summoning is more expensive than storing the summoned module implementation.
