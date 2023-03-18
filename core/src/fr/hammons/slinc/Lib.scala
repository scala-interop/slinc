package fr.hammons.slinc

import scala.quoted.*
import fr.hammons.slinc.modules.LibModule
import java.util.concurrent.atomic.AtomicStampedReference
import scala.annotation.nowarn

import fr.hammons.slinc.CFunctionDescriptor
trait Lib[L]:
  val description: List[CFunctionDescriptor]
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

    val names = repr.classSymbol.get.declaredMethods.map: method =>
      Expr(method.name)

    val descriptors = Expr.ofList(
      names.map: methodNameExpr =>
        '{
          CFunctionDescriptor[L]($methodNameExpr)
        }
    )

    val generators = Expr.ofList(
      names.map: methodNameExpr =>
        '{
          CFunctionBindingGenerator[L](
            $methodNameExpr
          )
        }
    )

    '{
      new Lib[L]:
        val description = $descriptors
        val generation = $generators
    }

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
