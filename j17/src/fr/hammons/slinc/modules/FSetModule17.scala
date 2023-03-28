package fr.hammons.slinc.modules

import fr.hammons.slinc.FSetBacking
import java.util.concurrent.atomic.AtomicReference
import fr.hammons.slinc.CFunctionBindingGenerator
import fr.hammons.slinc.CFunctionDescriptor
import fr.hammons.slinc.MethodHandler
import fr.hammons.slinc.Variadic
import fr.hammons.slinc.CFunctionRuntimeInformation
import fr.hammons.slinc.DescriptorOf

given fsetModule17: FSetModule with
  val runtimeVersion = 17
  import LinkageModule17.*

  override def getBacking(
      desc: List[CFunctionDescriptor],
      generators: List[CFunctionBindingGenerator]
  ): FSetBacking[?] =
    val fns = desc
      .zip(generators)
      .map:
        case (cfd, generator) =>
          val functionInformation = CFunctionRuntimeInformation(cfd)
          val addr = defaultLookup(functionInformation.name).get
          val mh: MethodHandler = MethodHandler((v: Seq[Variadic]) =>
            getDowncall(cfd, v).bindTo(addr).nn
          )

          val fn =
            generator.generate(
              mh,
              functionInformation,
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

    FSetBacking(IArray.from(fns)).asInstanceOf[FSetBacking[?]]

  end getBacking
