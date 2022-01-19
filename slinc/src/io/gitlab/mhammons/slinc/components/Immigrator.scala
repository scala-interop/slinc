package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{CLinker, MemoryAddress}

type Immigratee[A, B] = Immigrator[A] ?=> B

def immigrator[A]: Immigratee[A, Immigrator[A]] = summon[Immigrator[A]]

def immigrate[A](a: Any): Immigratee[A, A] = immigrator[A](a)

trait Immigrator[A]:
   def apply(a: Any): A

object Immigrator:
   given Immigrator[Unit] = _ => ()

   given Immigrator[Double] = _.asInstanceOf[Double]

   given Immigrator[Float] = _.asInstanceOf[Float]

   given Immigrator[String] = o =>
      CLinker.toJavaString(o.asInstanceOf[MemoryAddress])

   given Immigrator[Long] = _.asInstanceOf[Long]

   given Immigrator[Int] = _.asInstanceOf[Int]

   given Immigrator[Char] = _.asInstanceOf[Byte].toChar

   given Immigrator[Byte] = _.asInstanceOf[Byte]

   given Immigrator[Short] = _.asInstanceOf[Short]

   given Immigrator[Boolean] = a =>
      if a.asInstanceOf[Byte] == 1 then true else false
