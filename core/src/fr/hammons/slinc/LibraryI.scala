package fr.hammons.slinc

import scala.quoted.*
import java.lang.invoke.MethodHandle
import NativeInCompatible.PossiblyNeedsAllocator
import scala.annotation.nowarn

class LibraryI(platformSpecific: LibraryI.PlatformSpecific):
  trait Library[+L]:
    val handles: IArray[MethodHandle]
    val varGens: IArray[Seq[DataLayout] => MethodHandle]
    val addresses: IArray[Object]

  object Library:
    inline def derived[L]: Library[L] = new Library[L]:
      val lookup = LibraryI.getLookup[L](platformSpecific)
      val addresses = LibraryI.getMethodAddress[L](lookup)
      val (handles, varGens) =
        MethodHandleTools.calculateMethodHandles[L](platformSpecific, addresses)

    inline def binding[R]: R =
      ${
        LibraryI.bindingImpl[R, Library]
      }

object LibraryI:
  trait PlatformSpecific(layoutI: LayoutI):
    def getDowncall(
        address: Object,
        descriptor: Descriptor
    ): MethodHandle

    def getLocalLookup(name: String): Lookup
    def getLibraryPathLookup(name: String): Lookup
    def getStandardLibLookup: Lookup

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
                case '[Seq[Variadic]] =>
                  ()
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

  @nowarn
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

  def makeAllTakeAlloc(
      exprs: List[PossiblyNeedsAllocator]
  )(using Quotes): List[Expr[Allocator => Any]] =
    exprs.map { exp =>
      if exp.isExprOf[Allocator => Any] then exp.asExprOf[Allocator => Any]
      else '{ (_: Allocator) => $exp }
    }

  def bindingImpl[R, L[_] <: LibraryI#Library[?]](using
      Quotes,
      Type[R],
      Type[L]
  ) =
    import quotes.reflect.*
    val library = LibraryI.getLibrary[L]

    val owningClass = MacroHelpers.findOwningClass(Symbol.spliceOwner)

    val methodSymbol = MacroHelpers.findOwningMethod(Symbol.spliceOwner)
    val methodPositionExpr = MacroHelpers
      .getMethodSymbols(owningClass)
      .zipWithIndex
      .find((s, _) => methodSymbol == s)
      .map((_, i) => Expr(i))
      .getOrElse(
        report.errorAndAbort("Couldn't find method in declared methods?")
      )

    val methodHandle = '{ $library.handles($methodPositionExpr) }
    val methodHandleGen = '{ $library.varGens($methodPositionExpr) }
    val address = '{ $library.addresses($methodPositionExpr) }
    checkMethodIsCompatible(methodSymbol)

    val prefix: List[PossiblyNeedsAllocator] =
      if needsAllocator(methodSymbol) then
        val summonAlloc =
          Expr.summon[InTransitionNeeded[Allocator]].getOrElse(???)
        List(
          '{ (a: Allocator) => $summonAlloc.in(a) }
        )
      else Nil

    val inputs =
      prefix ++ MacroHelpers
        .getInputsAndOutputType(methodSymbol)
        ._1

    val mappedInputs = inputs
      .map(NativeInCompatible.handleInput)
      .filter(!_.isExprOf[Seq[Variadic]])

    val allocationlessInputs =
      mappedInputs.filter(_.isExprOf[Object]).map(_.asExprOf[Object])

    val allocInputs = makeAllTakeAlloc(mappedInputs)

    val code: Expr[R] =
      if inputs.size == mappedInputs.size then

        if allocationlessInputs == mappedInputs && !needsAllocator(methodSymbol)
        then
          val invokation =
            MethodHandleTools.invokeArguments[R](
              methodHandle,
              allocationlessInputs
            )
          NativeOutCompatible.handleOutput[R](invokation).asExprOf[R]
        else
          val methodInvoke =
            '{ (a: Allocator) =>
              ${
                MethodHandleTools.invokeArguments[R](
                  methodHandle,
                  allocInputs.map(exp => '{ $exp(a) }).map(Expr.betaReduce)
                )
              }
            }

          val scopeThing = Expr
            .summon[TempScope]
            .getOrElse(report.errorAndAbort("need temp allocator in scope"))
          val subCode = '{
            Scope.temp(using $scopeThing)((a: Allocator) ?=>
              ${
                NativeOutCompatible.handleOutput[R](Expr.betaReduce('{
                  $methodInvoke(a)
                }))
              }
            )
          }
          subCode
      else
        val varargs = inputs.last.asExprOf[Seq[Variadic]]
        val scopeThing = Expr
          .summon[TempScope]
          .getOrElse(report.errorAndAbort("need temp allocator in scope"))
        '{
          Scope.temp(using $scopeThing)((a: Allocator) ?=>
            ${
              val normalInputs = Expr.ofSeq(allocInputs.map(e => '{ $e(a) }))
              val totalInputs = '{
                $normalInputs ++ MethodHandleTools.getVariadicExprs($varargs)
              }
              NativeOutCompatible.handleOutput[R](
                MethodHandleTools.invokeVariadicArguments(
                  methodHandleGen,
                  totalInputs,
                  '{ MethodHandleTools.getVariadicContext($varargs) }
                )
              )
            }
          )
        }
    report.warning(code.asTerm.show(using Printer.TreeShortCode))
    code

  def getLibrary[L[_]](using Quotes, Type[L]): Expr[L[Any]] =
    import quotes.reflect.*
    val c = MacroHelpers.findOwningClass(Symbol.spliceOwner)

    TypeRepr.of[L].appliedTo(c.typeRef).asType match
      case '[l] =>
        Expr.summon[l] match
          case None =>
            report.errorAndAbort(
              s"Cannot find library ${Type.show[l]}"
            )
          case Some(exp) => exp.asExprOf[L[Any]]

  inline def getLookup[L](platformSpecific: PlatformSpecific): Lookup = ${
    getLookupImpl[L]('platformSpecific)
  }

  def getLookupImpl[L](
      platformSpecificExpr: Expr[PlatformSpecific]
  )(using Quotes, Type[L]) =
    import quotes.reflect.*
    val name: Option[LibraryLocation] = LibraryName.libraryName[L]
    name match
      case None => '{ $platformSpecificExpr.getStandardLibLookup }
      case Some(LibraryLocation.Local(s)) =>
        '{ $platformSpecificExpr.getLocalLookup(${ Expr(s) }) }
      case Some(LibraryLocation.Path(s)) =>
        '{ $platformSpecificExpr.getLibraryPathLookup(${ Expr(s) }) }
      case Some(LibraryLocation.Resource(s)) =>
        val sExpr = Expr(s)
        val clPath = '{
          Tools.sendResourceToCache($sExpr)
          Tools.compileCachedResourceIfNeeded($sExpr).toAbsolutePath().nn.toString()
        }
        '{ $platformSpecificExpr.getLocalLookup($clPath ) }

  inline def getMethodAddress[L](l: Lookup) = ${
    getMethodAddressImpl[L]('l)
  }

  def getMethodAddressImpl[L](l: Expr[Lookup])(using Quotes, Type[L]) =
    import quotes.reflect.*

    val methodList =
      MacroHelpers
        .getMethodSymbols(MacroHelpers.getClassSymbol[L])
        .map(s => Expr(s.name))

    report.info(Type.show[L])

    val addresses = methodList.map(method =>
      '{
        val s = $method
        $l.lookup(s)
        // .getOrElse(throw Error(s"Can't find method ${s}"))
      }
    )

    '{ IArray(${ Varargs(addresses) }*) }
