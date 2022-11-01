---
layout: index
---

Slinc is a Scala 3 library that allows users to interoperate with C code via Java 17+'s foreign api. 

It's designed to make use of Scala's type system, macros, and runtime multi-stage programming to make bindings to C from Scala.

## Quickstart 

Slinc is published to Maven Central for Scala 3. It is built to take advantage of the cutting edge of Scala functionality and features, so your project will most certainly need to track the latest Scala version if you want to use the latest version of Slinc. Currently, the Scala version in use is `3.2.1`. 

### SBT setup 

In your `build.sbt`:

```scala
libraryDependencies += "fr.hammons" %% "slinc-runtime" % "0.1.1-66-a2fa26-DIRTYd1a2c450"
//if forking and on Java 17
javaOptions ++= Seq("--add-modules=jdk.incubator.foreign", "--enable-native-access=ALL-UNNAMED")
```

in .jvmopts in the root of your build:
```
--add-modules=jdk.incubator.foreign
--enable-native-access=ALL-UNNAMED
```

For additional setup instructions, please refer to the configuration page.

Once you have your build system set up, you can create a new file and write the following code: 

```scala
import fr.hammons.slinc.runtime.{*,given}

case class div_t(quot: Int, rem: Int) derives Struct 

object MyLib derives Library:
  def div(numer: Int, denom: Int): div_t = Library.binding

@main def calc = 
  val (quot, rem) = Tuple.fromProduct(MyLib.div(5,2))
  println(s"Got a quotient of $quot and a remainder of $rem")
```

This library relies on the user importing the runtime from `fr.hammons.slinc.runtime`.