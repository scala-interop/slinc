package fr.hammons.slinc.modules

import fr.hammons.slinc.LibBacking
import fr.hammons.slinc.Allocator
import java.util.concurrent.atomic.AtomicReference
import fr.hammons.slinc.CFunctionBindingGenerator
import fr.hammons.slinc.OutputTransition
import fr.hammons.slinc.CFunctionDescriptor
import fr.hammons.slinc.MethodHandler
import fr.hammons.slinc.Variadic

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
          // getDowncall(cfd).bindTo(addr).nn
          val transitions = IArray.from(
            cfd.inputDescriptors
              .map(td =>
                (a: Allocator) ?=> transitionModule17.methodArgument(td, _, a)
              )
              .map(fn => (a: Allocator, b: Any) => fn(using a)(b))
          )

          val retTransition: OutputTransition = cfd.returnDescriptor
            .map: td =>
              (o: Object | Null) =>
                transitionModule17.methodReturn[AnyRef](td, o.nn)
            .getOrElse: (_: Object | Null) =>
              ().asInstanceOf[Object]

          val fn =
            if !cfd.isVariadic then
              generator.generate(mh, transitions, retTransition, tempScope())
            else
              generator.generateVariadic(
                mh,
                transitions,
                (td, alloc, a) =>
                  transitionModule17.methodArgument(td, a, alloc),
                retTransition,
                tempScope()
              )

          AtomicReference(fn)

    LibBacking(IArray.from(fns)).asInstanceOf[LibBacking[?]]

  end getLibrary
