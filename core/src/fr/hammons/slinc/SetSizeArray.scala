package fr.hammons.slinc

import scala.reflect.ClassTag
import scala.compiletime.ops.int.{`*`, `-`, `<=`, `+`, `<`}
import scala.compiletime.constValue
import scala.quoted.*
import scala.language.experimental.erasedDefinitions

class SetSizeArray[A, B <: Int] private[slinc] (private val array: Array[A])
    extends AnyVal:
  def map[C: ClassTag](fn: A => C): SetSizeArray[C, B] =
    new SetSizeArray[C, B](array.map(fn))
  def flatMap[C: ClassTag, D <: Int](
      fn: A => SetSizeArray[C, D]
  ): SetSizeArray[C, B * D] =
    new SetSizeArray[C, B * D](array.flatMap(fn.andThen(_.array)))
  def toSeq: Seq[A] = array.toSeq
  def toArray: Array[A] = array
  inline def take[C <: Int](using
      C <= B =:= true,
      0 <= C =:= true
  ): SetSizeArray[A, C] =
    SetSizeArray.fromArrayUnsafe[C](array.take(constValue[C]))
  inline def drop[C <: Int](using
      0 <= B - C =:= true,
      0 <= C =:= true
  ): SetSizeArray[A, B - C] =
    SetSizeArray.fromArrayUnsafe[B - C](array.drop(constValue[C]))
  def forall(fn: A => Boolean): Boolean = array.forall(fn)
  def exists(fn: A => Boolean): Boolean = array.exists(fn)
  inline def concat[C >: A: ClassTag, D <: Int](
      o: SetSizeArray[C, D]
  )(using 0 <= D + B =:= true): SetSizeArray[C, D + B] =
    SetSizeArray.fromArrayUnsafe[D + B](array.concat(o.array))
  def isEqual(oArray: SetSizeArray[A, B]): Boolean =
    array.zip(oArray.array).forall(_ == _)

  def unsafeApply(index: Int): A = array(index)
  def unsafePut(index: Int, value: A): Unit = array(index) = value
  inline def apply[C <: Int](using 0 <= C =:= true, C < B =:= true): A = array(
    constValue[C]
  )
  inline def put[C <: Int](
      value: A
  )(using 0 <= C =:= true, C < B =:= true): Unit = array(constValue[C]) = value

  def zip[C](oArr: SetSizeArray[C, B]): SetSizeArray[(A, C), B] =
    new SetSizeArray[(A, C), B](array.zip(oArr.array))
  def foreach(fn: A => Unit) = array.foreach(fn)

object SetSizeArray:
  class SetSizeArrayBuilderUnsafe[B <: Int]:
    def apply[A](array: Array[A]): SetSizeArray[A, B] = new SetSizeArray(array)
  class SetSizeArrayBuilder[B <: Int](length: B):
    def apply[A](array: Array[A]): Option[SetSizeArray[A, B]] =
      if length == array.length then Some(new SetSizeArray(array)) else None

  inline def fromArray[B <: Int](using
      (0 <= B) =:= true
  ): SetSizeArrayBuilder[B] = SetSizeArrayBuilder[B](constValue[B])

  def fromArrayUnsafe[B <: Int](using
      (0 <= B) =:= true
  ): SetSizeArrayBuilderUnsafe[B] = SetSizeArrayBuilderUnsafe[B]

  inline def ofDim[B <: Int, A: ClassTag](using
      0 <= B =:= true
  ): SetSizeArray[A, B] =
    SetSizeArray.fromArrayUnsafe[B](Array.ofDim[A](constValue[B]))

  transparent inline def apply[A: ClassTag](inline a: A*): Any = ${
    knownValues[A]('a, '{ summon[ClassTag[A]] })
  }
  private def knownValues[A](values: Expr[Seq[A]], ct: Expr[ClassTag[A]])(using
      Quotes,
      Type[A]
  ): Expr[Any] =
    import quotes.reflect.*
    values match
      case Varargs(vs) =>
        val n = Apply(
          TypeApply(
            Select(
              New(
                Applied(
                  TypeTree.of[SetSizeArray],
                  List(TypeTree.of[A], Singleton(Literal(IntConstant(vs.size))))
                )
              ),
              TypeRepr.of[SetSizeArray].classSymbol.get.primaryConstructor
            ),
            List(TypeTree.of[A], Singleton(Literal(IntConstant(vs.size))))
          ),
          List('{ Array($values*)(using $ct) }.asTerm)
        )

        n.asExpr
