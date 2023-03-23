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

          val allocatingReturn = cfd.returnDescriptor
            .map:
              case ad: AliasDescriptor[?] => ad.real
              case a                      => a
            .exists:
              case _: StructDescriptor => true
              case _                   => false

          val prefixTransition =
            if allocatingReturn then
              List((a: Allocator, _: Any) =>
                transitionModule19.methodArgument(a)
              )
            else Nil

          val regularTransitions =
            cfd.inputDescriptors
              .map: td =>
                (a: Allocator) ?=> transitionModule19.methodArgument(td, _, a)
              .map: fn =>
                (a: Allocator, b: Any) => fn(using a)(b)

          val retTransition: OutputTransition = cfd.returnDescriptor
            .map: td =>
              (o: Object | Null) =>
                transitionModule19.methodReturn[Object](td, o.nn)
            .getOrElse: (_: Object | Null) =>
              ().asInstanceOf[Object]

          val fn =
            generator.generate(
              mh,
              IArray.from(prefixTransition.concat(regularTransitions)),
              transitionModule19,
              retTransition,
              tempScope(),
              allocatingReturn,
              cfd.isVariadic
            )

          AtomicReference(fn)
    LibBacking(IArray.from(fns)).asInstanceOf[LibBacking[?]]
