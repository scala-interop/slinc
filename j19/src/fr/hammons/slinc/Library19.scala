package fr.hammons.slinc

import java.lang.foreign.Linker
import java.lang.invoke.MethodHandle
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Addressable
import dotty.tools.dotc.core.SymbolLoader
import java.lang.foreign.SymbolLookup

class Library19(layoutI: LayoutI) extends LibraryI.PlatformSpecific(layoutI):
  val linker = Linker.nativeLinker().nn
  import layoutI.*

  override def getDowncall(address: Object, descriptor: Descriptor): MethodHandle = 
    val fd = descriptor.outputLayout match 
      case Some(r) => FunctionDescriptor.of(LayoutI19.dataLayout2MemoryLayout(r), descriptor.inputLayouts.map(LayoutI19.dataLayout2MemoryLayout)*)
      case _ => FunctionDescriptor.ofVoid(descriptor.inputLayouts.map(LayoutI19.dataLayout2MemoryLayout)*)

    val md = descriptor.toMethodType

    linker.downcallHandle(address.asInstanceOf[Addressable], fd).nn

  override def getLookup(name: Option[String]): Lookup = 
    import scala.jdk.OptionConverters.*
    name match 
      case Some(n) => new Lookup:
        System.loadLibrary(n)
        val l = SymbolLookup.loaderLookup().nn
        def lookup(name: String) = l.lookup(name).nn.toScala
      case None => new Lookup:
        val l = linker.defaultLookup().nn
        def lookup(name: String) = l.lookup(name).nn.toScala
    

