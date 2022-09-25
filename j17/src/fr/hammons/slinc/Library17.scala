package fr.hammons.slinc

import java.lang.invoke.MethodHandle
import jdk.incubator.foreign.FunctionDescriptor
import jdk.incubator.foreign.MemorySegment
import jdk.incubator.foreign.MemoryAddress
import java.lang.invoke.MethodType
import fr.hammons.sffi.VoidHelper
import jdk.incubator.foreign.CLinker
import jdk.incubator.foreign.SymbolLookup
import jdk.incubator.foreign.Addressable

object Library17 extends LibraryI.PlatformSpecific:
  val linker = CLinker.getInstance().nn

  def layout2CarrierType(dataLayout: DataLayout) = 
    dataLayout match 
      case IntLayout(_,_,_) => classOf[Int]
      case _: StructLayout  => classOf[MemorySegment]
      case _: PointerLayout => classOf[MemoryAddress]
  override def getDowncall(address: Object, layout: Seq[DataLayout], ret: Option[DataLayout]): MethodHandle = 
    val fd = ret match 
      case Some(r) => FunctionDescriptor.of(LayoutI17.dataLayout2MemoryLayout(r), layout.map(LayoutI17.dataLayout2MemoryLayout)*)
      case _ => FunctionDescriptor.ofVoid(layout.map(LayoutI17.dataLayout2MemoryLayout)*)

    val md = (ret, layout.headOption) match
      case (Some(r), Some(h)) => 
        MethodType.methodType(layout2CarrierType(r), layout2CarrierType(h), layout.tail.map(layout2CarrierType)*)
      case (Some(r), None) =>
        MethodType.methodType(layout2CarrierType(r))
      case (None, Some(h)) => 
        VoidHelper.methodTypeV(layout2CarrierType(h), layout.tail.map(layout2CarrierType)*)
      case (None, None) =>
        VoidHelper.methodTypeV()

      
    linker.downcallHandle(address.asInstanceOf[Addressable],md, fd).nn

  def getDowncall(layout: Seq[DataLayout], ret: Option[DataLayout]): MethodHandle =     
    val fd = ret match 
      case Some(r) => FunctionDescriptor.of(LayoutI17.dataLayout2MemoryLayout(r), layout.map(LayoutI17.dataLayout2MemoryLayout)*)
      case _ => FunctionDescriptor.ofVoid(layout.map(LayoutI17.dataLayout2MemoryLayout)*)

    val md = (ret, layout.headOption) match
      case (Some(r), Some(h)) => 
        MethodType.methodType(layout2CarrierType(r), layout2CarrierType(h), layout.tail.map(layout2CarrierType)*)
      case (Some(r), None) =>
        MethodType.methodType(layout2CarrierType(r))
      case (None, Some(h)) => 
        VoidHelper.methodTypeV(layout2CarrierType(h), layout.tail.map(layout2CarrierType)*)
      case (None, None) =>
        VoidHelper.methodTypeV()

    linker.downcallHandle(md, fd).nn


  override def getLookup(name: Option[String]): Lookup = 
    import scala.jdk.OptionConverters.*
    name match 
      case Some(n) => new Lookup:
        System.loadLibrary(n)
        val l = SymbolLookup.loaderLookup().nn
        def lookup(name: String) = l.lookup(name).nn.toScala
      case None => new Lookup:
        val l = CLinker.systemLookup().nn
        def lookup(name: String) = l.lookup(name).nn.toScala

