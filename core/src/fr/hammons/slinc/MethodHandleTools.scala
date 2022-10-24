package fr.hammons.slinc

import scala.quoted.*
import java.lang.invoke.MethodHandle

object MethodHandleTools:
  def exprNameMapping(expr: Expr[Any])(using Quotes): String =
    if expr.isExprOf[Int] then "I"
    else if expr.isExprOf[Short] then "S"
    else if expr.isExprOf[Long] then "L"
    else if expr.isExprOf[Byte] then "B"
    else if expr.isExprOf[Double] then "D"
    else if expr.isExprOf[Float] then "F"
    else "O"

  def returnMapping[R](using Quotes, Type[R]) =
    Type.of[R] match
      case '[Int]    => "I"
      case '[Short]  => "S"
      case '[Long]   => "L"
      case '[Byte]   => "B"
      case '[Double] => "D"
      case '[Float]  => "F"
      case '[Unit]   => "O"
      case '[Object] => "O"


  def invokeVariadicArguments(
    mhGen: Expr[Seq[DataLayout] => MethodHandle],
    exprs: Expr[Seq[Any]],
    varArgDescriptors: Expr[Seq[DataLayout]]
  )(using Quotes) =
    '{
      MethodHandleFacade.callVariadic(
        $mhGen($varArgDescriptors),
        $exprs*
      )
    }


  def invokeArguments[R](
      mh: Expr[MethodHandle],
      exprs: Seq[Expr[Any]],
  )(using
      Quotes,
      Type[R]
  ) =
    import quotes.reflect.*

      val arity = exprs.size
      val callName = (exprs.map(exprNameMapping) :+ returnMapping[R]).mkString

      val mod = Symbol
        .requiredPackage("fr.hammons.slinc")
        .declarations
        .find(_.name == s"MethodHandleArity$arity")
        .map(_.companionModule)

      val backupMod = TypeRepr
        .of[MethodHandleFacade]
        .classSymbol
        .getOrElse(report.errorAndAbort("This class should exist!!"))
        .companionModule

      val methodSymbol = mod.flatMap(
        _.declaredMethods
          .find(_.name == callName)
      )

      methodSymbol.foreach(println)

      val backupSymbol =
        backupMod.declaredMethods.find(_.name.endsWith(arity.toString()))

      val call = methodSymbol
        .map(ms =>
          Apply(
            Select(Ident(mod.get.termRef), ms),
            mh.asTerm :: exprs.map(_.asTerm).toList
          ).asExpr
        )
        .orElse(
          backupSymbol.map(ms =>
            Apply(
              Select(Ident(backupMod.termRef), ms),
              mh.asTerm :: exprs.map(_.asTerm).toList
            ).asExpr
          )
        )
        .getOrElse(
          '{ MethodHandleFacade.callVariadic($mh, ${ Varargs(exprs) }*) }
        )

      val expr = call
      report.info(expr.show)
      expr

  inline def getVariadicContext(s: Seq[Variadic]) =
    s.map(_.use[LayoutOf](l ?=> _ => l.layout))

  inline def getVariadicExprs(s: Seq[Variadic]) = (alloc: Allocator) ?=>
    s.map(
      _.use[NativeInCompatible](nic ?=>
        d =>
          nic match
            case i: InAllocatingTransitionNeeded[?] => i.in(d)
            case i: InTransitionNeeded[?]           => i.in(d)
            case i: NativeInCompatible[d.type]           => 
              val res = d.asInstanceOf[Any]
              res
      )
    )

  def calculateMethodHandleImplementation[L](
      platformExpr: Expr[LibraryI.PlatformSpecific],
      addresses: Expr[IArray[Object]]
  )(using Quotes, Type[L]) =
    import quotes.reflect.*

    val methodSymbols = MacroHelpers.getMethodSymbols(
      TypeRepr
        .of[L]
        .classSymbol
        .getOrElse(
          report.errorAndAbort(
            s"Can't calculate methodhandles from type ${Type.show[L]}"
          )
        )
    )

    val methodHandles = methodSymbols
      .map(
        Descriptor.fromDefDef
      )
      .zipWithIndex
      .map { case (descriptor, addressIdx) =>
        '{
          $platformExpr
            .getDowncall(
              $addresses(${ Expr(addressIdx) }),
              $descriptor
            )
            .nn
        }
      }

    val varMethodHandleGens = methodSymbols
      .map(
        Descriptor.fromDefDef
      )
      .zipWithIndex
      .map((descriptor, addressIdx) =>
        '{ (varargsDesc: Seq[DataLayout]) =>
          $platformExpr
            .getDowncall(
              $addresses(${ Expr(addressIdx) }),
              $descriptor
                .addVarargs(varargsDesc*)
            )
            .nn
        }
      )

    '{
      (
        IArray(${ Varargs(methodHandles) }*),
        IArray(${ Varargs(varMethodHandleGens) }*)
      )
    }

  inline def calculateMethodHandles[L](
      platformSpecific: LibraryI.PlatformSpecific,
      addresses: IArray[Object]
  ) = ${
    calculateMethodHandleImplementation[L]('platformSpecific, 'addresses)
  }

  inline def wrappedMH[A](methodHandle: MethodHandle) = ${
    wrappedMHImpl[A]('methodHandle)
  }

  private def wrappedMHImpl[A](
      methodHandleExpr: Expr[MethodHandle]
  )(using Quotes, Type[A]) =
    import quotes.reflect.*

    val (inputTypes, retType) = TypeRepr.of[A] match
      case AppliedType(_, args) =>
        (args.init, args.last)
      case _ => report.errorAndAbort(TypeRepr.of[A].show)

    val paramNames = LazyList.iterate("a")(a => a ++ a)

    Lambda(
      Symbol.spliceOwner,
      MethodType(paramNames.take(inputTypes.size).toList)(
        _ => inputTypes,
        _ => retType
      ),
      (meth, params) =>
        retType.asType match
          case '[r] =>
            invokeArguments[r](
              methodHandleExpr,
              params.map(_.asExpr),
            ).asTerm
              .changeOwner(meth)
    ).asExprOf[A]
