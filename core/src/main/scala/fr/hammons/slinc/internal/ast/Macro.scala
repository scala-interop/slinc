package fr.hammons.slinc.internal.ast

import scala.quoted.Quotes
import scala.quoted.Expr
import scala.util.Random

object Macro {
  transparent inline def myMacro: String = ${
    myMacroImpl
  }

  def myMacroImpl(using Quotes): Expr[String] =
    val random = Random.nextString(50)
    Expr(random)
}
