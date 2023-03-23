package fr.hammons.slinc.modules

import fr.hammons.slinc.LibBacking
import fr.hammons.slinc.Allocator
import java.util.concurrent.atomic.AtomicReference
import fr.hammons.slinc.CFunctionBindingGenerator
import fr.hammons.slinc.OutputTransition
import fr.hammons.slinc.CFunctionDescriptor
import fr.hammons.slinc.MethodHandler
import fr.hammons.slinc.Variadic
import fr.hammons.slinc.AliasDescriptor
import fr.hammons.slinc.StructDescriptor

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
                transitionModule17.methodArgument(a)
              )
            else Nil

          val regularTransitions =
            cfd.inputDescriptors
              .map: td =>
                (a: Allocator) ?=> transitionModule17.methodArgument(td, _, a)
              .map: fn =>
                (a: Allocator, b: Any) => fn(using a)(b)

          val retTransition: OutputTransition = cfd.returnDescriptor
            .map: td =>
              (o: Object | Null) =>
                transitionModule17.methodReturn[AnyRef](td, o.nn)
            .getOrElse: (_: Object | Null) =>
              ().asInstanceOf[Object]

          val fn =
            generator.generate(
              mh,
              IArray.from(prefixTransition ++ regularTransitions),
              transitionModule17,
              retTransition,
              tempScope(),
              allocatingReturn,
              cfd.isVariadic
            )

          AtomicReference(fn)

    LibBacking(IArray.from(fns)).asInstanceOf[LibBacking[?]]

  end getLibrary
