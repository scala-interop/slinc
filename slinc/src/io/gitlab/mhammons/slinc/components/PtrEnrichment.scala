package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import io.gitlab.mhammons.slinc.Ptr
import scala.util.chaining.*
import jdk.incubator.foreign.*, MemoryLayout.PathElement

object PtrEnrichment:
   transparent inline def PartialCapable[A](p: Ptr[A]) = ${
      selectableImpl[A]('p)
   }

   def selectableImpl[A: Type](nptr: Expr[Ptr[A]])(using Quotes) =
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

   def produceDualRefinement(typeInfo: TypeInfo)(using Quotes): Type[?] =
      import quotes.reflect.*
      typeInfo match
         case ProductInfo(name, members, '[a]) =>
            members
               .foldLeft(TypeRepr.of[Ptr[a]]) { (accum, m) =>
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
            Type.of[Ptr[a]]
         case PtrInfo(_, _, _) => ???

   def getNPtrs(
       typeInfo: TypeInfo,
       segment: Expr[MemorySegment],
       layout: Expr[MemoryLayout],
       path: Seq[Expr[PathElement]]
   )(using Quotes): Expr[Ptr[?]] =
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
               Ptr[a](
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
               Ptr[a](
                 $segment,
                 $layout.byteOffset(${ Expr.ofSeq(modifiedPath) }*)
               )
            }
