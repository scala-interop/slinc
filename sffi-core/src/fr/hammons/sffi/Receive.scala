package fr.hammons.sffi

import scala.annotation.targetName

import scala.quoted.*
import scala.deriving.Mirror
import java.lang.reflect.Modifier

trait Receive[A]:
  def from(mem: Mem, offset: Bytes): A

object Receive:
  given [A]: Fn[Receive[A], (Mem, Bytes), A] with
    def andThen(fn: Receive[A], andThen: A => A): Receive[A] =
      (mem: Mem, offset: Bytes) => andThen(fn.from(mem, offset))

    @targetName("complexAndThen")
    def andThen[ZZ](
        fn: Receive[A],
        andThen: A => ZZ
    ): FnCalc[(Mem, Bytes), ZZ] = (mem, offset) => andThen(fn.from(mem, offset))

  given Receive[Int] with
    def from(mem: Mem, offset: Bytes): Int = mem.readInt(offset)

  def receiveStaged(
      layout: StructLayout
  ): ([A] => (Quotes ?=> Expr[A]) => A) => Receive[Product] =
    val transforms =
      getTransforms(layout).distinctBy((className, _) => className)
    val transformsArray = transforms.map((_, transform) => transform).toArray
    val transformIndices =
      transforms.map((className, _) => className).zipWithIndex.toMap

    (run: [A] => (Quotes ?=> Expr[A]) => A) =>
      run {
        val code = '{ (fns: Array[Tuple => Product]) =>
          new Receive[Product]:
            def from(mem: Mem, offset: Bytes) = ${
              receiveStagedHelper(
                layout,
                transformIndices
              )('mem, 'offset, 'fns).asExprOf[Product]
            }
        }
        println(code.show)
        code
      }(transformsArray)

  end receiveStaged

  private def receiveStagedHelper(
      layout: DataLayout,
      transformIndices: Map[String, Int]
  )(
      mem: Expr[Mem],
      offset: Expr[Bytes],
      transforms: Expr[Array[Tuple => Product]]
  )(using Quotes): Expr[Any] =
    layout match
      case IntLayout(_, _) =>
        '{ $mem.readInt($offset) }
      case s @ StructLayout(_, _, children) =>
        val transformIndex = transformIndices(s.clazz.getCanonicalName().nn)
        val exprs = children.map {
          case StructMember(childLayout, _, structOffset) =>
            receiveStagedHelper(childLayout, transformIndices)(
              mem,
              '{ $offset + ${ Expr(structOffset) } },
              transforms
            )
        }.toList

        if canBeUsedDirectly(s.clazz) then
          constructFromTarget(s.clazz.getCanonicalName().nn, exprs)
        else
          '{
            $transforms(${ Expr(transformIndex) })(${
              Expr.ofTupleFromSeq(exprs)
            })
          }
  end receiveStagedHelper

  private def canBeUsedDirectly(clazz: Class[?]): Boolean =
    println(clazz.getCanonicalName().nn)
    val enclosingClass = clazz.getEnclosingClass()
    if enclosingClass == null && clazz
        .getEnclosingConstructor() == null && clazz.getEnclosingMethod() == null
    then true
    else if canBeUsedDirectly(enclosingClass.nn) && Modifier.isStatic(
        clazz.getModifiers()
      ) && Modifier.isPublic(clazz.getModifiers())
    then true
    else false

  private def getTransforms(layout: DataLayout): Vector[(String, Tuple => Product)] =
    layout match
      case s @ StructLayout(_, _, members) =>
        (s.clazz.getCanonicalName().nn, s.transform) +: members
          .map(_.layout)
          .flatMap(getTransforms)
      case _ => Vector.empty

  private def constructFromTarget(target: String, members: List[Expr[Any]])(
      using Quotes
  ) =
    import quotes.reflect.*
    Symbol.classSymbol(target).typeRef.asType match
      case '[a] =>
        Apply(
          Select(
            New(TypeTree.of[a]),
            TypeRepr.of[a].typeSymbol.primaryConstructor
          ),
          members.map(_.asTerm)
        ).asExpr
