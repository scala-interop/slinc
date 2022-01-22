package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemoryAddress, FunctionDescriptor, CLinker}
import scala.compiletime.erasedValue
import io.gitlab.mhammons.polymorphics.VoidHelper
import java.lang.invoke.{MethodType => MT, MethodHandles}
import scala.quoted.*
import scala.util.chaining.*
import scala.reflect.ClassTag
import java.lang.invoke.MethodHandle

/** Indicates how to turn JVM types into [[jdk.incubator.foreign.MemoryAddress]]
  * @tparam A
  *   The type to be turned into a MemoryAddresses
  */
trait Exporter[A]:
   def exportValue(a: A): Scopee[Allocatee[MemoryAddress]]

type Exportee[A, B] = Exporter[A] ?=> B

def exportValue[A](a: A): Scopee[Allocatee[Exportee[A, MemoryAddress]]] =
   summon[Exporter[A]].exportValue(a)

object Exporter:
   private inline def allocatingSerialize[A](
       a: A
   ): Allocatee[Informee[A, Encodee[A, MemoryAddress]]] =
      val address = allocate[A].address
      encode(a, address, 0)
      address

   def derive[A]: Informee[A, Encodee[A, Exporter[A]]] =
      new Exporter[A]:
         def exportValue(a: A) = allocatingSerialize(a)

   given Exporter[Int] = derive[Int]
   given Exporter[Long] = derive[Long]
   given Exporter[Float] = derive[Float]

   given Exporter[Double] = derive[Double]

   given Exporter[Short] = derive[Short]

   given Exporter[Boolean] = derive[Boolean]

   // given Exporter[Char] with
   //    def exportValue(a: Char) = allocatingSerialize(a)

   given Exporter[Byte] = derive[Byte]

   given [A](using Encoder[A], NativeInfo[A]): Exporter[Array[A]] with
      def exportValue(a: Array[A]) =
         val address = segAlloc.allocate(layoutOf[A].byteSize * a.size).address
         encode(a, address, 0)
         address

   inline given fnExporter[A](using Fn[A]): Exporter[A] = ${
      fnExporterImpl[A]
   }

   private val paramNames =
      LazyList.iterate('a', 24)(c => (c.toInt + 1).toChar).map(_.toString)

   private def fnExporterImpl[A](using Quotes, Type[A]): Expr[Exporter[A]] =
      import quotes.reflect.*

      '{
         given Fn[A] = ${ Expr.summonOrError[Fn[A]] }
         new Exporter[A]:
            val typeName = ${ Expr(TypeRepr.of[A].typeSymbol.name) }
            val methodType = MethodHandleMacros.methodTypeForFn[A]

            val functionDescriptor =
               MethodHandleMacros.functionDescriptorForFn[A]

            def exportValue(a: A): Scopee[Allocatee[MemoryAddress]] = ${
               val aTerm = 'a.asTerm
               val (nTypeRepr, inputTypes, retType) = TypeRepr.of[A] match
                  case AppliedType(oTypeRepr, args) =>
                     val types = args.map(_.asType)
                     (
                       oTypeRepr.appliedTo(args.map(_ => TypeRepr.of[Any])),
                       types.init,
                       types.last
                     )

               val wrappedLambda = nTypeRepr.asType.pipe {
                  case '[nLambdaType] =>
                     Lambda(
                       Symbol.spliceOwner,
                       MethodType(paramNames.take(inputTypes.size).toList)(
                         _ => inputTypes.map(_ => TypeRepr.of[Any]),
                         _ => TypeRepr.of[Any]
                       ),
                       (_, params) =>
                          params
                             .map(_.asExpr)
                             .zip(inputTypes)
                             .map { case (p, '[a]) =>
                                val immigrator =
                                   Expr.summonOrError[Immigrator[a]]
                                '{ $immigrator($p) }.asTerm
                             }
                             .pipe(terms =>
                                retType.pipe { case '[r] =>
                                   val symbol = TypeRepr
                                      .of[A]
                                      .typeSymbol
                                      .declaredMethod("apply")
                                      .head
                                   val selected = Select(aTerm, symbol)
                                   val expr = Apply(selected, terms).asExprOf[r]
                                   val emigrator =
                                      Expr.summonOrError[Emigrator[r]]
                                   '{ $emigrator($expr) }.asTerm
                                }
                             )
                     ).asExprOf[nLambdaType]
               }

               val classRepr = nTypeRepr.asType.pipe { case '[nLambdaType] =>
                  val classTag = Expr.summonOrError[ClassTag[nLambdaType]]
                  '{ $classTag.runtimeClass }
               }
               '{
                  val lambdaMh: MethodHandle = MethodHandles.lookup
                     .findVirtual(
                       $classRepr,
                       "apply",
                       MT.genericMethodType(${ Expr(inputTypes.size) })
                     )
                     .bindTo($wrappedLambda)
                     .asType(methodType)

                  Linker.linker.upcallStub(
                    lambdaMh,
                    functionDescriptor,
                    currentScope
                  )
               }
            }
      }
