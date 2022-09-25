package fr.hammons.slinc

import scala.quoted.*
import java.lang.invoke.MethodHandle
import fr.hammons.slinc.LibraryI.PlatformSpecific

class LibraryI(platformSpecific: PlatformSpecific):
  trait Library[L]:
    val handles: IArray[MethodHandle]
    val addresses: IArray[Object]

  object Library:
    inline def derived[L]: Library[L] = new Library[L]:
      val lookup = LibraryI.getLookup[L](platformSpecific)
      val addresses = LibraryI.getMethodAddress[L](lookup)
      val handles = LibraryI.calculateMethodHandles[L](platformSpecific, addresses)


    inline def binding[R]: R = ${
      LibraryI.bindingImpl[R,Library]
    }



object LibraryI:
  trait PlatformSpecific:
    def getDowncall(
        address: Object,
        layout: Seq[DataLayout],
        ret: Option[DataLayout]
    ): MethodHandle

    def getDowncall(
      layout: Seq[DataLayout],
      ret: Option[DataLayout]
    ): MethodHandle

    def getLookup(name: Option[String]): Lookup

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

  type PossiblyNeedsAllocator = (Expr[Allocator => Object]) | Expr[Object]
  def handleInput(
      expr: Expr[Any]
  )(using q: Quotes): (Expr[Allocator => Object]) | Expr[Object] =
    import quotes.reflect.*
    expr match
      case '{ $exp: a } =>
        Expr
          .summon[InAllocatingTransitionNeeded[a]]
          .map(f =>
            (
                '{ (alloc: Allocator) =>  $f.in($exp)(using alloc) }
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
      .map(fn => '{ $fn.out($expr.asInstanceOf[Object]) })
      .getOrElse(Type.of[R] match {
        case '[Unit] => '{ $expr; () }.asExprOf[R]
        case _       => '{ $expr.asInstanceOf[R] }
      })


  def makeAllTakeAlloc(
      exprs: List[PossiblyNeedsAllocator]
  )(using Quotes): List[Expr[Allocator => Object]] =
    
    exprs.map { exp =>
      if exp.isExprOf[Allocator => Object] then 
        exp.asExprOf[Allocator => Object] 
      else 
        '{(_: Allocator) => ${exp.asExprOf[Object]}}
    }


  def bindingImpl[R, L[_] <: LibraryI#Library[_]](using Quotes, Type[R], Type[L]) = 
    import quotes.reflect.*
    val library = LibraryI.getLibrary[L]

    val owningClass = MacroHelpers.findOwningClass(Symbol.spliceOwner)

    val methodSymbol = MacroHelpers.findOwningMethod(Symbol.spliceOwner)
    val methodPositionExpr = getMethodSymbols(owningClass).zipWithIndex
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
        val summonAlloc = Expr.summon[InTransitionNeeded[Allocator]].getOrElse(???)
        List(
          '{(a: Allocator) => $summonAlloc.in(a)}
        )
      else Nil

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
          '{(a: Allocator) => 
            ${MethodHandleTools.invokeArguments(
              methodHandle,
              allocInputs.map(exp => '{$exp(a)}).map(Expr.betaReduce)*
            )}
          }

        val scopeThing = Expr
          .summon[TempScope]
          .getOrElse(report.errorAndAbort("need temp allocator in scope"))
        val subCode = '{
          given TempScope = $scopeThing
          Scope.temp((a: Allocator) ?=>
            val res = ${Expr.betaReduce('{$methodInvoke(a)})} 
            ${handleOutput[R]('res)}
          )
        }
        subCode

    report.warning(code.asTerm.show(using Printer.TreeShortCode))
    code
  
  def getLibrary[L[_]](using Quotes, Type[L]): Expr[L[Any]] = 
    import quotes.reflect.* 
    val c = MacroHelpers.findOwningClass(Symbol.spliceOwner)

    TypeRepr.of[L].appliedTo(c.typeRef).asType match 
      case '[l] => Expr.summon[l] match 
        case None => 
          report.errorAndAbort(
            s"Cannot find library ${Type.show[l]}"
          )
        case Some(exp) => '{$exp.asInstanceOf[L[Any]]}

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
                .map(t => MacroHelpers.widenExpr(Ref(t.symbol).asExpr))

            inputs -> ret.tpe.asType

  inline def calculateMethodHandles[L](platformSpecific: PlatformSpecific, addresses: IArray[Object]) = ${
    calculateMethodHandleImplementation[L]('platformSpecific, 'addresses)
  }

  inline def getLookup[L](platformSpecific: PlatformSpecific): Lookup = ${
    getLookupImpl[L]('platformSpecific)
  }

  def getLookupImpl[L](
      platformSpecificExpr: Expr[PlatformSpecific]
  )(using Quotes, Type[L]) =
    import quotes.reflect.*
    val name = LibraryName.libraryName[L]
    '{ $platformSpecificExpr.getLookup(${ Expr(name) }) }

  inline def getMethodAddress[L](l: Lookup) = ${
    getMethodAddressImpl[L]('l)
  }

  def getMethodAddressImpl[L](l: Expr[Lookup])(using Quotes, Type[L]) =
    import quotes.reflect.*


    val methodList =
      getMethodSymbols(MacroHelpers.getClassSymbol[L]).map(s => Expr(s.name))

    report.info(Type.show[L])

    val addresses = methodList.map(method =>
      '{
        val s = $method
        println(s)
        $l.lookup(s)
          .getOrElse(throw Error(s"Can't find method ${s}"))
      }
    )

    '{ IArray(${ Varargs(addresses) }*) }

  def getLayoutFor[A](using Quotes, Type[A]) =
    import quotes.reflect.*
    val expr = Expr
      .summon[LayoutOf[A]]
      .getOrElse(
        report.errorAndAbort(s"Cannot find a layout of ${Type.show[A]}")
      )

    '{ $expr.layout }

  def getMethodSymbols(using q: Quotes)(s: q.reflect.Symbol) = 
    s.declaredMethods.filter(_.name != "writeReplace")
  def calculateMethodHandleImplementation[L](
      platformExpr: Expr[PlatformSpecific],
      addresses: Expr[IArray[Object]]
  )(using Quotes, Type[L]) =
    import quotes.reflect.*

    val methodSymbols = getMethodSymbols(TypeRepr
      .of[L]
      .classSymbol
      .getOrElse(
        report.errorAndAbort(
          s"Can't calculate methodhandles from type ${Type.show[L]}"
        )
      ))

    val exprs = methodSymbols
      .map(
        getInputsAndOutputType(_)
      ).zipWithIndex
      .map { case ((is, o), addressIdx) =>
        val inputs = Expr.ofSeq(is.map { case '{ ${ _ }: a } =>
          getLayoutFor[a]
        })
        val oLayout = o match
          case '[Unit] => '{ None }
          case '[o] =>
            val layout = getLayoutFor[o]
            '{ Some($layout) }

        '{ $platformExpr.getDowncall($inputs, $oLayout).bindTo($addresses(${Expr(addressIdx)})).nn }
      }

    '{ IArray(${ Varargs(exprs) }*) }