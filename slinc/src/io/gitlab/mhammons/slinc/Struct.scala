package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.MemorySegment
import io.gitlab.mhammons.polymorphics.VarHandleHandler
import jdk.incubator.foreign.GroupLayout
import jdk.incubator.foreign.MemoryLayout
import scala.quoted.*
import scala.util.chaining.*
import java.lang.invoke.VarHandle
import io.gitlab.mhammons.slinc.components.Ptr
import components.{StructInfo, StructStub, PrimitiveInfo, NamedVarhandle}
import io.gitlab.mhammons.slinc.components.MemLayout

class Member[T](memSgmnt: MemorySegment, varHandle: VarHandle):
   def set(t: T) = VarHandleHandler.set(varHandle, memSgmnt, t)
   def get: T = VarHandleHandler.get(varHandle, memSgmnt).asInstanceOf[T]

   private[slinc] val mem = memSgmnt

object Member:
   type int = Member[Int]
   type float = Member[Float]
   type long = Member[Long]

class Struct(vals: Map[String, Any], memorySegment: MemorySegment)
    extends Selectable:
   def selectDynamic(name: String) = vals(name)

   private[slinc] val $mem = memorySegment

object Struct:
   extension [S <: Struct](s: S) inline def `unary_~` = Ptr[S](s)

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

   def getStructInfo[A: Type](using Quotes): StructInfo =
      import quotes.reflect.*
      TypeRepr.of[A].dealias match
         case Refinement(ancestor, name, typ) =>
            val typType = typ.asType

            val thisType: PrimitiveInfo | StructStub = typType match
               case '[Struct] =>
                  StructStub(name, typType)

               case '[a] =>
                  PrimitiveInfo(name, typType)
            ancestor.asType.pipe { case '[a] =>
               getStructInfo[a].pipe(res =>
                  res.copy(members = thisType +: res.members)
               )
            }

         case repr if repr =:= TypeRepr.of[Struct] =>
            StructInfo(None, Seq.empty)

         case t =>
            report.errorAndAbort(
              s"Cannot extract refinement data for non-struct type ${t.show(using Printer.TypeReprCode)}"
            )
   end getStructInfo

   inline def structFromMemSegment[A] = ${
      structFromMemSegmentImpl[A]
   }

   private def structFromMemSegmentImpl[A: Type](using Quotes) =
      ???

   inline def genVarHandles[A] = ${
      genVarHandlesImpl[A]
   }

   private def genVarHandlesImpl[A: Type](using q: Quotes) =
      import quotes.reflect.report
      import TransformMacros.{type2MethodTypeArg, type2MemLayout}
      val structInfo = getStructInfo[A]
      val nCache = Expr.summon[NativeCache].getOrElse(missingNativeCache)

      { (layoutExpr: Expr[MemLayout]) =>
         {
            structInfo.members
               .flatMap {

                  case PrimitiveInfo(name, '[a]) =>
                     val nameExp = Expr(name)
                     List(
                       '{
                          NamedVarhandle(
                            $nameExp,
                            $layoutExpr.varHandle(
                              ${ type2MethodTypeArg[a] },
                              MemoryLayout.PathElement.groupElement($nameExp)
                            )
                          )
                       }
                     )

                  case _ => Nil
               }
               .pipe(Expr.ofSeq)
         }
      }
         .pipe(lambdaList =>
            Expr.betaReduce('{
               val layout = $nCache.layout[A]
               ${ lambdaList('layout) }

            })
         )
         .tap(_.show.tap(report.info))
