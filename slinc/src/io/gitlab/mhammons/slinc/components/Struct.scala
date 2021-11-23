package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemorySegment, SegmentAllocator}
import io.gitlab.mhammons.slinc.NativeCache
import scala.collection.concurrent.TrieMap
import scala.quoted.*
import scala.util.chaining.*
import io.gitlab.mhammons.slinc.components.{primitive => Primitive}

trait struct(
    private[components] val memorySegment: MemorySegment,
    val layout: MemLayout,
    private[components] val template: MemorySegment => struct
) extends Selectable:
   def selectDynamic(key: String): Any

extension [A <: struct](a: A)
   def `unary_~` : Ptr[A] =
      Ptr(
        a.memorySegment.address,
        a.layout.byteSize(),
        a.template.andThen(_.asInstanceOf[A])
      )

type StructTemplate = MemorySegment => struct
object Struct:
   inline def allocate[A](
       inline builder: (
           MemorySegment,
           MemLayout,
           StructTemplate
       ) => struct
   )(using nc: NativeCache, seg: SegmentAllocator) =
      val layout = nc.layout[A]
      val template = nc.template[A]
      builder(seg.allocate(layout.underlying), layout, template).asInstanceOf[A]

   lazy val structInfoCache = TrieMap.empty[String, StructInfo]
   def getStructInfo[A: Type](using Quotes): StructInfo =
      structInfoCache.getOrElseUpdate(
        Type.show[A], {
           import quotes.reflect.*
           TypeRepr.of[A].dealias match
              case Refinement(ancestor, name, typ) =>
                 val typType = typ.asType

                 val thisType: PrimitiveInfo | StructStub = typType match
                    case '[struct] =>
                       StructStub(name, typType)

                    case '[Primitive[a]] =>
                       PrimitiveInfo(name, typType)
                 ancestor.asType.pipe { case '[a] =>
                    getStructInfo[a].pipe(res =>
                       res.copy(members = res.members :+ thisType)
                    )
                 }

              case repr if repr =:= TypeRepr.of[struct] =>
                 StructInfo(None, Seq.empty)

              case t =>
                 report.errorAndAbort(
                   s"Cannot extract refinement data for non-struct type ${t
                      .show(using Printer.TypeReprCode)}"
                 )
        }
      )
   end getStructInfo
