package fr.hammons.slinc.modules

import fr.hammons.slinc.*

trait ReadWriteModule:
  def write[A](memory: Mem, offset: Bytes, value: A)(using
      DescriptorOf[A]
  ): Unit
  def writeArray[A](memory: Mem, offset: Bytes, value: Array[A])(using
      DescriptorOf[A]
  ): Unit
  def read[A](memory: Mem, offset: Bytes)(using DescriptorOf[A]): A
  def registerReader[A](fn: (Mem, Bytes) => A)(using DescriptorOf[A]): Unit
  def registerWriter[A](fn: (Mem, Bytes, A) => Unit)(using
      DescriptorOf[A]
  ): Unit
