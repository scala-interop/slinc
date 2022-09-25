package fr.hammons.slinc

import java.lang.invoke.MethodHandle
import scala.quoted.*

trait Libraryo[L]:
  val handles: IArray[MethodHandle]
  val addresses: IArray[Mem]

object Libraryo:
  inline def derived[L]: Libraryo[L] = ${ derivedImpl[L] }

  def getReturnType(using q: Quotes)(s: quotes.reflect.Symbol) =
    import quotes.reflect.*
    if s.isDefDef then
      s.typeRef.translucentSuperType match
        case TypeLambda(_, _, ret: LambdaType) => ret.resType
        case ret: LambdaType                   => ret.resType
    else report.errorAndAbort("This symbol isn't a method!")

  def needsAllocator(using q: Quotes)(s: q.reflect.Symbol): Boolean =
    import quotes.reflect.*

    getReturnType(s).asType match
      case '[r & Product] =>
        Expr.summon[InAllocatingTransitionNeeded[r & Product]].isDefined
      case _ => false

  type PossiblyNeedsAllocator = (Expr[Allocator] => Expr[Object]) | Expr[Object]
  def handleInput(
      expr: Expr[Any]
  )(using q: Quotes): (Expr[Allocator] => Expr[Object]) | Expr[Object] =
    import quotes.reflect.*
    expr match
      case '{ $exp: a } =>
        Expr
          .summon[InAllocatingTransitionNeeded[a]]
          .map(f =>
            (
                (alloc: Expr[Allocator]) => '{ $f.in($exp)(using $alloc) }
            ): PossiblyNeedsAllocator
          )
          .orElse(
            Expr
              .summon[InTransitionNeeded[a]]
              .map(f =>
                '{ $f.in(${ expr.asExprOf[a] }) }: PossiblyNeedsAllocator
              )
          )
          .getOrElse('{ $expr.asInstanceOf[Object] }: PossiblyNeedsAllocator)

  def handleOutput[R](
      expr: Expr[Object | Null]
  )(using Quotes, Type[R]): Expr[R] =
    Expr
      .summon[OutTransitionNeeded[R]]
      .map(fn => '{ $fn.out($expr.nn) })
      .getOrElse(Type.of[R] match {
        case '[Unit] => '{ $expr; () }.asExprOf[R]
        case _       => '{ $expr.asInstanceOf[R] }
      })

  def checkMethodIsCompatible(using q: Quotes)(s: q.reflect.Symbol): Unit =
    import quotes.reflect.*

    s.tree match
      case DefDef(name, paramClauses, returnType, _) =>
        if paramClauses.size <= 2 then () else report.errorAndAbort("")

        paramClauses.zipWithIndex.foreach {
          case (TypeParamClause(typDefs), 0) =>
            typDefs.map(_.rhs).foreach {
              case TypeBoundsTree(Inferred(), Inferred()) => ()
              case TypeBoundsTree(_, _) =>
                report.errorAndAbort(
                  "Type bounds aren't supported on method bindings"
                )
            }
          case (TermParamClause(valDefs), i) if i == paramClauses.size - 1 =>
            valDefs.foreach { vd =>
              vd.tpt.tpe.asType match
                case '[a] =>
                  Expr
                    .summon[NativeInCompatible[a]]
                    .map(_ => ())
                    .getOrElse(
                      report.errorAndAbort(
                        s"Type ${Type.show[a]} isn't compatible with native bindings."
                      )
                    )
            }
          case _ =>
            report.errorAndAbort("Method bindings cannot be curried")

            returnType.tpe.asType.match {
              case '[Unit] => ()
              case '[a] =>
                Expr
                  .summon[NativeOutCompatible[a]]
                  .map(_ => ())
                  .getOrElse(
                    report.errorAndAbort(
                      s"Return type ${Type.show[a]} isn't compatible with native bindings."
                    )
                  )
            }

        }

  inline def binding[R]: R = ${
    bindingImpl[R]
  }

  def findOwningClass(using q: Quotes)(s: q.reflect.Symbol): q.reflect.Symbol =
    if s.isClassDef then s
    else findOwningClass(s.owner)

  def findOwningMethod(using q: Quotes)(s: q.reflect.Symbol): q.reflect.Symbol =
    if s.isDefDef then s
    else findOwningMethod(s.owner)

  def bindingWithinLibrary(using Quotes) =
    import quotes.reflect.*
    val c = findOwningClass(Symbol.spliceOwner)

    c.typeRef.asType match
      case '[a] =>
        Expr.summon[Libraryo[a]] match
          case None =>
            report.errorAndAbort(
              s"Cannot use binding on a non-library class ${Type.show[a]}"
            )
          case _ => ()

  def getLibrary(using Quotes) =
    import quotes.reflect.*
    val c = findOwningClass(Symbol.spliceOwner)

    c.typeRef.asType match
      case '[a] =>
        Expr.summon[Libraryo[a]] match
          case None =>
            report.errorAndAbort(
              s"Cannot use binding on a non-library class ${Type.show[a]}"
            )
          case Some(exp) => exp

  def widenExpr(t: Expr[?])(using Quotes) =
    import quotes.reflect.*
    t match
      case '{ $a: a } =>
        TypeRepr.of[a].widen.asType match
          case '[b] => '{ ${ a.asExprOf[b] }: b }

  def makeAllTakeAlloc(
      exprs: List[PossiblyNeedsAllocator]
  ): List[Expr[Allocator] => Expr[Object]] =
    exprs.map {
      case nonFn: Expr[Object] => (_: Expr[Allocator]) => nonFn
      case fn: (Expr[Allocator] => Expr[Object]) => fn
    }

  def getInputsAndOutputType(using
      q: Quotes
  )(methodSymbol: q.reflect.Symbol) =
    import quotes.reflect.*
    methodSymbol.tree match
      case DefDef(_, params, ret, _) =>
        params.last match
          case TermParamClause(valDefs) =>
            val inputs =
              valDefs
                .map(t => widenExpr(Ref(t.symbol).asExpr))

            inputs -> ret.tpe.asType

  def bindingImpl[R](using Quotes, Type[R]): Expr[R] =
    import quotes.reflect.*
    val library = getLibrary
    val owningClass = findOwningClass(Symbol.spliceOwner)

    val methodSymbol = findOwningMethod(Symbol.spliceOwner)
    val methodPositionExpr = owningClass.declaredMethods.zipWithIndex
      .find((s, _) => methodSymbol == s)
      .map((_, i) => Expr(i))
      .getOrElse(
        report.errorAndAbort("Couldn't find method in declared methods?")
      )

    val methodHandle = '{ $library.handles($methodPositionExpr) }
    val address = '{ $library.addresses($methodPositionExpr) }
    checkMethodIsCompatible(methodSymbol)

    val prefix: List[PossiblyNeedsAllocator] =
      if needsAllocator(methodSymbol) then
        List(
          address,
          (a: Expr[Allocator]) => a
        )
      else List(address)

    val inputs =
      prefix ++ getInputsAndOutputType(methodSymbol)._1.map(handleInput)

    val allocationlessInputs = inputs.collect { case e: Expr[Object] =>
      e
    }

    val allocInputs = makeAllTakeAlloc(inputs)

    val code: Expr[R] =
      if allocationlessInputs == inputs && !needsAllocator(methodSymbol) then
        val invokation =
          MethodHandleTools.invokeArguments(
            methodHandle,
            allocationlessInputs*
          )
        handleOutput[R](invokation).asExprOf[R]
      else
        val methodInvoke =
          (a: Expr[Allocator]) =>
            MethodHandleTools.invokeArguments(
              methodHandle,
              allocInputs.map(_(a))*
            )

        val scopeThing = Expr
          .summon[TempScope]
          .getOrElse(report.errorAndAbort("need temp allocator in scope"))
        val subCode = '{
          given TempScope = $scopeThing
          Scope.temp((a: Allocator) ?=>
            ${ handleOutput[R](methodInvoke('{ a })) }
          )
        }
        subCode

    report.warning(code.show)
    code

  // report.info("la")
  private def derivedImpl[L](using Quotes, Type[L]): Expr[Libraryo[L]] =
    import quotes.reflect.*

    val symbol = TypeRepr.of[L].classSymbol.getOrElse(???)
    val methodSymbols = symbol.declaredMethods
    val downcalls = methodSymbols
      .map(getInputsAndOutputType(_))
      .map((is, o) =>
        Expr.ofTupleFromSeq(is) match
          case '{ $tup: (t & Tuple) } =>
            o match
              case '[r] =>
                Expr
                  .summon[Downcall[t & Tuple, r]]
                  .getOrElse(
                    report.errorAndAbort(s"Can't calculate Downcall from ${Type
                        .show[t & Tuple]} with return ${Type.show[r]}")
                  )
      )
      .map(dc =>
        '{
          $dc.mh
        }
      )

    val code = '{
      new Libraryo[L]:
        val handles = IArray(${ Varargs(downcalls) }*)
        val addresses = IArray.empty
    }

    report.info(code.show)
    code
