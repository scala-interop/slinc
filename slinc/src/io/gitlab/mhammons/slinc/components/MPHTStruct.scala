package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import jdk.incubator.foreign.MemorySegment
import io.gitlab.mhammons.slinc.{
   StructMacros,
   NativeCache,
   missingNativeCache,
   Struct
}
import scala.util.chaining.*

class MPHTStruct(
    minimalPerfectHashTable: MinimalPerfectHashtable[Any],
    memorySegment: MemorySegment
) extends Struct(memorySegment):
   def selectDynamic(key: String) = minimalPerfectHashTable(key)

object MPHTStruct:
   inline def structFromMemSegment[A](memSegment: MemorySegment): A = ${
      structFromMemSegmentImpl[A]('memSegment)
   }

   private def structFromMemSegmentImpl[A: Type](
       memSegmentExpr: Expr[MemorySegment]
   )(using Quotes) =
      import quotes.reflect.*

      val structInfo = StructMacros.getStructInfo[A]
      val keys = structInfo.members.map {
         case PrimitiveInfo(name, _) => name
         case StructStub(name, _)    => name
      }

      val salts = MinimalPerfectHashtable.findSalts(keys)

      def fn2(parentLayout: Expr[StructLayout]): Expr[Seq[?]] =
         structInfo.members
            .map {
               case StructStub(name, '[a]) =>
                  (
                    name,
                    '{
                       structFromMemSegment[a](
                         $parentLayout
                            .subsegmntOf(${ Expr(name) }, $memSegmentExpr)
                       )
                    }
                  )
               case PrimitiveInfo(name, '[a]) =>
                  (
                    name,
                    '{
                       Member(
                         $memSegmentExpr,
                         $parentLayout.varhandleOf(${ Expr(name) })
                       )
                    }
                  )
            }
            .map((k, v) =>
               (
                 MinimalPerfectHashtable
                    .indexOfStr(k, salts.toArray, salts.length),
                 v
               )
            )
            .sortBy(_._1)
            .map(_._2)
            .pipe(Expr.ofSeq)

      val sortedKeys =
         Expr(
           keys.map(
             MinimalPerfectHashtable.indexOfStr(_, salts.toArray, salts.length)
           )
         )

      val saltsExpr = Expr.ofSeq(salts.map(Expr(_)))

      val nc = Expr.summon[NativeCache].getOrElse(missingNativeCache)

      '{
         val msgmnt = $memSegmentExpr
         val nativeCache = $nc

         val layout = nativeCache.layout[A]

         val mpht = new MinimalPerfectHashtable(
           Array(${ saltsExpr }*),
           Array(${ fn2('layout) }*)
         )

         MPHTStruct(mpht, msgmnt).asInstanceOf[A]
      }.tap(e => report.info(e.show))
