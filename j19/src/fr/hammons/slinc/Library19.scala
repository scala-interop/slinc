package fr.hammons.slinc

import java.lang.foreign.Linker
import java.lang.invoke.MethodHandle
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Addressable
import dotty.tools.dotc.core.SymbolLoader
import java.lang.foreign.SymbolLookup
import java.nio.file.Paths
import java.nio.file.Files

class Library19(layoutI: LayoutI, linker: Linker)
    extends LibraryI.PlatformSpecific(layoutI):
  import layoutI.*

  override def getDowncall(
      address: Object,
      descriptor: Descriptor
  ): MethodHandle =
    val fd = descriptor.outputLayout match
      case Some(r) =>
        FunctionDescriptor
          .of(
            LayoutI19.dataLayout2MemoryLayout(r),
            descriptor.inputLayouts.map(LayoutI19.dataLayout2MemoryLayout)*
          )
          .nn
          .asVariadic(
            descriptor.variadicLayouts.map(LayoutI19.dataLayout2MemoryLayout)*
          )
      case _ =>
        FunctionDescriptor
          .ofVoid(
            descriptor.inputLayouts.map(LayoutI19.dataLayout2MemoryLayout)*
          )
          .nn
          .asVariadic(
            descriptor.variadicLayouts.map(LayoutI19.dataLayout2MemoryLayout)*
          )

    val md = descriptor.toMethodType

    linker.downcallHandle(address.asInstanceOf[Addressable], fd).nn

  class J19Lookup(s: SymbolLookup, l: LibraryLocation) extends Lookup(l):
    def lookup(name: String): Object = s.lookup(name).nn.orElseThrow(() => this.lookupError(name)).nn
  override def getLibraryPathLookup(name: String): Lookup = 
    System.loadLibrary(name)

    J19Lookup(SymbolLookup.loaderLookup().nn, LibraryLocation.Path(name))

  override def getLocalLookup(name: String): Lookup = 
    System.load(name)

    J19Lookup(SymbolLookup.loaderLookup().nn, LibraryLocation.Local(name))

  override def getResourceLibLookup(location: String): Lookup = 
    Tools.sendResourceToCache(location)
    Tools.compileCachedResourceIfNeeded(location)
    Tools.loadCachedLibrary(location)

    J19Lookup(SymbolLookup.loaderLookup().nn, LibraryLocation.Resource(location))

  override def getStandardLibLookup: Lookup = J19Lookup(linker.defaultLookup().nn, LibraryLocation.Standardard)
end Library19