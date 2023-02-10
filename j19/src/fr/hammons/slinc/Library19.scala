package fr.hammons.slinc

import java.lang.foreign.Linker
import java.lang.invoke.MethodHandle
import java.lang.foreign.{FunctionDescriptor as JFunctionDescriptor}
import java.lang.foreign.Addressable
import java.lang.foreign.SymbolLookup
import fr.hammons.slinc.modules.descriptorModule19

class Library19(linker: Linker) extends LibraryI.PlatformSpecific:

  override def getDowncall(
      address: Object,
      descriptor: FunctionDescriptor
  ): MethodHandle =
    val fd = descriptor.outputDescriptor match
      case Some(r) =>
        JFunctionDescriptor
          .of(
            descriptorModule19.toMemoryLayout(r),
            descriptor.inputDescriptors.view
              .map(descriptorModule19.toMemoryLayout)
              .toSeq*
          )
          .nn
          .asVariadic(
            descriptor.variadicDescriptors.view
              .map(descriptorModule19.toMemoryLayout)
              .toSeq*
          )
      case _ =>
        JFunctionDescriptor
          .ofVoid(
            descriptor.inputDescriptors.view
              .map(descriptorModule19.toMemoryLayout)
              .toSeq*
          )
          .nn
          .asVariadic(
            descriptor.variadicDescriptors.view
              .map(descriptorModule19.toMemoryLayout)
              .toSeq*
          )

    linker.downcallHandle(address.asInstanceOf[Addressable], fd).nn

  class J19Lookup(s: SymbolLookup, l: LibraryLocation) extends Lookup(l):
    def lookup(name: String): Object =
      s.lookup(name).nn.orElseThrow(() => this.lookupError(name)).nn
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

    J19Lookup(
      SymbolLookup.loaderLookup().nn,
      LibraryLocation.Resource(location)
    )

  override def getStandardLibLookup: Lookup =
    J19Lookup(linker.defaultLookup().nn, LibraryLocation.Standard)
end Library19
