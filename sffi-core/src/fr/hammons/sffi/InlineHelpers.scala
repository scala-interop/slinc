package fr.hammons.sffi

import scala.quoted.*
import java.lang.reflect.Modifier


inline def nameOf[T] = ${
  nameOfImpl[T]
}

private def nameOfImpl[T](using Quotes, Type[T]): Expr[String] = 
  import quotes.reflect.*
  Expr(TypeRepr.of[T].classSymbol.get.fullName)


def canBeUsedDirectly(clazz: Class[?]): Boolean =
  val enclosingClass = clazz.getEnclosingClass()
  if enclosingClass == null && clazz
      .getEnclosingConstructor() == null && clazz.getEnclosingMethod() == null
  then true
  else if canBeUsedDirectly(enclosingClass.nn) && Modifier.isStatic(
      clazz.getModifiers()
    ) && Modifier.isPublic(clazz.getModifiers())
  then true
  else false
