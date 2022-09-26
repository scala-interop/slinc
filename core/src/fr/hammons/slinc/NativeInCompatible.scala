package fr.hammons.slinc

import scala.quoted.*

trait NativeInCompatible[A]

object NativeInCompatible:
  given NativeInCompatible[Int] with {}
  given NativeInCompatible[Float] with {}
  given NativeInCompatible[Long] with {}
  given NativeInCompatible[Double] with {}
  given NativeInCompatible[Byte] with {}
  given NativeInCompatible[Short] with {}
  given NativeInCompatible[Char] with {}

  type PossiblyNeedsAllocator = (Expr[Allocator => Any]) | Expr[Any]
  def handleInput(
      expr: Expr[Any]
  )(using q: Quotes): (Expr[Allocator => Any]) | Expr[Any] =
    import quotes.reflect.*
    expr match
      case '{ $exp: a } =>
        Expr
          .summon[InAllocatingTransitionNeeded[a]]
          .map(f =>
            (
                '{ (alloc: Allocator) =>  $f.in($exp)(using alloc) }
            ): PossiblyNeedsAllocator
          )
          .orElse{
            
            Expr
              .summon[InTransitionNeeded[a]]
              .map(f =>
                '{ $f.in($exp) }: PossiblyNeedsAllocator
              )
          }
          .getOrElse(expr: PossiblyNeedsAllocator)
