package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.MemorySegment
import io.gitlab.mhammons.polymorphics.VarHandleHandler
import jdk.incubator.foreign.GroupLayout
import jdk.incubator.foreign.MemoryLayout, MemoryLayout.PathElement
import scala.quoted.*
import scala.util.chaining.*
import java.lang.invoke.VarHandle
import io.gitlab.mhammons.slinc.components.{Ptr, Member}
import components.{StructInfo, NamedVarhandle}
import io.gitlab.mhammons.slinc.components.MemLayout
import io.gitlab.mhammons.slinc.components.StructLayout
import scala.collection.concurrent.TrieMap
import io.gitlab.mhammons.slinc.components.{
   Template,
   Allocatable,
   LayoutOf,
   missingTemplate
}
import io.gitlab.mhammons.slinc.components.StructTemplate
import io.gitlab.mhammons.slinc.components.BoundaryCrossing
import io.gitlab.mhammons.slinc.components.StructElement
import io.gitlab.mhammons.slinc.components.SegmentTemplate

trait Struct(
    memorySegment: MemorySegment,
    layout: MemoryLayout,
    path: Seq[PathElement]
) extends Selectable:
   def selectDynamic(name: String): Any

   val $mem = memorySegment
   val $layout = layout
   val $path = path
   lazy val thisSize = layout.select(path*).byteSize
   lazy val offset = layout.byteOffset(path*)
   lazy val thisSegment = memorySegment.address
      .addOffset(offset)
      .asSegment(thisSize, memorySegment.scope)

object Struct:
   extension [S <: Struct: Template](s: S)
      def update(os: S) =
         s.thisSegment.copyFrom(os.thisSegment)

      def `unary_~` = Ptr[S](s.thisSegment.address)

   inline given [A <: Struct]: SegmentTemplate[A] = ${
      genStructTemplateImpl[A]
   }

   def genStructTemplateImpl[A <: Struct: Type](using q: Quotes) =
      val name = LayoutMacros.layoutNameImpl[A]

      val index = Expr(
        components.UniversalNativeCache.getLayoutIndex(name.valueOrAbort)
      )

      val templates = StructMacros
         .getStructInfo[A]
         .members
         .map { case StructElement(name, '[a]) =>
            val template = Expr
               .summon[Template[a]]
               .getOrElse(missingTemplate[a]) // todo: report proper error
            (name, template)
         }

      val namedTemplates = templates
         .map { case (name, templateExpr) =>
            '{ (${ Expr(name) }, $templateExpr) }
         }
         .pipe(Expr.ofSeq)

      '{
         components.UniversalNativeCache
            .getTemplate[A](
              $index,
              StructTemplate[A]($namedTemplates, summon[LayoutOf[A]].layout)
            )
            .asInstanceOf[SegmentTemplate[A]]
      }

   inline given [A <: Struct](using
       t: Template[A]
   ): BoundaryCrossing[A, MemorySegment] =
      components.UniversalNativeCache.getBoundaryCrossing(
        LayoutMacros.layoutIndex[A],
        new BoundaryCrossing[A, MemorySegment]:
           def toNative(a: A) = a.$mem
           def toJVM(sgmnt: MemorySegment) = summon[Template[A]](sgmnt)
      )
end Struct
class MapStruct(
    map: Map[String, Any],
    memorySegment: MemorySegment,
    layout: MemoryLayout,
    path: Seq[PathElement]
) extends Struct(memorySegment, layout, path):
   def selectDynamic(name: String) = map(name)

object StructMacros:
   lazy val structInfoCache = TrieMap.empty[Type[?], StructInfo]
   def getStructInfo[A: Type](using Quotes): StructInfo =
      structInfoCache.getOrElseUpdate(
        Type.of[A], {
           import quotes.reflect.*
           TypeRepr.of[A].dealias match
              case Refinement(ancestor, name, typ) =>
                 val typType = typ.asType

                 val thisType = typType match
                    case '[a] =>
                       StructElement(name, typType)

                 ancestor.asType.pipe { case '[a] =>
                    getStructInfo[a].pipe(res =>
                       res.copy(members = res.members :+ thisType)
                    )
                 }

              case repr if repr =:= TypeRepr.of[Pt1] =>
                 StructInfo(Seq.empty)

              case t =>
                 report.errorAndAbort(
                   s"Cannot extract refinement data for non-struct type ${t
                      .show(using Printer.TypeReprCode)}"
                 )
        }
      )
   end getStructInfo

   transparent inline def makePTDual[A] = ${
      makePTDualImpl[A]
   }

   def transformRefinement[A: Type](using Quotes): quotes.reflect.TypeRepr =
      import quotes.reflect.*
       TypeRepr.of[A].dealias match 
          case Refinement(ancestor, name, typ) => 
             ancestor.asType.pipe{ case '[a] => Refinement(transformRefinement[a], name, typ.asType.pipe{ case '[b] => TypeRepr.of[Wrap[b]]})}
          case repr if repr =:= TypeRepr.of[Pt1] => repr
   def makePTDualImpl[A: Type](using Quotes) =
      import quotes.reflect.*
      val newTypeRepr = transformRefinement[A].asType

      newTypeRepr match 
         case '[a] => '{
            Pt1(Map("h" -> Wrap(4), "k" -> Wrap("hello") )).asInstanceOf[a]
         }