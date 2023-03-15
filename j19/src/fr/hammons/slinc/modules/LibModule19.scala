package fr.hammons.slinc.modules

import java.lang.invoke.MethodHandle
import fr.hammons.slinc.*
import java.util.concurrent.atomic.AtomicReference

import fr.hammons.slinc.LibBacking

given libModule19: LibModule with
  override val runtimeVersion: Int = 19

  override def getLibrary(
      desc: List[(String, FunctionDescriptor)],
      generators: List[CFunctionBindingGenerator]
  ): LibBacking[?] =
    import LinkageModule19.*
    val fns = desc
      .zip(generators)
      .map:
        case ((name, fd), generator) =>
          val addr = defaultLookup(name).get
          val mh: MethodHandle = getDowncall(fd).bindTo(addr).nn
          val transitions = IArray.from(
            fd.inputDescriptors
              .map: td =>
                (a: Allocator) ?=> transitionModule19.methodArgument(td, _, a)
              .map: fn =>
                (a: Allocator, b: Any) => fn(using a)(b)
          )

          val retTransition: OutputTransition = fd.outputDescriptor
            .map: td =>
              (o: Object | Null) =>
                transitionModule19.methodReturn[Object](td, o.nn)
            .getOrElse: (_: Object | Null) =>
              ().asInstanceOf[Object]

          val fn =
            generator.generate(mh, transitions, retTransition, tempScope())

          AtomicReference(fn)
    LibBacking(IArray.from(fns)).asInstanceOf[LibBacking[?]]
