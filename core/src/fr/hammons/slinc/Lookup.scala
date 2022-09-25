package fr.hammons.slinc

trait Lookup:
  def lookup(name: String): Option[Object]