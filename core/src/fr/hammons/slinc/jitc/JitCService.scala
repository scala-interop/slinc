package fr.hammons.slinc.jitc

import scala.quoted.Quotes
import scala.quoted.Expr
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.Future
import scala.quoted.staging.run
import java.util.UUID
import java.{util as ju}
import scala.util.Try

type JitCompiler = [A] => (
    Quotes ?=> Expr[A]
) => A
trait JitCService:
  def jitC(tag: UUID, c: JitCompiler => Unit): Unit
  def async: Boolean

object JitCService:
  lazy val standard = new JitCService:
    given compiler: scala.quoted.staging.Compiler =
      scala.quoted.staging.Compiler.make(getClass().getClassLoader().nn)

    private val shutdown = AtomicBoolean(false)
    private val tp = Executors.newWorkStealingPool(1).nn
    private val runnable: Runnable = () =>
      shutdown.setOpaque(true)
      tp.shutdown()

    Runtime.getRuntime().nn.addShutdownHook(Thread(runnable))
    given ExecutionContext = ExecutionContext.fromExecutor(tp)

    private val workQueue = AtomicReference(
      Vector.empty[(UUID, JitCompiler => Unit)]
    )
    private var workDone =
      Vector.empty[UUID]

    private val doneCache = 32

    Future {
      while !shutdown.getOpaque() do
        val start = System.currentTimeMillis()
        val workToDo = workQueue
          .getAndSet(Vector.empty)
          .nn
          .distinctBy(_._1)
          .filter((id, _) => !workDone.contains(id))

        val done = for
          (id, work) <- workToDo
          pfn: JitCompiler = [A] => (fn: Quotes ?=> Expr[A]) => run[A](fn)
          workDone <- Try {
            work(pfn)
            Vector(id)
          }.recover { case t =>
            t.printStackTrace()
            Vector.empty[UUID]
          }.getOrElse(Vector.empty)
        yield workDone

        workDone = done.take(32) ++ workDone.dropRight(
          math.max(done.take(32).size + workDone.size - doneCache, 0)
        )

        val end = System.currentTimeMillis()
        val duration = end - start
        println(s"sleeping ${Math.max(100 - duration, 0)} ms")
        Thread.sleep(Math.max(100 - duration, 0))
    }

    override def jitC(uuid: UUID, fn: JitCompiler => Unit) =
      import language.unsafeNulls
      var succeeded = false
      while !succeeded do
        val workToDo = workQueue.getOpaque()
        succeeded = workQueue.compareAndSet(
          workToDo,
          (workToDo :+ (uuid, fn))
        )

    override def async: Boolean = true

  lazy val synchronous = new JitCService:
    private val wdoneCache = 32
    given compiler: scala.quoted.staging.Compiler =
      scala.quoted.staging.Compiler.make(getClass().getClassLoader().nn)

    private val workDone: AtomicReference[Vector[UUID]] = AtomicReference(
      Vector.empty
    )

    override def jitC(tag: UUID, c: JitCompiler => Unit): Unit =
      val pfn = [A] => (fn: Quotes ?=> Expr[A]) => run[A](fn)
      c(pfn)
      var succeeded = false
      while !succeeded do
        val wDone = workDone.get().nn
        val toDrop = if wDone.size == wdoneCache then 1 else 0
        succeeded =
          workDone.compareAndSet(wDone, tag +: wDone.dropRight(toDrop))

    override def async: Boolean = false
