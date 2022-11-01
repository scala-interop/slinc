package fr.hammons.slinc.container

private[slinc] sealed trait Capabilities
private[slinc] sealed trait *:::[A[_], B <: Capabilities] extends Capabilities
private[slinc] sealed trait End extends Capabilities
