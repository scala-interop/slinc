package fr.hammons.slinc

import compiletime.summonInline

import fr.hammons.slinc.internal.ast.Expression
import fr.hammons.slinc.internal.ast.StackAlloc
import fr.hammons.slinc.internal.ast.Block
import fr.hammons.slinc.internal.Describe

import java.lang.foreign.MemoryLayout
import java.lang.foreign.ValueLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.Arena

import fr.hammons.slinc.internal.ast.PTypeDescriptor
import quoted.*
import java.lang.invoke.VarHandle
import fr.hammons.slinc.internal.FieldGenerator

object TestPlatform extends Runtime:
  def platform: Platform = Platform.LinuxX64

  def makeStruct(
      selectorFn: String => Int,
      backingGen: Array[FieldGenerator]
  ): Struct = ???

  private def memoryLayoutFromPType(
      ptypeDescriptor: PTypeDescriptor
  ): MemoryLayout =
    ptypeDescriptor match
      case PTypeDescriptor.IntTypeDescriptor    => ValueLayout.JAVA_INT.nn
      case PTypeDescriptor.FloatTypeDescriptor  => ValueLayout.JAVA_FLOAT.nn
      case PTypeDescriptor.DoubleTypeDescriptor => ValueLayout.JAVA_DOUBLE.nn
      case PTypeDescriptor.ShortTypeDescriptor  => ValueLayout.JAVA_SHORT.nn
      case PTypeDescriptor.ByteTypeDescriptor   => ValueLayout.JAVA_BYTE.nn
      case PTypeDescriptor.LongTypeDescriptor =>
        ValueLayout.JAVA_LONG.nn
      case PTypeDescriptor.PointerTypeDescriptor => ValueLayout.ADDRESS.nn
      case PTypeDescriptor.StructTypeDescriptor(fieldDescriptors*) =>
        MemoryLayout
          .structLayout(
            fieldDescriptors.map((s, l) =>
              memoryLayoutFromPType(l).nn.withName(s)
            )*
          )
          .nn
      case PTypeDescriptor.FixSizedArrayTypeDescriptor(containedType, number) =>
        ???
      case PTypeDescriptor.UnionTypeDescriptor(possibleTypes) => ???
      case PTypeDescriptor.VoidTypeDescriptor                 => ???

  private inline def structToSwitch[A] = ${
    ???
  }

  private def structToSwitch[A](
      name: Expr[String]
  )(using Quotes, Type[A]): Expr[Int] =
    import quotes.reflect.*

    def handleRefinement(count: Int, repr: TypeRepr): Seq[CaseDef] =
      repr match
        case Refinement(parent, name, _) =>
          CaseDef(
            Expr(name).asTerm,
            None,
            Expr(count).asTerm
          ) +: handleRefinement(count, repr)
        case _ => Seq.empty

    Match(name.asTerm, handleRefinement(0, TypeRepr.of[A]).toList)
      .asExprOf[Int]

  inline def switchMaker[A](name: String): Int = ${
    structToSwitch[A]('name)
  }

  inline def varHandleMaker[A](
      memoryLayout: MemoryLayout,
      memorySegment: MemorySegment
  ): Array[Field[?]] = ${
    varHandleMakerImpl[A]('memorySegment, 'memoryLayout)
  }

  private def varHandleMakerImpl[A](
      segmentExpr: Expr[MemorySegment],
      memLayoutExpr: Expr[MemoryLayout]
  )(using Quotes, Type[A]): Expr[Array[Field[?]]] =
    import quotes.reflect.*

    def handleRefinement(count: Int, repr: TypeRepr): Seq[Expr[Field[?]]] =
      repr match
        case Refinement(parent, name, repr) =>
          '{
            new Field[Matchable]($segmentExpr):
              val varHandle: VarHandle = $memLayoutExpr
                .varHandle(
                  MemoryLayout.PathElement.groupElement(${ Expr(name) })
                )
                .nn
          } +: handleRefinement(count + 1, parent)
        case _ =>
          Seq.empty

    val seq = Expr.ofSeq(handleRefinement(0, TypeRepr.of[A]))

    '{ Array($seq*) }

  def run[A](expression: Expression[A]): A = ???
  inline def runInline[A](inline expression: Expression[A]): A =
    expression match
      case stackAlloc: StackAlloc[A] =>
        val ptype = summonInline[Describe[A]].apply

        val str = new Struct:
          val memLayout = memoryLayoutFromPType(ptype)
          val memSegment = Arena
            .ofAuto()
            .nn
            .allocate(
              memLayout
            )
            .nn
          val fieldHandles: Array[Field[?]] =
            varHandleMaker[A](memLayout, memSegment)
          def selector(value: String): Int = switchMaker[A](value)

          override def selectDynamic(key: String): Object = fieldHandles(
            selector(key)
          )

        str.asInstanceOf[A]
      case Block(startingExpressions, lastExpression) => ???

  def makeStruct(
      selectorFn: String => Int,
      description: PTypeDescriptor.StructTypeDescriptor
  ): Struct =
    ???
