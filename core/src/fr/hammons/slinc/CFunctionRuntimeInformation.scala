package fr.hammons.slinc

import fr.hammons.slinc.modules.TransitionModule
import fr.hammons.slinc.CFunctionRuntimeInformation.InputTransition
import fr.hammons.slinc.CFunctionRuntimeInformation.ReturnTransition
import fr.hammons.slinc.types.{os, arch}

final case class CFunctionRuntimeInformation(
    name: String,
    inputDescriptors: Seq[TypeDescriptor],
    inputTransitions: IArray[InputTransition],
    returnDescriptor: Option[TypeDescriptor],
    returnTransition: ReturnTransition,
    isVariadic: Boolean,
    returnAllocates: Boolean
)

object CFunctionRuntimeInformation:
  def apply(functionDescriptor: CFunctionDescriptor)(using
      transitionModule: TransitionModule
  ) =
    val allocatingReturn = functionDescriptor.returnDescriptor
      .map:
        case ad: AliasDescriptor[?] => ad.real
        case a                      => a
      .exists:
        case _: StructDescriptor => true
        case _                   => false

    val allocationTransition: Seq[InputTransition] =
      if allocatingReturn then
        Seq((allocator, _) => transitionModule.methodArgument(allocator))
      else Seq.empty

    val inputTransitions: Seq[InputTransition] =
      functionDescriptor.inputDescriptors.map: typeDescriptor =>
        (allocator, input) =>
          transitionModule.methodArgument(typeDescriptor, input, allocator)
    val outputTransition: ReturnTransition =
      functionDescriptor.returnDescriptor match
        case None => _ => ().asInstanceOf[Object]
        case Some(descriptor) =>
          returnValue =>
            transitionModule.methodReturn[Object](descriptor, returnValue.nn)

    new CFunctionRuntimeInformation(
      functionDescriptor.name(os, arch),
      functionDescriptor.inputDescriptors,
      IArray.from(allocationTransition ++ inputTransitions),
      functionDescriptor.returnDescriptor,
      outputTransition,
      functionDescriptor.isVariadic,
      allocatingReturn
    )

  type InputTransition = (Allocator, Any) => Any
  type ReturnTransition = (Object | Null) => AnyRef
