---
title: slinc
layout: main
---

# **sl**in**c** - **S**cala **L**ink to **C**

![stars](https://badgen.net/gitlab/stars/mhammons/slinc)![lastcommit](https://badgen.net/gitlab/last-commit/mhammons/slinc)
![pipeline status](https://gitlab.com/mhammons/slinc/badges/master/pipeline.svg)![Maven Central](https://img.shields.io/maven-central/v/io.gitlab.mhammons/slinc_3)


[![fork me](https://img.shields.io/badge/gitlab-fork%20me-orange?logo=gitlab&style=for-the-badge)](https://gitlab.com/mhammons/slinc)

[Slinc](https://gitlab.com/mhammons/slinc) is a Scala 3 library that allows users to interoperate with C code via Java 17's [foreign api incubator](https://docs.oracle.com/en/java/javase/17/docs/api/jdk.incubator.foreign/jdk/incubator/foreign/package-summary.html).

It's designed to make use of Scala's type system and macros to handle most of the work of making bindings to C from Scala.

## Quickstart

slinc is published to Maven Central for Scala 3. Adding it to your build requires a bit of work at present thanks to the experimental nature of project panama. For all build systems, you will need to be running Java 17.

### SBT setup

In your build.sbt:
```
libraryDependencies += "io.gitlab.mhammons" %% "slinc" % "0.1.0"
//if forking
javaOptions ++= Seq("--add-modules=jdk.incubator.foreign", "--enable-native-access=ALL-UNNAMED")
```
In .jvmopts in the root of your build:
```
--add-modules=jdk.incubator.foreign
--enable-native-access=ALL-UNNAMED
```

### Mill setup
In your project's `ScalaModule`:
```
def ivyDeps = Agg(ivy"io.gitlab.mhammons::slinc:0.1.0")
def forkArgs = Seq(
   "--add-modules=jdk.incubator.foreign",
   "--enable-native-access=ALL-UNNAMED"
)
```
In .mill-jvm-opts:
```
--add-modules=jdk.incubator.foreign
--enable-native-access=ALL-UNNAMED
```

Once you have your build system set up, you can create a new file and write the following code:
```scala
import io.gitlab.mhammons.slinc.*

case class div_t(quot: Int, rem: Int) derives Struct

def div(numer: Int, denom: Int) = bind[div_t]

@main def calc =
   val (quot, rem) = Tuple.fromProduct(div(5, 2))
   println(s"got a quotient of $quot and a remainder $rem")
```

This library has a set of helper syntax and extension methods, accesible via importing `import io.gitlab.mhammons.slinc.*`

Please look at the usage section to the left and the documentation of the elements in [[io.gitlab.mhammons.slinc]] for more details of using this library.