![workflow status](https://github.com/markehammons/slinc/actions/workflows/ci.yml/badge.svg)![Maven Central](https://img.shields.io/maven-central/v/fr.hammons/slinc-core_3)

```
S     Lin     C
Scala Link to C
```

Slinc is a Scala 3 library that allows users to interoperate with C code via Java 17+'s foreign api. 

It's designed to make use of Scala's type system, macros, and runtime multi-stage programming to make bindings to C from Scala.

## Quickstart 

Slinc is published to Maven Central for Scala 3. It is built to take advantage of the cutting edge of Scala functionality and features, so your project will most certainly need to track the latest Scala version if you want to use the latest version of Slinc. Currently, the Scala version in use is `3.3.0-RC3`. 

### SBT setup 

In your `build.sbt`:

```scala
libraryDependencies += "fr.hammons" %% "slinc-runtime" % "0.2.0"
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
import fr.hammons.slinc.runtime.given

case class div_t(quot: CInt, rem: CInt) derives Struct 

trait MyLib derives Lib:
  def div(numer: CInt, denom: CInt): div_t

val myLib = Lib.instance[MyLib]

@main def calc = 
  val (quot, rem) = Tuple.fromProduct(myLib.div(5,2))
  println(s"Got a quotient of $quot and a remainder of $rem")
```

This library relies on the user importing the runtime from `fr.hammons.slinc.runtime`.

To learn more about the library, refer to the documentation website for Slinc [here](https://slinc.hammons.fr/docs/index.html)