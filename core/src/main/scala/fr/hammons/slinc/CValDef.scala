package fr.hammons.slinc

trait CValDef[A <: CVal[A], B] {
  type RetrieveOutput
  type MakeOutput
  def make(b: B): MakeOutput
  def retrieve(a: A): RetrieveOutput
}
