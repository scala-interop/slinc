package fr.hammons.slinc

import fr.hammons.slinc.internal.ast.Expression
import fr.hammons.slinc.internal.FieldGenerator

trait Runtime:
  def platform: Platform
