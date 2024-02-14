package fr.hammons.slinc

import compiletime.{erasedValue, summonInline, summonFrom}
import fr.hammons.slinc.internal.ast.StackAlloc
import fr.hammons.slinc.internal.IsValidStruct
import fr.hammons.slinc.internal.ast.Expression
import annotation.switch

import quoted.*
import scala.util.Using

inline def stackAlloc[A](using p: Platform): A = ${
  ???
}

// private def stackAllocImpl[A](using Quotes, Type[A]): Expr[A] =
//   import quotes.reflect.*

//   def handleRefinement(count: Int, repr: TypeRepr): Seq[CaseDef] =
//     repr match
//       case Refinement(parent, name, repr) =>
//         if repr.
//         CaseDef(
//           Expr(name).asTerm,
//           None,
//           Expr(count).asTerm
//         ) +: handleRefinement(count, repr)
//   val expr =

//   report.errorAndAbort(expr.asTerm.show(using Printer.TreeStructure))

def find(s: String) = (s: @switch) match
  case "la" => 5

type Identity[A] = A
