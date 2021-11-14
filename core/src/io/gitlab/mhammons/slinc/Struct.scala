package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.MemorySegment
import io.gitlab.mhammons.polymorphics.VarHandleHandler
import jdk.incubator.foreign.GroupLayout
import jdk.incubator.foreign.MemoryLayout
import scala.quoted.*
import scala.util.chaining.*
import java.lang.invoke.VarHandle

class Fd[T](memSgmnt: MemorySegment, varHandle: VarHandle):
   def set(t: T) = VarHandleHandler.set(varHandle, memSgmnt, t)
   def get: T = VarHandleHandler.get(varHandle, memSgmnt).asInstanceOf[T]

   private[slinc] val mem = memSgmnt

class Struct2(
    vals: Map[String, Any],
    memSgmnt: MemorySegment
) extends Selectable:
   private def selectDynamic(name: String) = vals(name)
   private[slinc] def backing = memSgmnt

object Fd:
   type int = Fd[Int]
   type float = Fd[Float]
   type long = Fd[Long]

class Struct(vals: Map[String, Any]) extends Selectable:
   self =>
   def selectDynamic(name: String) = vals(name)

   val $mem: MemorySegment = vals("$mem").asInstanceOf[MemorySegment]
   val `unary_~` = $mem.address

object StructMacros:
   def refinementDataExtraction[A: Type](using
       Quotes
   ): List[(String, Type[?])] =
      import quotes.reflect.*
      TypeRepr.of[A].dealias match
         case Refinement(ancestor, name, typ) =>
            name -> typ.asType :: ancestor.asType.pipe { case '[t] =>
               refinementDataExtraction[t]
            }
         case TypeRef(repr, name)
             if TypeRepr.of[Struct].typeSymbol.name == name =>
            Nil
         case t =>
            report.errorAndAbort(
              s"Cannot derive a layout for non-struct type ${t.show(using Printer.TypeReprCode)}"
            )

   def refinementDataExtraction2[A: Type](names: Seq[String] = Seq.empty)(using
       Quotes
   ): List[(Seq[String], Type[?])] =

      import quotes.reflect.*

      TypeRepr.of[A].dealias match
         case Refinement(ancestor, name, typ) =>
            val thisTypes = typ.asType match
               case '[Struct] | '[components.Struct] =>
                  typ.asType.pipe { case '[t] =>
                     refinementDataExtraction2[t](names :+ name)
                  }
               case '[NonEmptyTuple] =>
                  typ.asType.pipe { case '[t] =>
                     refinementDataExtraction2[t](names :+ name)
                  }
               case '[t] =>
                  List((names :+ name) -> typ.asType)

            thisTypes ++ ancestor.asType
               .pipe { case '[t] =>
                  refinementDataExtraction2[t](names)
               }
         case TypeRef(repr, name)
             if TypeRepr.of[Struct].typeSymbol.name == name =>
            Nil
         case t =>
            report.errorAndAbort(
              s"Cannot derive a layout for non-struct type ${t.show(using Printer.TypeReprCode)}"
            )

   inline def genVarHandles[A] = ${
      genVarHandlesImpl[A]
   }

   def genVarHandlesImpl[A: Type](using q: Quotes) =
      import quotes.reflect.report
      import TransformMacros.{type2MethodTypeArg, type2MemoryLayout}
      val refinementData = refinementDataExtraction[A]

      refinementData
         .map { case (name, '[a]) =>
            val nameExp = Expr(name)
            '{
               $nameExp -> ${ type2MemoryLayout[A] }
                  .varHandle(
                    ${ type2MethodTypeArg[a] },
                    MemoryLayout.PathElement.groupElement($nameExp)
                  )
            }
         }
         .pipe(Expr.ofSeq)
         .tap(_.show.tap(report.info))
