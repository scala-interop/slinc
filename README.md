![workflow status](https://github.com/markehammons/slinc/actions/workflows/ci.yml/badge.svg)
![Maven Central](https://img.shields.io/maven-central/v/fr.hammons/slinc-runtime_3)

```
S     Lin     C
Scala Link to C
```

Slinc is a Scala 3 library that allows users to interoperate with C code via Java 17+'s foreign api. 

It's designed to make use of Scala's type system, macros, and runtime multi-stage programming to make bindings to C from Scala.

## Quickstart 

Slinc is published to Maven Central for Scala 3. It is built to take advantage of the cutting edge of Scala functionality and features, so your project will most certainly need to track the latest Scala version if you want to use the latest version of Slinc. Currently, the Scala version in use is `3.2.1`. 

In order to test Slinc quick, one can use scala-cli:

### test.scala
```scala
using lib "fr.hammons::slinc-runtime:0.1.1-72-1cedff"

import fr.hammons.slinc.runtime.{*,given}

case class div_t(quot: CInt, rem: CInt) derives Struct

object MyLib derives Library:
  def abs(i: CInt): CInt = Library.binding
  def div(numer: CInt, denom: CInt): div_t = Library.binding

@main def program = 
  println(MyLib.abs(-5)) // prints 5
  println(MyLib.div(5,2)) // prints div_t(2,1)
```

You can run this program with Java 19 via `scala-cli -j 19 -J --enable-native-access=ALL-UNNAMED test.scala` or with Java 17 via `scala-cli -j 17 -J --enable-native-access=ALL-UNNAMED -J --add-modules=jdk.incubator.foreign test.scala`.

To learn more about the library, refer to the documentation website for Slinc [here](https://slinc.hammons.fr/docs/index.html)