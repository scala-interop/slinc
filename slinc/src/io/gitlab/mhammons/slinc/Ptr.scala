package io.gitlab.mhammons.slinc

import scala.quoted.*
import scala.util.chaining.*
import jdk.incubator.foreign.{
   MemorySegment,
   MemoryLayout,
   MemoryAddress,
   CLinker
}, MemoryLayout.PathElement, CLinker.C_POINTER
import components.{
   Deserializer,
   Serializer,
   TypeInfo,
   NativeInfo,
   summonOrError,
   ProductInfo,
   PrimitiveInfo,
   PtrInfo,
   Deserializable,
   SerializeFrom
}
import scala.reflect.ClassTag

class Ptr[A: ClassTag](
    memoryAddress: MemoryAddress,
    offset: Long,
    map: => Map[String, Any] = Map.empty
) extends Selectable:
   lazy val myMap = map
   def `unary_!` : Deserializable[A] =
      Deserializer.from(
        memoryAddress,
        offset
      )
   def `unary_!_=`(a: A): SerializeFrom[A] =
      SerializeFrom(a, memoryAddress, offset)

   def +(plus: Long)(using layoutOf: NativeInfo[A]) = Ptr[A](
     memoryAddress.addOffset(layoutOf.layout.byteSize * plus),
     offset,
     map
   )
   def asMemoryAddress = memoryAddress
   def selectDynamic(key: String) = myMap(key)
   def toArray(size: Int)(using Deserializer[A], NativeInfo[A]) =
      val l = NativeInfo[A].layout
      val elemSize = l.byteSize
      var i = 0
      val arr = Array.ofDim[A](size)
      while i < size do
         arr(i) = Deserializer.from(memoryAddress, i * elemSize + offset)
         i += 1
      arr

object Ptr:
   class Null[A: NativeInfo: ClassTag] extends Ptr[A](MemoryAddress.NULL, 0):
      override def `unary_!` =
         throw NullPointerException("SLinC Null Ptr attempted dereference")
      override def `unary_!_=`(a: A) =
         throw NullPointerException("SLinC Null Ptr attempted value update")
      override def asMemoryAddress = MemoryAddress.NULL
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
            val layout = Expr.summonOrError[NativeInfo[A]]
            '{
               val l = $layout.layout
               val memSegmnt = $nptr.asMemoryAddress
               $nptr.asInstanceOf[refinement]
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
       segment: Expr[MemoryAddress],
       layout: Expr[MemoryLayout],
       path: Seq[Expr[PathElement]]
   )(using Quotes): Expr[Ptr[?]] =
      val (modifiedPath, membersMap, typ) = typeInfo match
         case ProductInfo(name, members, typ) =>
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
               .pipe(expr => '{ $expr.toMap })

            (modifiedPath, membersMap, typ)
         case PrimitiveInfo(name, typ) =>
            val modifiedPath = path :+ '{
               PathElement.groupElement(${ Expr(name) })
            }
            (modifiedPath, '{ Map.empty }, typ)

      typ match
         case '[a] =>
            val ct = Expr.summonOrError[ClassTag[a]]
            '{
               Ptr[a](
                 $segment,
                 $layout.byteOffset(${ Expr.ofSeq(modifiedPath) }*),
                 $membersMap.toMap
               )(using $ct)
            }

   given gen[A]: NativeInfo[Ptr[A]] =
      summon[NativeInfo[String]].asInstanceOf[NativeInfo[Ptr[A]]]
