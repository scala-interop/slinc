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

type JitCompiler = [A] => (
    Quotes ?=> Expr[A]
) => A
trait JitCService:
  def jitC(tag: UUID, c: JitCompiler => Unit): Unit
  def processedRecently(tag: UUID): Boolean

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
    private val workDone = AtomicReference(
      Vector.empty[UUID]
    )
    private val doneCache = 32

    Future {
      while !shutdown.getOpaque() do

        val workToDo = workQueue.get().nn.distinctBy(_._1)

        for
          (_, work) <- workToDo
          pfn: JitCompiler = [A] => (fn: Quotes ?=> Expr[A]) => run[A](fn)
        do work(pfn)

        val done = workToDo.map(_._1)
        var succeeded = false
        while !succeeded do
          val wDone = workDone.get().nn
          val toDrop = math.max((wDone.size + done.size) - doneCache, 0)
          succeeded =
            workDone.compareAndSet(wDone, done ++ wDone.dropRight(toDrop))

        succeeded = false
        val doneSet = done.toSet
        while !succeeded do
          val newWork = workQueue.getOpaque().nn
          succeeded = workQueue.compareAndSet(
            newWork,
            newWork.filterNot((uuid, _) => doneSet.contains(uuid))
          )
        Thread.sleep(100)
    }

    override def jitC(uuid: UUID, fn: JitCompiler => Unit) =
      import language.unsafeNulls
      var succeeded = false
      while !succeeded do
        val wDone = workDone.get()
        if wDone.contains(uuid) then
          succeeded = workDone.compareAndSet(
            wDone,
            uuid +: wDone.filter(_ == uuid)
          )
        else
          while !succeeded do
            val workToDo = workQueue.get()
            succeeded = workQueue.compareAndSet(
              workToDo,
              (workToDo :+ (uuid, fn))
            )

    override def processedRecently(tag: ju.UUID): Boolean =
      workDone.getOpaque().nn.contains(tag)

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

    override def processedRecently(tag: ju.UUID): Boolean =
      workDone.getOpaque().nn.contains(tag)
