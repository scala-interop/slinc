package fr.hammons.slinc

import scala.quoted.*
import scala.annotation.experimental
import scala.reflect.ClassTag
import scala.annotation.nowarn

trait LibraryExp[L]:
  val instance: L

@experimental
object LibraryExp:
  inline def derived[L](using ct: ClassTag[L]) = ${
    derivedImpl[L]('ct)
  }

  @nowarn
  def firstSectionIsGeneric(using q: Quotes)(
      list: List[q.reflect.Symbol]
  ): Boolean =
    import quotes.reflect.*
    list.map(_.typeRef.qualifier).forall {
      case NoPrefix() => true; case _ => false
    }

  def checkBounds(using
      q: Quotes
  )(list: List[q.reflect.Symbol], p: q.reflect.PolyType) =
    import quotes.reflect.*
    list.zipWithIndex.forall((s, i) => s.typeRef <:< p.param(i))

  def overridingSymbol(using
      q: Quotes
  )(s: q.reflect.Symbol, cls: q.reflect.Symbol) =
    import quotes.reflect.*
    if s.isDefDef then
      LibraryI.checkMethodIsCompatible(s)
      Symbol.newMethod(
        cls,
        s.name,
        if s.paramSymss.size == 1 then
          generateMethodType(
            Nil,
            Nil,
            s.paramSymss(0),
            LibraryI.getReturnType(s)
          )
        else if s.paramSymss.size == 2 then
          generatePolyType(s.paramSymss, LibraryI.getReturnType(s))
        else report.errorAndAbort("Too many clauses!!")
      )
    else report.errorAndAbort("Cannot override this symbol, it's not a method.")

  def generatePolyType(using
      q: Quotes
  )(params: List[List[q.reflect.Symbol]], ret: q.reflect.TypeRepr) =
    import quotes.reflect.*
    PolyType(params(0).map(_.name))(
      _ => params(0).map(_ => TypeBounds.empty),
      polyType =>
        generateMethodType(
          params(0),
          (0 until params(0).size).map(polyType.param(_)).toList,
          params(1),
          ret
        )
    )

  def generateMethodType(using q: Quotes)(
      replacementsFrom: List[q.reflect.Symbol],
      replacementsTo: List[q.reflect.TypeRepr],
      params: List[q.reflect.Symbol],
      ret: q.reflect.TypeRepr
  ) =
    import quotes.reflect.*
    MethodType(params.map(_.name))(
      _ =>
        params.map(
          _.typeRef.translucentSuperType
            .substituteTypes(replacementsFrom, replacementsTo)
        ),
      _ => ret.substituteTypes(replacementsFrom, replacementsTo)
    )

  def derivedImpl[L](expr: Expr[ClassTag[L]])(using Quotes, Type[L]) =
    import quotes.reflect.*
    val symbol = TypeRepr.of[L].classSymbol.getOrElse(???)

    val methods = symbol.declaredMethods

    val name: String = "$anon"
    val parents = List(TypeTree.of[Object], TypeTree.of[L])
    def decls(cls: Symbol): List[Symbol] =
      methods.map(overridingSymbol(_, cls))
    // methods.map(s => Symbol.newMethod(cls, s.name,
    // PolyType(List("A"))(_ => List(TypeBounds.empty), t => MethodType.apply(List("a","b"))(_ => List(s.paramSymss(1)(0).typeRef.translucentSuperType.substituteTypes(List(s.paramSymss(0)(1)), List(t.param(0))), TypeRepr.of[Int]), _ => TypeRepr.of[Unit]))))
    val cls = Symbol.newClass(
      Symbol.spliceOwner,
      name,
      parents = parents.map(_.tpe),
      decls,
      selfType = None
    )
    val fooSymb = cls.declaredMethods.head
    val fooDef = DefDef(fooSymb, argss => Some('{ println("dummy") }.asTerm))
    val clsDef = ClassDef(cls, parents, body = List(fooDef))
    val newCls = Typed(
      Apply(Select(New(TypeIdent(cls)), cls.primaryConstructor), Nil),
      TypeTree.of[L]
    )
    val block = Block(List(clsDef), newCls).asExprOf[L]

    '{
      new LibraryExp[L]:
        val instance = $block
    }

