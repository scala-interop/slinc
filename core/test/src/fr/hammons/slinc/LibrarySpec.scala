package fr.hammons.slinc

import scala.annotation.experimental

@experimental
object X {
  val sl: Slinc = ???
  import sl.{given, *}

  case class X(a: Int) derives Struct

  class MyLibX derives Library:
    def myMethod(a: Ptr[Any], b: Int): X = Library.binding
    def myOtherMethod(a: Int, b: Int): Int = Library.binding

// trait MyLib derives LibraryExp:
//   def myMethod(a: Ptr[Any], b: Int): X
}

// Inlined(
//   Some(TypeIdent("Library$")),
//   Nil,
//   Block(
//     List(
//       ClassDef(
//         "$anon",
//         DefDef(
//           "<init>",
//           List(TermParamClause(Nil)),
//           Inferred(),
//           None
//         ),
//         List(
//           Apply(
//             Select(
//               New(
//                 Inferred()
//               ), "<init>"),
//             Nil
//           ),
//           Applied(
//             TypeIdent("Library"),
//             List(Inferred())
//           )
//         ),
//       None,
//       List(
//         ValDef(
//           "instance",
//           Inferred(),
//           Some(Ident("???"))
//         )
//       )
//     )
//   ),
//   Typed(
//     Apply(
//       Select(
//         New(TypeIdent("$anon")),
//         "<init>"
//       ),
//       Nil
//     ),
//     Inferred()
//   )
//   )
// )
