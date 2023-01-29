package fr.hammons.slinc

import java.lang.invoke.MethodHandle
import jdk.incubator.foreign.FunctionDescriptor
import jdk.incubator.foreign.MemorySegment
import jdk.incubator.foreign.MemoryAddress
import java.lang.invoke.MethodType
import jdk.incubator.foreign.CLinker
import jdk.incubator.foreign.SymbolLookup
import jdk.incubator.foreign.Addressable
import java.nio.file.Paths
import java.nio.file.Files
import fr.hammons.slinc.modules.DescriptorModule

class Library17(layoutI: LayoutI, linker: CLinker)(using dm: DescriptorModule)
    extends LibraryI.PlatformSpecific(layoutI):
  import layoutI.*

  override def getDowncall(
      address: Object,
      descriptor: Descriptor
  ): MethodHandle =
    val fd = descriptor.outputDescriptor match
      case Some(r) =>
        FunctionDescriptor.of(
          LayoutI17.dataLayout2MemoryLayout(dm.toDataLayout(r)),
          descriptor.inputDescriptors.view.map(dm.toDataLayout)
            .map(LayoutI17.dataLayout2MemoryLayout)
            .concat(
              descriptor.variadicDescriptors.view.map(dm.toDataLayout)
                .map(LayoutI17.dataLayout2MemoryLayout)
                .map(CLinker.asVarArg)
            ).toSeq*
        )
      case _ =>
        FunctionDescriptor.ofVoid(
          descriptor.inputDescriptors.view
            .map(dm.toDataLayout)
            .map(LayoutI17.dataLayout2MemoryLayout)
            .concat(
              descriptor.variadicDescriptors.view.map(dm.toDataLayout)
                .map(LayoutI17.dataLayout2MemoryLayout)
                .map(CLinker.asVarArg)
            ).toSeq*
        )

    val md = descriptor.toMethodType

    linker.downcallHandle(address.asInstanceOf[Addressable], md, fd).nn

  class J17Lookup(s: SymbolLookup, libraryLocation: LibraryLocation) extends Lookup(libraryLocation):
    def lookup(name: String): Object = s.lookup(name).nn.orElseThrow(() => throw this.lookupError(name)).nn
  private val standardLibLookup = J17Lookup(CLinker.systemLookup().nn,LibraryLocation.Standardard)
  override def getStandardLibLookup: Lookup =
    Tools.hashCode()

    standardLibLookup
  override def getLibraryPathLookup(libName: String): Lookup =
    System.loadLibrary(libName)
    J17Lookup(SymbolLookup.loaderLookup().nn, LibraryLocation.Path(libName))


  override def getResourceLibLookup(location: String): Lookup = 
    Tools.sendResourceToCache(location)
    Tools.compileCachedResourceIfNeeded(location)
    Tools.loadCachedLibrary(location)
    
    J17Lookup(SymbolLookup.loaderLookup().nn, LibraryLocation.Resource(location))

  override def getLocalLookup(libPath: String): Lookup =
    System.load(libPath)

    J17Lookup(SymbolLookup.loaderLookup().nn, LibraryLocation.Local(libPath))

end Library17