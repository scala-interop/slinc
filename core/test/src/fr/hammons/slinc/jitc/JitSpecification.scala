package fr.hammons.slinc.jitc

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.compiletime.codeOf
import scala.concurrent.duration.*
import scala.concurrent.Await

class JitSpecification extends munit.FunSuite:
  test("jit-compilation works"):
      var optimized = false
      var fn: OptimizableFn[Int => Int, DummyImplicit] =
        new FnToJit(
          JitCService.standard,
          CountbasedInstrumentation(10),
          jitCompiler =>
            jitCompiler('{ (optimizedFn: Boolean => Unit) => (i: Int) =>
              optimizedFn(true)
              i
            })(optimized = _),
          i => i((a: Int) => i.instrument(a))
        )
      for _ <- 0 to 10
      yield fn.get(3)

      while !fn.isOptimized do
        println("wait")
        fn.get
        Thread.sleep(100)
      fn.get(4)
      assertEquals(optimized, true)

  test("jit-compilation in multithreaded env works"):
      val optimized = Array.fill(10)(false)
      val fn: OptimizableFn[Int => Int, DummyImplicit] =
        new FnToJit(
          JitCService.standard,
          CountbasedInstrumentation(10),
          jitCompiler =>
            jitCompiler('{ (optimizedFn: Int => Unit) => (i: Int) =>
              optimizedFn(i)
              i
            })(i => optimized(i) = true),
          i => i((a: Int) => i.instrument(a))
        )
      val futures =
        for i <- 0 until 10
        yield Future {
          for _ <- 0 until 1
          yield fn.get(i)
          while !fn.isOptimized do
            fn.get
            Thread.sleep(100)

          fn.get(i)
        }

      futures.foreach(Await.result(_, Duration.Inf))

      assertEquals(optimized.toSeq, Seq.fill(10)(true))

  test("instant compilation works"):
      var optimized = false
      val fn: OptimizableFn[Int => Int, DummyImplicit] =
        new InstantJitFn[Int => Int, DummyImplicit](
          JitCService.standard,
          jitCompiler =>
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
      val ignoreInstrumentation = IgnoreInstrumentation(false)

      assertEquals(ignoreInstrumentation.getCount(), 0)
      ignoreInstrumentation.instrument(5)
      assertEquals(ignoreInstrumentation.getCount(), 0)

  test("Count instrumentation records invokations"):
      val countInstrumentation = CountbasedInstrumentation(100)

      assertEquals(countInstrumentation.getCount(), 0)
      countInstrumentation.instrument(5)
      assertEquals(countInstrumentation.getCount(), 1)

  test("Count instrumentation is accurate in multithreaded contexts"):
      val countInstrumentation = CountbasedInstrumentation(10)

      val results =
        for _ <- 0 until 10
        yield Future {
          countInstrumentation.instrument(5)
        }

      results.foreach(Await.result(_, 5.seconds))

      assert(countInstrumentation.shouldOpt)
