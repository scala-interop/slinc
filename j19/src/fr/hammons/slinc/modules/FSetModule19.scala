package fr.hammons.slinc.modules

import fr.hammons.slinc.*
import java.util.concurrent.atomic.AtomicReference

import fr.hammons.slinc.FSetBacking

import fr.hammons.slinc.FunctionContext
import fr.hammons.slinc.fset.Dependency
given fsetModule19: FSetModule with
  override val runtimeVersion: Int = 19

  val loadedLibraries: AtomicReference[Set[Dependency]] =
    AtomicReference.apply(Set.empty)

  override def getBacking(
      dependencies: List[Dependency],
      desc: List[CFunctionDescriptor],
      generators: List[FunctionBindingGenerator]
  ): FSetBacking[?] =
    import LinkageModule19.*

    val loaded = loadedLibraries.get().nn
    val newLoaded = dependencies.foldLeft(loaded):
      case (loaded, dep @ Dependency.Resource(path)) if !loaded.contains(dep) =>
        System.load(path)
        loaded + dep
      case (loaded, _) =>
        loaded

    loadedLibraries.compareAndExchange(loaded, newLoaded)

    val fns = desc
      .zip(generators)
      .map:
        case (cfd, generator) =>
          val runtimeInformation = FunctionContext(cfd)
          val addr = defaultLookup(runtimeInformation.name).get
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
