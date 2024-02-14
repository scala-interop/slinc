package fr.hammons.slinc.internal

import fr.hammons.slinc.Platform

private[slinc] trait TypeRelation[-P, A]:
  type Real <: Matchable
