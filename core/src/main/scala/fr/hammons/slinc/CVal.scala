package fr.hammons.slinc

trait CVal[A <: CVal[A]]

object CVal:
  def apply[A <: CVal[A], B](v: B)(using cvd: CValDef[A, B]): cvd.MakeOutput =
    cvd.make(v)
  extension [A <: CVal[A]](v: A)
    def getAs[B](using cvd: CValDef[A, B]): cvd.RetrieveOutput = cvd.retrieve(v)

// trait CVal[A <: CVal[A]] {
//   def getAs[B](using cvd: CValDef[A, B]): cvd.GetOutput
//   def set[B](using cvd: CValDef[A,B])(a: B): cvd.AssignOutput[Unit]
// }

// object CVal:
//   def create[A <: CVal[A], B](b: B)(using cvd: CValDef[A,B]): cvd.AssignOutput[A] =
