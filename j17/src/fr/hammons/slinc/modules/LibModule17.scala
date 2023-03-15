package fr.hammons.slinc.modules

import fr.hammons.slinc.FunctionDescriptor

import fr.hammons.slinc.LibBacking
import fr.hammons.slinc.Allocator
import java.util.concurrent.atomic.AtomicReference
import java.lang.invoke.MethodHandle
import fr.hammons.slinc.CFunctionBindingGenerator
import fr.hammons.slinc.OutputTransition

given libraryModule17: LibModule with
  val runtimeVersion = 17
  import LinkageModule17.*

  override def getLibrary(
      desc: List[(String, FunctionDescriptor)],
      generators: List[CFunctionBindingGenerator]
  ): LibBacking[?] =
    val fns = desc
      .zip(generators)
      .map:
        case ((name, fd), generator) =>
          val addr = defaultLookup(name).get
          val mh: MethodHandle =
            getDowncall(fd).bindTo(addr).nn
          val transitions = IArray.from(
            fd.inputDescriptors
              .map(td =>
                (a: Allocator) ?=> transitionModule17.methodArgument(td, _, a)
              )
              .map(fn => (a: Allocator, b: Any) => fn(using a)(b))
          )

          val retTransition: OutputTransition = fd.outputDescriptor
            .map: td =>
              (o: Object | Null) =>
                transitionModule17.methodReturn[AnyRef](td, o.nn)
            .getOrElse: (_: Object | Null) =>
              ().asInstanceOf[Object]
          val fn =
            generator.generate(mh, transitions, retTransition, tempScope())

          AtomicReference(fn)

    LibBacking(IArray.from(fns)).asInstanceOf[LibBacking[?]]

  end getLibrary
