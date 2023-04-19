package fr.hammons.slinc.modules

import fr.hammons.slinc.fset.FSetBacking
import java.util.concurrent.atomic.AtomicReference
import fr.hammons.slinc.FunctionBindingGenerator
import fr.hammons.slinc.CFunctionDescriptor
import fr.hammons.slinc.MethodHandler
import fr.hammons.slinc.Variadic
import fr.hammons.slinc.FunctionContext
import fr.hammons.slinc.DescriptorOf
import fr.hammons.slinc.fset.Dependency
import java.util.NoSuchElementException

given fsetModule17: FSetModule with
  val runtimeVersion = 17
  import LinkageModule17.*

  override def getBacking(
      dependencies: List[Dependency],
      desc: List[CFunctionDescriptor],
      generators: List[FunctionBindingGenerator]
  ): FSetBacking[?] =

    dependencies.foreach(LinkageTools.loadDependency)

    val fns = desc
      .zip(generators)
      .map:
        case (cfd, generator) =>
          val functionInformation = FunctionContext(cfd)
          val addr = defaultLookup(functionInformation.name)
            .orElse(loaderLookup(functionInformation.name))
            .getOrElse(
              throw new NoSuchElementException(
                s"unable to find native function: ${functionInformation.name}"
              )
            )
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
              tempScope
            )

          AtomicReference(fn)

    FSetBacking(IArray.from(fns)).asInstanceOf[FSetBacking[?]]

  end getBacking
