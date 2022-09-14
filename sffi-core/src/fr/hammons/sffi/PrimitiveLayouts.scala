package fr.hammons.sffi

trait LayoutPlatformSpecific[B <: BasicsI](val basics: BasicsI):
  import basics.Context
  val intLayout: Context 
  val floatLayout: Context 
  val byteLayout: Context 
  val longLayout: Context 

  def groupLayout(layouts: List[(String, Context)]): Context 
  extension (c: Context) 
    def withName(name: String): Context