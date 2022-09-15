package fr.hammons.sffi

import scala.quoted.{ToExpr, Expr, Quotes}

opaque type Bytes = Long 

object Bytes:
  def apply(l: Long): Bytes = l

  extension (a: Bytes) 
    def +(b: Bytes): Bytes = a + b
    def toLong: Long = a 

  given ToExpr[Bytes] with 
    def apply(t: Bytes)(using Quotes) = ToExpr.LongToExpr[Long].apply(t)
