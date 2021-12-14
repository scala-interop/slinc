package io.gitlab.mhammons.slinc

import scala.quoted.*
import scala.util.chaining.*
import jdk.incubator.foreign.{MemorySegment, MemoryLayout, MemoryAddress},
MemoryLayout.PathElement
import components.{
   Deserializer,
   Serializer,
   TypeInfo,
   LayoutOf,
   summonOrError,
   ProductInfo,
   PrimitiveInfo,
   PtrInfo
}
import io.gitlab.mhammons.slinc.components.PtrEnrichment

class Ptr[A](
    memorySegment: MemorySegment,
    offset: Long,
    map: Map[String, Any] = Map.empty
) extends Selectable:
   def `unary_!`(using from: Deserializer[A]): A =
      from.from(memorySegment, offset)
   def `unary_!_=`(a: A)(using to: Serializer[A]): Unit =
      to.into(a, memorySegment, offset)

   def +(plus: Long) = Ptr[A](memorySegment, offset + plus, map)
   def asMemorySegment = memorySegment
   def asMemoryAddress = memorySegment.address
   // lazy val pt = PtrEnrichment.PartialCapable[A](this)
   // transparent inline def partial = ${ Ptr.selectableImpl[A]('this) }
   def selectDynamic(key: String) = map(key)

object Ptr:
   class Null[A] extends Ptr[A](null, 0, Map.empty):
      override def `unary_!`(using from: Deserializer[A]) =
         throw NullPointerException("SLinC Null Ptr attempted dereference")
      override def `unary_!_=`(a: A)(using to: Serializer[A]) =
         throw NullPointerException("SLinC Null Ptr attempted value update")
      override def asMemoryAddress = MemoryAddress.NULL
      override def asMemorySegment = throw NullPointerException(
        "SLinC Null Ptr cannot be made into a MemorySegment"
      )
   extension [A](a: Ptr[A])
      transparent inline def partial = ${ selectableImpl[A]('a) }

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
