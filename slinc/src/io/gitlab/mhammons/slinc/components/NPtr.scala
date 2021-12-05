package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import scala.util.chaining.*
import jdk.incubator.foreign.{MemorySegment, MemoryLayout},
MemoryLayout.PathElement
class NPtr[A](
    memorySegment: MemorySegment,
    offset: Long,
    map: Map[String, Any] = Map.empty
) extends Selectable:
   def `unary_!`(using from: FromNative[A]): A =
      from.from(memorySegment, offset)
   def `unary_!_=`(a: A)(using to: ToNative[A]): Unit =
      to.into(a, memorySegment, offset)
   def asMemorySegment = memorySegment
   def asMemoryAddress = memorySegment.address
   def selectDynamic(key: String) = map(key)

object NPtr:
   extension [A](a: NPtr[A])
      transparent inline def partial = ${ selectableImpl[A]('a) }

   def selectableImpl[A: Type](nptr: Expr[NPtr[A]])(using Quotes) =
      import quotes.reflect.*
      val typeInfo = TypeInfo[A]
      produceDualRefinement(typeInfo) match
         case '[refinement] =>
            report.warning(Type.show[refinement])
            val layout = Expr.summonOrError[LayoutOf[A]]
            '{
               val l = $layout.layout
               val memSegmnt = $nptr.asMemorySegment
               ${
                  getNPtrs(typeInfo, 'memSegmnt, 'l, Nil).tap(
                    _.show.tap(report.warning)
                  )
               }.asInstanceOf[refinement]
            }

   // private def genPtr[A: LayoutOf](
   //     memorySegment: MemorySegment,
   //     map: Map[String, Any],
   //     path: Seq[PathElement]
   // ) = new NPtr[A](memorySegment, map, path)
   // transparent inline def apply[A](
   //     memorySegment: MemorySegment
   // )(using layout: LayoutOf[A]) = ${
   //    applyImpl[A]('memorySegment, 'layout)
   // }

   // private def applyImpl[A](
   //     memorySegmentExpr: Expr[MemorySegment],
   //     offsetExpr: Expr[Long],
   //     layoutExpr: Expr[LayoutOf[A]]
   // )(using
   //     Quotes,
   //     Type[A]
   // ) =
   //    val typeInfo = TypeInfo[A]

   //    typeInfo match
   //       case PrimitiveInfo(name, '[a]) => '{
   //          genPtr[A]($memorySegmentExpr, Map.empty, )
   //       }
   //    applyImpl
   //    produceDualRefinement[A] match
   //       case '[refinement] =>
   //          '{
   //             genPtr[A]($memorySegmentExpr, Map.empty, $offsetExpr)(using
   //               $layoutExpr
   //             ).asInstanceOf[refinement]
   //          }

   def produceDualRefinement(typeInfo: TypeInfo)(using Quotes): Type[?] =
      import quotes.reflect.*
      typeInfo match
         case ProductInfo(name, members, '[a]) =>
            members
               .foldLeft(TypeRepr.of[NPtr[a]]) { (accum, m) =>
                  Refinement(
                    accum,
                    m.name,
                    produceDualRefinement(m).pipe { case '[a] =>
                       TypeRepr.of[a]
                    }
                  )
               }
               .asType
         case PrimitiveInfo(name, '[a]) =>
            Type.of[NPtr[a]]
         case PtrInfo(_, _, _) => ???

   def getNPtrs(
       typeInfo: TypeInfo,
       segment: Expr[MemorySegment],
       layout: Expr[MemoryLayout],
       path: Seq[Expr[PathElement]]
   )(using Quotes): Expr[NPtr[?]] =
      typeInfo match
         case ProductInfo(name, members, '[a]) =>
            val modifiedPath =
               if name.isEmpty then path
               else
                  path :+ '{
                     PathElement.groupElement(${ Expr(name) })
                  }
            val membersMap = members
               .map(m =>
                  getNPtrs(m, segment, layout, modifiedPath).pipe(exp =>
                     '{ ${ Expr(m.name) } -> $exp }
                  )
               )
               .pipe(Expr.ofSeq)
            '{
               NPtr[a](
                 $segment,
                 $layout.byteOffset(${ Expr.ofSeq(modifiedPath) }*),
                 $membersMap.toMap
               )
            }
         case PrimitiveInfo(name, '[a]) =>
            val modifiedPath = path :+ '{
               PathElement.groupElement(${ Expr(name) })
            }
            '{
               NPtr[a](
                 $segment,
                 $layout.byteOffset(${ Expr.ofSeq(modifiedPath) }*)
               )
            }
