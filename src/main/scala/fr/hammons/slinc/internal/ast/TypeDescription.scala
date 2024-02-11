package fr.hammons.slinc.internal.ast

import scala.quoted.ToExpr
import scala.quoted.Expr
import scala.quoted.Quotes

enum PTypeDescriptor:
  case IntTypeDescriptor
  case FloatTypeDescriptor
  case DoubleTypeDescriptor
  case ShortTypeDescriptor
  case ByteTypeDescriptor
  case LongTypeDescriptor
  case PointerTypeDescriptor
  case StructTypeDescriptor(fieldDescriptors: (String, PTypeDescriptor)*)
  case FixSizedArrayTypeDescriptor(containedType: PTypeDescriptor, number: Int)
  case UnionTypeDescriptor(possibleTypes: PTypeDescriptor*)
  case VoidTypeDescriptor

object PTypeDescriptor:
  given ToExpr[PTypeDescriptor] with
    def apply(x: PTypeDescriptor)(using Quotes): Expr[PTypeDescriptor] =
      x match
        case IntTypeDescriptor     => '{ IntTypeDescriptor }
        case FloatTypeDescriptor   => '{ FloatTypeDescriptor }
        case DoubleTypeDescriptor  => '{ DoubleTypeDescriptor }
        case ShortTypeDescriptor   => '{ ShortTypeDescriptor }
        case ByteTypeDescriptor    => '{ ByteTypeDescriptor }
        case LongTypeDescriptor    => '{ LongTypeDescriptor }
        case PointerTypeDescriptor => '{ PointerTypeDescriptor }
        case StructTypeDescriptor(fieldDescriptors*) =>
          val descriptorExprs =
            Expr.ofSeq(fieldDescriptors.map { case (name, descriptor) =>
              Expr.ofTuple(Expr(name) -> apply(descriptor))
            })
          '{ StructTypeDescriptor($descriptorExprs*) }
        case FixSizedArrayTypeDescriptor(containedType, number) =>
          '{
            FixSizedArrayTypeDescriptor(
              ${ apply(containedType) },
              ${ Expr(number) }
            )
          }
        case UnionTypeDescriptor(possibleTypes*) =>
          val utds = Expr.ofSeq(possibleTypes.map(apply))
          '{
            UnionTypeDescriptor($utds*)
          }
        case VoidTypeDescriptor => '{ VoidTypeDescriptor }

opaque type PTypeDescriptor2 = String
object PTypeDescriptor2:
  inline def IntTypeDescriptor: PTypeDescriptor2 = "i"
  inline def FloatTypeDescriptor: PTypeDescriptor2 = "f"

  transparent inline def structTypeDescriptor(using inline p: Int) = p

  transparent inline def structTypeDescriptorHelper(
      inline sum: String,
      inline remaining: Tuple
  ): String =
    inline remaining match
      case head *: next => structTypeDescriptorHelper(sum + "," + head, next)
      case EmptyTuple   => sum + "}"
  // transparent inline def structTypeDescriptor(inline ts: Tuple) =
  //   structTypeDescriptorHelper("{", ts)

  // inline val s2 = structTypeDescriptor((IntTypeDescriptor, FloatTypeDescriptor))

//allowed c type definitions:
// - predefined
// - alias
// - struct
// - array
// - union
// - pointer
// - void
