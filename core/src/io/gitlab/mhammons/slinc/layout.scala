package io.gitlab.mhammons.slinc

import scala.quoted.*
import scala.util.chaining.*
import jdk.incubator.foreign.CLinker.{C_INT, C_LONG, C_FLOAT}
import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.MemorySegment
import scala.collection.mutable.HashMap
import java.lang.invoke.VarHandle
import io.gitlab.mhammons.polymorphics.VarHandleHandler

inline def deriveLayout[T <: StructBacking]: MemoryLayout = ${
   deriveLayoutImpl[T]
}

inline def structFromMemorySegment[T <: StructBacking](
    memorySegment: MemorySegment
) =
   ${
      structFromMemorySegmentImpl[T]('memorySegment)
   }

def structFromMemorySegmentImpl[T <: StructBacking: Type](
    expr: Expr[MemorySegment]
)(using
    Quotes
) =
   import quotes.reflect.*

   val varHandles =
      refinementDataExtraction2(TypeRepr.of[T]).map((name, typ) =>
         (layout: Expr[MemoryLayout]) =>
            typ match
               case '[t] =>
                  '{
                     ${ Expr(name) } -> Fd[t](
                       $expr,
                       VarHandleHandler(
                         $layout.varHandle(
                           scala2MethodTypeArg[t],
                           MemoryLayout.PathElement.groupElement(${
                              Expr(name)
                           })
                         )
                       )
                     )
                  }
      )
   '{
      val l = deriveLayout[T]
      (StructBacking(${
         Varargs(varHandles.map(_('{ l })))
      }.toMap + ("$mem" -> $expr) + ("$layout" -> l))).asInstanceOf[T]
   }

def deriveLayoutImpl[T: Type](using q: Quotes) =
   import quotes.reflect.*

   val s = Symbol.classSymbol("io.gitlab.mhammons.slinc.Struct")
   val baseType = TypeRepr.of[T].baseClasses.contains(s)
   val ts = TypeRepr.of[T].typeSymbol

   val fields: Seq[Expr[MemoryLayout]] = refinementDataExtraction2(
     TypeRepr.of[T]
   ).reverse.map((name, typ) =>
      '{ ${ layouts(typ) }.withName(${ Expr(name) }) }
   )

   //    .collect { case cp: ClassDef =>
   //       ClassDef.copy(cp)(
   //         "$anon",
   //         cp.constructor,
   //         cp :: cp.parents,
   //         cp.self,
   //         cp.body
   //       )
   //    }
   //    .map(_ => "")
   //    .getOrElse("")
   '{
      println("derived layout")
      MemoryLayout
         .structLayout(${ Varargs(fields) }*)
         .withName(${ Expr(TypeRepr.of[T].typeSymbol.name) })
         .tap(println)
   }

val layouts = (q: Quotes) ?=>
   (t: Type[?]) =>
      t match {
         case '[Fd.int]   => '{ C_INT }
         case '[Fd.long]  => '{ C_LONG }
         case '[Fd.float] => '{ C_FLOAT }
      }

def refinementDataExtraction(using Quotes)(
    typ: quotes.reflect.TypeRepr
): List[Expr[MemoryLayout]] =
   import quotes.reflect.*
   typ.dealias match
      case Refinement(ancestor, name, typ) =>
         '{
            ${ layouts(typ.asType) }.withName(${ Expr(name) })
         } :: refinementDataExtraction(ancestor)
      case _ => Nil

def refinementDataExtraction2(using Quotes)(
    typ: quotes.reflect.TypeRepr
): List[(String, Type[?])] =
   import quotes.reflect.*
   typ.dealias match
      case Refinement(ancestor, name, typ) =>
         name -> typ.asType :: refinementDataExtraction2(ancestor)
      case _ => Nil
