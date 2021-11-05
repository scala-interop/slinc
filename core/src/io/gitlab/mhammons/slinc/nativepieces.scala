package io.gitlab.mhammons.slinc

import scala.quoted.*
import jdk.incubator.foreign.CLinker
import jdk.incubator.foreign.MemoryLayout
import scala.util.chaining.*
import cats.free.Free
import cats.implicits.*
import scala.compiletime.error
object NativePieces:
   def functionImpl[A: Type](name: Expr[String])(using Quotes) =
      import quotes.reflect.*

      TypeRepr.of[A] match
         case AppliedType(t, subtypes)
             if t.typeSymbol.name.startsWith("Function") && t.typeSymbol.name
                .replace("Function", "")
                .toIntOption
                .isDefined =>
            val pos = Position.ofMacroExpansion

            subtypes.map(typeRepr2MemoryLayout)
            '{
               println(${
                  Expr(
                    subtypes
                       .map(_.typeSymbol.fullName)
                       .mkString(",")
                       .pipe(b =>
                          s"($b) ${pos.startColumn} ${pos.startLine} ${pos.sourceFile.content
                             .map(_.linesIterator.toVector(pos.startLine))}"
                       )
                  )
               })
               Free.liftF[NativeOp, Unit](NativeOp.Unit)
            }
         case f =>
            report.errorAndAbort(
              f
                 .show(using Printer.TypeReprStructure)
            )

   def typeRepr2MemoryLayout(using Quotes)(
       typeRepr: quotes.reflect.TypeRepr
   ): Expr[NativeIO[MemoryLayout]] =
      type2MemoryLayout(typeRepr.asType)

   def type2MemoryLayout(typ: Type[?])(using
       q: Quotes
   ): Expr[NativeIO[MemoryLayout]] =
      import quotes.reflect.*
      typ match
         case '[Int] =>
            '{ NativeIO.pure(CLinker.C_INT) }
         case '[Float]   => '{ NativeIO.pure(CLinker.C_FLOAT) }
         case '[Double]  => '{ NativeIO.pure(CLinker.C_DOUBLE) }
         case '[Boolean] => '{ NativeIO.pure(CLinker.C_CHAR) }
         case '[Char]    => '{ NativeIO.pure(CLinker.C_CHAR) }
         case '[String]  => '{ NativeIO.pure(CLinker.C_POINTER) }
         case '[Short]   => '{ NativeIO.pure(CLinker.C_SHORT) }
         case '[Long]    => '{ NativeIO.pure(CLinker.C_LONG) }
         case '[Fd[t]] => type2MemoryLayout(Type.of[t])
         case '[Ptr[t]]  => '{ NativeIO.pure(CLinker.C_POINTER) }
         case '[t & StructBacking] => '{ NativeIO.layout[t & StructBacking] }

   def refinementDataExtraction2(using Quotes)(
       typ: quotes.reflect.TypeRepr
   ): List[(String, Type[?])] =
      import quotes.reflect.*
      typ.dealias match
         case Refinement(ancestor, name, typ) =>
            name -> typ.asType :: refinementDataExtraction2(ancestor)
         case _ => Nil

   def deriveLayoutImpl[T: Type](using
       q: Quotes
   ): Expr[NativeIO[MemoryLayout]] =
      import quotes.reflect.*

      val typRepr = TypeRepr.of[T]
      val fields = Varargs(
        refinementDataExtraction2(typRepr).reverse.map((name, typ) =>
           '{ ${ type2MemoryLayout(typ) }.map(_.withName(${ Expr(name) })) }
        )
      )
      '{
         ${ fields }
            .traverse(identity)
            .map(pieces =>
               MemoryLayout
                  .structLayout(pieces.tap(println)*)
                  .withName(${ typRepr.typeSymbol.name.pipe(Expr.apply) })
            )
      }.tap(_.show.tap(report.info(_)))

   inline def deriveLayout[T] = ${ deriveLayoutImpl[T] }
end NativePieces
