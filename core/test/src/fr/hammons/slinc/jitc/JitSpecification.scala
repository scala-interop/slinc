package fr.hammons.slinc.jitc

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.compiletime.codeOf

class JitSpecification extends munit.FunSuite:
  test("jit-compilation works"):
      var optimized = false
      var fn =
        new OptimizableFn[Int => Int, DummyImplicit](JitCService.standard)(
          i => i((a: Int) => i.instrument(a)),
          10
        )(jitCompiler =>
          jitCompiler('{ (optimizedFn: Boolean => Unit) => (i: Int) =>
            optimizedFn(true)
            i
          })(optimized = _)
        )
      for _ <- 0 to 10
      yield fn.get(3)

      while !JitCService.standard.processedRecently(fn.uuid) do
        Thread.sleep(100)
      fn.get(4)
      assertEquals(optimized, true)

  test("jit-compilation in multithreaded env works"):
      var optimized = false
      val fn =
        new OptimizableFn[Int => Int, DummyImplicit](JitCService.standard)(
          i => i((a: Int) => i.instrument(a)),
          10
        )(jitCompiler =>
          jitCompiler('{ (optimizedFn: Boolean => Unit) => (i: Int) =>
            optimizedFn(true)
            i
          })(
            optimized = _
          )
        )
      for _ <- 0 to 5
      yield Future {
        for _ <- 0 until 2
        yield fn.get(3)
      }

      while !JitCService.standard.processedRecently(fn.uuid) do
        println("waiting")
        Thread.sleep(100)

      fn.get(6)
      assertEquals(optimized, true)

  test("instant compilation works"):
      var optimized = false
      val fn =
        new OptimizableFn[Int => Int, DummyImplicit](
          JitCService.synchronous,
          IgnoreInstrumentation
        )(
          i => i((a: Int) => i.instrument(a)),
          0
        )(jitCompiler =>
          jitCompiler('{ (optimizedFn: Boolean => Unit) => (i: Int) =>
            optimizedFn(true)
            i
          })(optimized = _)
        )

      fn.get(6)
      assertEquals(optimized, true)

  test("instant compilation from properties"):
      System.setProperty(OptimizableFn.modeSetting, "immediate")
      var optimized = false
      val fn = OptimizableFn[Int => Int, DummyImplicit](jitCompiler =>
        jitCompiler('{ (optimizedFn: Boolean => Unit) => (i: Int) =>
          optimizedFn(true)
          i
        })(optimized = _)
      )(i => i((a: Int) => i.instrument(a)))

      fn.get(6)
      assertEquals(optimized, true)

  test("jitc disable from properties"):
      System.setProperty(OptimizableFn.modeSetting, "disabled")
      var optimized = false
      val fn = OptimizableFn[Int => Int, DummyImplicit](jitCompiler =>
        jitCompiler('{ (optimizedFn: Boolean => Unit) => (i: Int) =>
          optimizedFn(true)
          i
        })(optimized = _)
      )(i => i((a: Int) => i.instrument(a)))

      var i = 0
      while i < 100000 do
        fn.get(i)
        i += 1

      assertEquals(optimized, false)

  test("Ignore instrumentation records no info"):
      val ignoreInstrumentation = IgnoreInstrumentation

      assertEquals(ignoreInstrumentation.getCount(), 0)
      ignoreInstrumentation.instrument(5)
      assertEquals(ignoreInstrumentation.getCount(), 0)

  test("Count instrumentation records invokations"):
      val countInstrumentation = CountbasedInstrumentation()

      assertEquals(countInstrumentation.getCount(), 0)
      countInstrumentation.instrument(5)
      assertEquals(countInstrumentation.getCount(), 1)
