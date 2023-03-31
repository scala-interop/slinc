package fr.hammons.slinc.annotations

import fr.hammons.slinc.fset.Dependency

trait DependencyAnnotation:
  def toDependency: Dependency
