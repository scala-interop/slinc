package fr.hammons.slinc

import java.lang.invoke.MethodHandle
import jdk.incubator.foreign.{FunctionDescriptor as JFunctionDescriptor}
import jdk.incubator.foreign.CLinker
import jdk.incubator.foreign.SymbolLookup
import jdk.incubator.foreign.Addressable
import modules.descriptorModule17
import fr.hammons.slinc

class Library17(linker: CLinker) extends LibraryI.PlatformSpecific:

  override def getDowncall(
      address: Object,
      descriptor: FunctionDescriptor
  ): MethodHandle =
    val fd = descriptor.outputDescriptor match
      case Some(r) =>
        JFunctionDescriptor.of(
          descriptorModule17.toMemoryLayout(r),
          descriptor.inputDescriptors.view
            .map(descriptorModule17.toMemoryLayout)
            .concat(
              descriptor.variadicDescriptors.view
                .map(descriptorModule17.toMemoryLayout)
                .map(CLinker.asVarArg)
            )
            .toSeq*
        )
      case _ =>
        JFunctionDescriptor.ofVoid(
          descriptor.inputDescriptors.view
            .map(descriptorModule17.toMemoryLayout)
            .concat(
              descriptor.variadicDescriptors.view
                .map(descriptorModule17.toMemoryLayout)
                .map(CLinker.asVarArg)
            )
            .toSeq*
        )

    val md = descriptor.toMethodType

    linker.downcallHandle(address.asInstanceOf[Addressable], md, fd).nn

  def getDowncall(descriptor: slinc.FunctionDescriptor): MethodHandle =
    val fd = descriptor.outputDescriptor match
      case Some(r) =>
        JFunctionDescriptor.of(
          descriptorModule17.toMemoryLayout(r),
          descriptor.inputDescriptors.view
            .map(descriptorModule17.toMemoryLayout)
            .concat(
              descriptor.variadicDescriptors.view
                .map(descriptorModule17.toMemoryLayout)
                .map(CLinker.asVarArg)
            )
            .toSeq*
        )
      case _ =>
        JFunctionDescriptor.ofVoid(
          descriptor.inputDescriptors.view
            .map(descriptorModule17.toMemoryLayout)
            .concat(
              descriptor.variadicDescriptors.view
                .map(descriptorModule17.toMemoryLayout)
                .map(CLinker.asVarArg)
            )
            .toSeq*
        )

    val md = descriptor.toMethodType
    linker.downcallHandle(md, fd).nn

  class J17Lookup(s: SymbolLookup, libraryLocation: LibraryLocation)
      extends Lookup(libraryLocation):
    def lookup(name: String): Object =
      s.lookup(name).nn.orElseThrow(() => throw this.lookupError(name)).nn
  private val standardLibLookup =
    J17Lookup(CLinker.systemLookup().nn, LibraryLocation.Standard)
  override def getStandardLibLookup: Lookup =
    Tools.hashCode()

    standardLibLookup
  override def getLibraryPathLookup(libName: String): Lookup =
    System.loadLibrary(libName)
    J17Lookup(SymbolLookup.loaderLookup().nn, LibraryLocation.Path(libName))

    
  override def getResourceLibLookup(name:String,candidates: List[String]): Lookup =
    val sharedLibLocation = prepareSharedLibFromResource(name,candidates).toAbsolutePath().toString()
    System.loadLibrary(sharedLibLocation)
    J17Lookup(
      SymbolLookup.loaderLookup().nn,
      LibraryLocation.Resource(name,candidates)
    )

    

  override def getLocalLookup(libPath: String): Lookup =
    System.load(libPath)

    J17Lookup(SymbolLookup.loaderLookup().nn, LibraryLocation.Local(libPath))

end Library17
