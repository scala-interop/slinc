package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{
   MemoryLayout,
   CLinker,
   SegmentAllocator,
   MemoryAddress
}
import scala.quoted.*
import scala.util.chaining.*
import scala.util.NotGiven
import scala.annotation.implicitNotFound

trait VariadicMechanisms:
   protected def variadicHandler[R](
       address: Expr[MemoryAddress],
       params: List[Expr[Any]],
       args: Expr[Seq[Any]]
   )(using Quotes, Type[R]) =
      import quotes.reflect.*
      val vParams = args match
         case Varargs(exprs) =>
            exprs
               .map(_.widen)
               .map { case '{ $v: a } =>
                  '{ Variadic[a](${ v }) }
               }
               .toList

      MethodHandleMacros.binding[R](address, params ++ vParams).asExprOf[R]
case class Variadic[A](a: A) extends AnyVal
object Variadic extends VariadicMechanisms:

   given [A](using orig: NativeInfo[A]): NativeInfo[Variadic[A]] with {
      val layout = CLinker.asVarArg(orig.layout)
      val carrierType = orig.carrierType
   }

   given [A](using orig: Emigrator[A]): Emigrator[Variadic[A]] =
      orig.contramap[Variadic[A]](_.a)

   transparent inline def variadicBind[R](inline args: Any*)(using
       @implicitNotFound(
         "You must provide a return type for variadicBind"
       ) n: NotGiven[R =:= Nothing]
   ) = ${
      variadicBindImpl[R]('args)
   }

   private def variadicBindImpl[R](
       args: Expr[Seq[Any]]
   )(using Quotes, Type[R]): Expr[Any] =
      import quotes.reflect.*

      val owner = Symbol.spliceOwner.owner

      val (name, params) =
         if owner.isDefDef then

            val defName = owner.name
            val params = args match
               case Varargs(exprs) =>
                  exprs.map { case '{ $e: a } =>
                     TypeRepr.of[a].widen.asType.pipe { case '[b] =>
                        e.asExprOf[b] -> TypeTree.of[b]
                     }
                  }.toList

            defName -> params
         else
            report.errorAndAbort(
              s"Naughty trying to variadic bind against something that's not a method!"
            )

      val arity = params.size
      val symbol = TypeRepr
         .of[VariadicCalls.type]
         .typeSymbol
         .declaredTypes
         .filter(_.name.endsWith(arity.toString))
         .head
      val tt = Applied(TypeIdent(symbol), params.map(_._2) :+ TypeTree.of[R])
      val address = '{
         ${ Expr.summonOrError[SymbolLookup] }.lookup(${ Expr(name) })
      }.asTerm
      Apply(
        TypeApply(
          Select(
            New(tt),
            symbol.primaryConstructor
          ),
          params.map(_._2) :+ TypeTree.of[R]
        ),
        address +: params.map(_._1.asTerm)
      ).asExpr

end Variadic
