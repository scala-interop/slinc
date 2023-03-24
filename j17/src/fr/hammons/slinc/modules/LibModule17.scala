package fr.hammons.slinc.modules

import fr.hammons.slinc.LibBacking
import java.util.concurrent.atomic.AtomicReference
import fr.hammons.slinc.CFunctionBindingGenerator
import fr.hammons.slinc.CFunctionDescriptor
import fr.hammons.slinc.MethodHandler
import fr.hammons.slinc.Variadic
import fr.hammons.slinc.CFunctionRuntimeInformation
import fr.hammons.slinc.DescriptorOf

given libraryModule17: LibModule with
  val runtimeVersion = 17
  import LinkageModule17.*

  override def getLibrary(
      desc: List[CFunctionDescriptor],
      generators: List[CFunctionBindingGenerator]
  ): LibBacking[?] =
    val fns = desc
      .zip(generators)
      .map:
        case (cfd, generator) =>
          val addr = defaultLookup(cfd.name).get
          val mh: MethodHandler = MethodHandler((v: Seq[Variadic]) =>
            getDowncall(cfd, v).bindTo(addr).nn
          )

          val fn =
            generator.generate(
              mh,
              CFunctionRuntimeInformation(cfd),
              (allocator, varArgs) =>
                varArgs.map: varArg =>
                  varArg.use[DescriptorOf]: descriptorOf ?=>
                    data =>
                      transitionModule17.methodArgument(
                        descriptorOf.descriptor,
                        data,
                        allocator
                      ),
              tempScope()
            )

          AtomicReference(fn)

    LibBacking(IArray.from(fns)).asInstanceOf[LibBacking[?]]

  end getLibrary
