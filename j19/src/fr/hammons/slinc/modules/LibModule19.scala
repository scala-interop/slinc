package fr.hammons.slinc.modules

import fr.hammons.slinc.*
import java.util.concurrent.atomic.AtomicReference

import fr.hammons.slinc.LibBacking

given libModule19: LibModule with
  override val runtimeVersion: Int = 19

  override def getLibrary(
      desc: List[CFunctionDescriptor],
      generators: List[CFunctionBindingGenerator]
  ): LibBacking[?] =
    import LinkageModule19.*
    val fns = desc
      .zip(generators)
      .map:
        case (cfd, generator) =>
          val addr = defaultLookup(cfd.name).get
          val mh: MethodHandler = MethodHandler((v: Seq[Variadic]) =>
            getDowncall(cfd, v).bindTo(addr).nn
          )
          val transitions = IArray.from(
            cfd.inputDescriptors
              .map: td =>
                (a: Allocator) ?=> transitionModule19.methodArgument(td, _, a)
              .map: fn =>
                (a: Allocator, b: Any) => fn(using a)(b)
          )

          val retTransition: OutputTransition = cfd.returnDescriptor
            .map: td =>
              (o: Object | Null) =>
                transitionModule19.methodReturn[Object](td, o.nn)
            .getOrElse: (_: Object | Null) =>
              ().asInstanceOf[Object]

          val fn =
            if !cfd.isVariadic then
              generator.generate(mh, transitions, retTransition, tempScope())
            else
              generator.generateVariadic(
                mh,
                transitions,
                (descriptor, alloc, value) =>
                  transitionModule19.methodArgument(descriptor, value, alloc),
                retTransition,
                tempScope()
              )

          AtomicReference(fn)
    LibBacking(IArray.from(fns)).asInstanceOf[LibBacking[?]]
