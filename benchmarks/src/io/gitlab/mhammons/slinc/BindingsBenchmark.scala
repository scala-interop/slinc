package io.gitlab.mhammons.slinc

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import cats.catsInstancesForId
import scala.annotation.tailrec

@State(Scope.Thread)
class BindingsBenchmark:
   val getpid = NativeIO.function[() => Long]("getpid")
   val strlen = NativeIO.function[String => Int]("strlen")

   @tailrec
   final def nativeIORepeat[A](io: NativeIO[A], remaining: Int): NativeIO[A] =
      if remaining > 0 then nativeIORepeat(io.flatMap(_ => io), remaining - 1)
      else io

   @Benchmark
   @Fork(
     jvmArgsAppend = Array(
       "--add-modules",
       "jdk.incubator.foreign",
       "--enable-native-access",
       "ALL-UNNAMED"
     )
   )
   @OutputTimeUnit(TimeUnit.MICROSECONDS)
   def strlenBench =
      NativeIO
         .scope(nativeIORepeat(strlen("hello world"), 100))
         .foldMap(NativeIO.impureCompiler)

   @Benchmark
   @Fork(
     jvmArgsAppend = Array(
       "--add-modules",
       "jdk.incubator.foreign",
       "--enable-native-access",
       "ALL-UNNAMED"
     )
   )
   @OutputTimeUnit(TimeUnit.MICROSECONDS)
   def getpidBench =
      nativeIORepeat(getpid(), 5).foldMap(NativeIO.impureCompiler)
