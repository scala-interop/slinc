## Project overview

The code in this repository is primarily divided into four top-level directories:
* `core/`: The base API and most of the implementation of Slinc
* `j17/`: Java 17 specific parts of the Slinc implementation
* `j19/`: Java 19 specific parts of the Slinc implementation
* `runtime/`: The complete Slinc project, with Java 17 and 19 implementations combined and the correct implementation chosen at runtime by your system.
  
## Developing with Slinc
Slinc is a somewhat involved project to program for. With implementations targeting Java 17 and Java 19, one needs access to both JDKs to develop and run the j17 and j19 projects. 

### Sdkman

In order to develop Slinc, it's suggested that one goes to [sdkman.io](https://sdkman.io/) and installs their software development kit manager based on the platform you're working in. The sdkman website has easy to follow instructions for installing on MacOS and Linux, and the tool will allow you to install multiple JDKs and switch between them with ease.

Once you have sdkman installed, you can use the following commands to install and use the standard JDKs for this project:

#### Java 19
* Install: `sdk i java 19-tem`
* Use: `sdk u java 19-tem`
* Default: `sdk d java 19-tem`
  
#### Java 17
* Install: `sdk i java 17.0.4.1-tem`
* Use: `sdk u java 17.0.4.1-tem`
* Default: `sdk d java 17.0.4.1-tem`

`Install` will of course install the JVM in question to your system (specifically in a portion of your user directory). Once installed, sdkman will prompt you if you want to make the JDK that was installed your default JDK. Choosing to do so will make that JDK the one seen by most all programs launched by the user in question.

`Use` will set the JDK visible for a certain terminal instance. This is useful if you want to build the entire project. Use will only effect the JDK choice for the terminal it's invoked in, and will not affect the user default JVM.

`Default` will set the JDK that's visible for all programs launched after the command has been invoked. This is useful for reloading your code editor to work on a different java implementation for Slinc. 

### Editor
When developing Slinc, it's suggested to use [VSCode](https://code.visualstudio.com/) along with the [Metals](https://marketplace.visualstudio.com/items?itemName=scalameta.metals) extension. Slinc is heavily dependent on compile-time programming, and VSCode+Metals works very well with this development model. One can use other editors, but it's probably mandatory to use Metals. 

Using metals, one can import the build definition from mill. If one encounters an issue with the import failing for no discernable reason, try deleting the `out` directory and trying again. There is a problem with this project and mill failing to generate bloop configurations. If one encounters errors when viewing a code base that do not resolve themselves, it's suggested to try closing VSCode, killing all Java processes, and deleting .metals, .bloop, and out. Generally, this will fix all issues. 

When developing for Slinc, choose an implementation to focus on, and choose the appropriate JDK for it. Switch with the appropriate `default` command on sdkman, kill all java processes, and afterwards open the project with VSCode. The corresponding `j` project should be having no missing definition errors after this process. Switching between JDK versions follows the same process. 

## Compiling 

The following commands compile the Slinc projecs:

* core: `./mill core.compile`
* j17: `./mill j17.compile`
* j19: `./mill j19.compile`
* runtime: `./mill runtime.compile`

Compiling the entire project would normally be done by running `./mill _.compile`, but considering the different project have different JDK requirements, the full compilation takes the form of 

```bash
sdk u java 17.0.4.1-tem && \
./mill core.compile && \
./mill core.test.compile && \
./mill j17.compile && \
sdk u java 19-tem && \
./mill j19.compile && \
#optional sdk u java 17.0.4.1-tem &&
./mill runtime.compile
```

Only j17 and j19 have a hard dependency on specific JDK major versions. Core and runtime can be compiled with either java 17 or 19 as you wish.


## Testing
Tests exist for all portions of the project. They can be executed by running `./mill <project-name>.test`. Examples are:

* `./mill j17.test`
* `./mill j19.test`
* `./mill core.test`
* `./mill runtime.test`

Please note that testing runtime involves doing the delicate compilation dance listed above.

Testing code is generally stored in the `core` project under `core/test/src`. Java 17, Java 19, and runtime specific tests may exist in the future, but at the moment, all implementations use a generic testing base.

Tests in Slinc use munit and scalacheck. One can read how to use munit with scalacheck [here](https://scalameta.org/munit/docs/integrations/scalacheck.html) and how to use scalacheck [here](https://github.com/typelevel/scalacheck/blob/main/doc/UserGuide.md).


In order to develop a new test suite for Slinc, add the implementation to `core/test/src`. If the test suite is testing an implementation in `core` then one can define it in the normal way specified by the munit documentation. However, if it's meant to be a test of Slinc implementations, it should be defined in a generic fashion like so: 

```scala
package fr.hammons.slinc

import munit.ScalaCheckSuite

trait MyTestSuite(slinc: Slinc) extends ScalaCheckSuite:
  import slinc.{*,given}
  test("myTest") { 
    assertEquals(4,4)
  }
```

After defining this in core, add the test to `j17/test/src/fr/hammons/slinc` and `j19/test/src/fr/hammons/slinc` with the following code:

```scala
package fr.hammons.slinc

class MyTestSuite17 extends MyTestSuite(Slinc17.default)
```

If one's test suite concerns JIT compilation, one can use `noJit` and `immediate` implementations to make one's test suites test the unjitted and jitted versions of the runtime.

### Troubleshooting tests

Sometimes when running a freshly written test, or testing freshly written code, one might encounter a situation where the test suite will stop testing early, or never stop running. 

Generally, the test suite will stop running early when some part of the Slinc runtime fails to initialize properly. One can easily detect if this is the case by moving some test code out of the test section into the root of the suite. 

Observe the following example:

```scala
trait MySuite(s: Slinc) extends ScalacheckSuite:
  import s.{*, given}
  test("myTest") {
    assertEquals(sizeOf[Int], 4.as[SizeT]
  }
```

should be rewritten to

```scala
trait MySuite(s: Slinc) extends ScalacheckSuite:
  import s.{*,given}
  sizeOf[Int]
  4.as[SizeT]
  
  test("myTest") {
    assertEquals(sizeOf[Int], 4.as[SizeT])
  }
```

This tends to force the test suite to actually reveal the exception that's breaking it, and will help one fix the issue in question.

When a test suite continues forever, the cause is usually the same, but with regards to a propertyBased test:

```scala
trait MySuite(s: Slinc) extends ScalacheckSuite:
  import s.{*, given}

  property("myProperty") {
    forAll{
      (i: Int) => 
        Scope.confined{
          val ptr = Ptr.blank[CInt]

          !ptr = i 
          assertEquals(!ptr, i)
        }
    }
  }
```

should be changed to 

```scala
trait MySuite(s: Slinc) extends ScalacheckSuite:
  import s.{*, given}
  val ptr = Ptr.blank[CInt]

  !ptr = 4
  !ptr

  property("myProperty") {
    forAll{
      (i: Int) => 
        Scope.confined{
          val ptr = Ptr.blank[CInt]

          !ptr = i 
          assertEquals(!ptr, i)
        }
    }
  }
```