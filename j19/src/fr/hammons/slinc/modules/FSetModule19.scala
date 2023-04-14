package fr.hammons.slinc.modules

import fr.hammons.slinc.*
import java.util.concurrent.atomic.AtomicReference

import fr.hammons.slinc.fset.FSetBacking

import fr.hammons.slinc.FunctionContext
import fr.hammons.slinc.fset.Dependency

import java.util.NoSuchElementException
given fsetModule19: FSetModule with
  override val runtimeVersion: Int = 19

  override def getBacking(
      dependencies: List[Dependency],
      desc: List[CFunctionDescriptor],
      generators: List[FunctionBindingGenerator]
  ): FSetBacking[?] =
    import LinkageModule19.*

    dependencies.foreach(LinkageTools.loadDependency)

    val fns = desc
      .zip(generators)
      .map:
        case (cfd, generator) =>
          val runtimeInformation = FunctionContext(cfd)
          val addr = defaultLookup(runtimeInformation.name)
            .orElse(loaderLookup(runtimeInformation.name))
            .getOrElse(
              throw new NoSuchElementException(
                s"unable to find native function: ${runtimeInformation.name}"
              )
            )
          val mh: MethodHandler = MethodHandler((v: Seq[Variadic]) =>
            getDowncall(cfd, v).bindTo(addr).nn
          )

          val fn =
            generator.generate(
              mh,
              runtimeInformation,
              (allocator, varArgs) =>
                varArgs.map: varArg =>
                  varArg.use[DescriptorOf]: descriptorOf ?=>
                    data =>
                      transitionModule19.methodArgument(
                        descriptorOf.descriptor,
                        data,
                        allocator
                      ),
              tempScope()
            )

          AtomicReference(fn)
    FSetBacking(IArray.from(fns)).asInstanceOf[FSetBacking[?]]
