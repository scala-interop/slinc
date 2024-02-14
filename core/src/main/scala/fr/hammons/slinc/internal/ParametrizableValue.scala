package fr.hammons.slinc.internal

import fr.hammons.slinc.internal.ast.PTypeDescriptor
import fr.hammons.slinc.internal.ast.Expression

trait ParametrizableValue[A] extends TypeDescription[A]:
  def adjustmentAst: Expression[?]

object ParametrizableValue
