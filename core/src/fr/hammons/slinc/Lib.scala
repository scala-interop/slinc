package fr.hammons.slinc

import scala.quoted.*
import fr.hammons.slinc.modules.LibModule
import java.util.concurrent.atomic.AtomicStampedReference
import scala.annotation.nowarn

trait Lib[L]:
  val description: List[(String, FunctionDescriptor)]
  val generation: List[CFunctionBindingGenerator]
  private var lib: AtomicStampedReference[LibBacking[L]] =
    AtomicStampedReference(null, 0)
  private val ver = Array(0)

  def get(using lm: LibModule): LibBacking[L] =
    var l = lib.get(ver)
    if ver(0) != lm.runtimeVersion || l == null then
      val old = l
      l = lm.getLibrary(description, generation).asInstanceOf[LibBacking[L]]
      lib.compareAndSet(old, l, ver(0), lm.runtimeVersion)
    l.asInstanceOf[LibBacking[L]]

object Lib:
  transparent inline def apply[A](using l: Lib[A], lm: LibModule) =
    ${ summonImpl[A]('l, 'lm) }

  inline def derived[L]: Lib[L] = ${ derivedImpl[L]() }

  @nowarn
  private def derivedImpl[L]()(using q: Quotes, t: Type[L]) =
    import quotes.reflect.*
    val repr = TypeRepr.of[L]

    val memberTypes =
      repr.classSymbol.get.declaredMethods.map:
        repr.memberType

    val names = repr.classSymbol.get.declaredMethods.map:
      _.name

    val functionsArgsAndReturns = names
      .zip(memberTypes)
      .map:
        case (_, MethodType(_, args, ret)) =>
          args -> ret

        case (name, t) =>
          report.errorAndAbort(s"Method $name has unsupported type ${t.show}")

    val functionArgumentAndReturnDescriptors = functionsArgsAndReturns.map:
      case (argTypes, returnType) =>
        argTypes.map(TypeDescriptor.fromTypeRepr) -> Option.unless(
          returnType =:= TypeRepr.of[Unit]
        )(TypeDescriptor.fromTypeRepr(returnType))

    val functionDescriptions =
      functionArgumentAndReturnDescriptors.map:
        case (args, ret) =>
          val retDesc = ret.map(exp => '{ Some($exp) }).getOrElse('{ None })
          '{ FunctionDescriptor(${ Expr.ofList(args) }, Nil, $retDesc) }

    val structure =
      names.map(Expr.apply).zip(functionDescriptions)

    val generators = Expr.ofList(
      names.map(methodName =>
        '{
          CFunctionBindingGenerator[L](
            ${ Expr(methodName) }
          )
        }
      )
    )

    '{
      new Lib[L]:
        val description = ${ Expr.ofList(structure.map(Expr.ofTuple)) }
        val generation = ${ generators }
    }

  @nowarn
  private def getTypeDescriptor(using q: Quotes)(
      typeRepr: q.reflect.TypeRepr
  ): Expr[TypeDescriptor] =
    import quotes.reflect.*

    val descOf = typeRepr.asType match
      case '[a] =>
        Expr
          .summon[DescriptorOf[a]]
          .getOrElse(report.errorAndAbort(s"No descriptor for ${Type.show[a]}"))

    '{ $descOf.descriptor }

  @nowarn
  private def summonImpl[A](
      library: Expr[Lib[A]],
      libraryModule: Expr[LibModule]
  )(using Quotes, Type[A]): Expr[Any] =
    import quotes.reflect.*
    val methodRepr = TypeRepr
      .of[A]
      .classSymbol
      .get
      .declaredMethods
      .map:
        TypeRepr.of[A].memberType

    val names = TypeRepr.of[A].classSymbol.get.declaredMethods.map(_.name)

    val refined = names
      .zip(methodRepr)
      .foldLeft(TypeRepr.of[LibBacking[A]]):
        case (backing, (name, repr)) =>
          Refinement(backing, name, repr)

    refined.asType match
      case '[a] =>
        '{
          $library.get(using $libraryModule).asInstanceOf[a]
        }
