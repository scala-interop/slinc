package fr.hammons.slinc

import scala.quoted.*
import fr.hammons.slinc.modules.FSetModule
import java.util.concurrent.atomic.AtomicStampedReference
import fr.hammons.slinc.fset.Dependency
import fr.hammons.slinc.annotations.NeedsResource
import fr.hammons.slinc.annotations.Needs
import fr.hammons.slinc.annotations.NeedsFile

import fr.hammons.slinc.fset.FSetBacking
trait FSet[L]:
  val dependencies: List[Dependency]
  val description: List[CFunctionDescriptor]
  val generation: List[FunctionBindingGenerator]
  private val lib: AtomicStampedReference[FSetBacking[L]] =
    AtomicStampedReference(null, 0)
  private val ver = Array(0)

  def get(using lm: FSetModule): FSetBacking[L] =
    var l = lib.get(ver)
    if ver(0) != lm.runtimeVersion || l == null then
      val old = l
      l = lm
        .getBacking(dependencies, description, generation)
        .asInstanceOf[FSetBacking[L]]
      lib.compareAndSet(old, l, ver(0), lm.runtimeVersion)
    l.asInstanceOf[FSetBacking[L]]

object FSet:
  transparent inline def instance[A](using l: FSet[A], lm: FSetModule) =
    ${ summonImpl[A]('l, 'lm) }

  inline def derived[L]: FSet[L] = ${ derivedImpl[L]() }

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
          FunctionBindingGenerator[L](
            $methodNameExpr
          )
        }
    )

    '{
      new FSet[L]:
        val dependencies =
          (NeedsResource[L] ++ Needs[L] ++ NeedsFile[L]).map(_.toDependency)
        val description = $descriptors
        val generation = $generators
    }

  private def summonImpl[A](
      library: Expr[FSet[A]],
      libraryModule: Expr[FSetModule]
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
      .foldLeft(TypeRepr.of[FSetBacking[A]]):
        case (backing, (name, repr)) =>
          Refinement(backing, name, repr)

    refined.asType match
      case '[a] =>
        '{
          $library.get(using $libraryModule).asInstanceOf[a]
        }
