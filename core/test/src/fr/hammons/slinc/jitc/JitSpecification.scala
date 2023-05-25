package fr.hammons.slinc.jitc

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*
import org.scalacheck.Gen

class JitSpecification extends ScalaCheckSuite:
  test("single-threaded count works"):
      var optimizationTriggered = false
      val fn = UnoptimizedFunction(
        (i: Int) => i,
        10,
        () => optimizationTriggered = true
      )

      var i = 0
      while i < 10 do
        fn(_(6))
        i += 1

      assertEquals(fn.getCount(), 10)
      assert(
        optimizationTriggered,
        s"Optimization function not run? ${optimizationTriggered} - ${fn.getCount()}"
      )
      assertNoDiff(
        fn.originalCode,
        """|{
       |  def $anonfun(i: Int): Int = i
       |  closure($anonfun)
       |}
       |""".stripMargin
      )

  property("multi-threaded count works"):
      forAll(Gen.choose(0, 100)): (runs: Int) =>
        var optimizationTriggered = false
        val fn = UnoptimizedFunction(
          (i: Int) => i,
          10,
          () => optimizationTriggered = true
        )

        val futures: Seq[Future[Unit]] = for
          j <- 0 until 5
          res = Future(
            (0 until runs).foreach(i => fn(_(i * j)))
          )
        yield res

        Await.result(Future.traverse(futures)(identity), Duration.Inf)

        assert(fn.getCount() >= runs)
        assert(runs < 10 || optimizationTriggered)
