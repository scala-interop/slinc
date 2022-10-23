package fr.hammons.slinc

import scala.compiletime.uninitialized
import scala.quoted.staging.Compiler
import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class JitManagerSpec extends munit.FunSuite:
  test("immediately produces unoptimized code") {
    val compiler = Compiler.make(getClass().getClassLoader().nn)
    val jitManager =
      JitManagerImpl(compiler, 3)

    var code: (() => Int) | Null = null
    import scala.language.unsafeNulls
    jitManager.jitc(() => 1, _('{() => 2}), code = _)
    assertEquals(code(), 1)
  }

  test("runs jit after prescribed number of runs") {
    val compiler = Compiler.make(getClass().getClassLoader().nn)
    val jitManager =
      JitManagerImpl(compiler, 3)

    var code: (() => Int) | Null = null
    import scala.language.unsafeNulls
    jitManager.jitc(() => 1, _('{() => 2}), code = _)
    for 
      i <- 0 until 4
    yield code()

    Await.result(Future{
      var changed = false 
      while !changed do
        changed = code() != 1
        Thread.sleep(100)
    }, 15.seconds)
    assertEquals(code(), 2)
  }

  test("instant jit immediately compiles code") {
    val compiler = Compiler.make(getClass().getClassLoader().nn)
    val instantJit = 
      InstantJitManager(compiler)
    var code: (() => Int) | Null = null
    import scala.language.unsafeNulls
    instantJit.jitc(() => 1, _('{() => 2}), code = _)

    assertEquals(code(), 2)
  }

  test("no jit manager never compiles code") {
    var code: (() => Int) | Null = null
    import scala.language.unsafeNulls
    NoJitManager.jitc(() => 1, _('{() => 2}), code = _)

    for 
      _ <- 0 until 1000
    yield 
      assertEquals(code(), 1)
  }
