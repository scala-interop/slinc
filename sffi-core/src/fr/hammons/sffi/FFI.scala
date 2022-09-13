package fr.hammons.sffi

import java.lang.invoke.MethodHandle
import scala.compiletime.{constValue, erasedValue, summonInline, summonFrom}
import scala.annotation.targetName
import fr.hammons.sffi.PtrShape
import scala.deriving.Mirror
import scala.IArray
import scala.quoted.*

trait FFI
    extends WBasics,
      WAllocatable,
      WStructInfo,
      WLayoutInfo,
      WAssign,
      WDeref,
      WPtr,
      WInTransition,
      WOutTransition,
      WTempAllocator,
      WLookup,
      NativeMethodI,
      WStruct,
      StaticArrayI,
      UpcallI:

  val Scope: ScopeSingleton[Scope, Allocator]

  extension [A](a: A)(using eC: LayoutInfo[A]) def context = eC

  def toNative[A](a: A)(using
      exporter: Allocatable[A]
  )(using Scope, Allocator) = exporter(a)

  @targetName("arrayToNative")
  def toNative[A, B <: Tuple](as: Array[A])(using
      exporter: Allocatable[A]
  )(using Scope, Allocator, PtrShape[A, B]) = exporter(as)

  @targetName("stringToNative")
  def toNative[B <: Tuple](string: String)(using
      allocation: Allocatable[Byte]
  )(using Scope, Allocator, PtrShape[Byte, B]): Ptr[Byte]
//given ptrOutTransition[A]: OutTransition[Pointer[A]]

object FFI:
  inline def getFFI = ${
    getFFIImpl
  }
  private def getFFIImpl(using Quotes): Expr[FFI] =
    import quotes.reflect.*

    val ffiP = Symbol.requiredClass("fr.hammons.sffi.FFI").typeRef
    val annotation = Symbol.requiredClass("fr.hammons.sffi.JavaPlatform")
    val searchResult = Symbol
      .requiredPackage("fr.hammons.sffi")
      .declarations
      .filter(potentialSubClass =>
        potentialSubClass.typeRef.<:<(ffiP) && !potentialSubClass.typeRef
          .=:=(ffiP) && potentialSubClass.isTerm && potentialSubClass
          .hasAnnotation(annotation)
      )

    val platforms = searchResult.flatMap(s =>
      s.annotations.collect {
        case Apply(
              Select(New(TypeIdent("JavaPlatform")), "<init>"),
              List(Literal(StringConstant(platform)))
            ) =>
          s -> platform
      }
    )

    val matchExpr = Match(
      '{ System.getProperty("java.version").nn.split('.').head }.asTerm,
      platforms.map((impl, platform) =>
        CaseDef(Expr(platform).asTerm, None, Ident(impl.termRef))
      )
    ).asExprOf[FFI]

    report.info(matchExpr.show)

    matchExpr
