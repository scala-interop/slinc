package fr.hammons.slinc.modules

import jdk.incubator.foreign.{FunctionDescriptor as JFunctionDescriptor, *}
import fr.hammons.slinc.*
import scala.jdk.OptionConverters.*
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodType

object LinkageModule17 extends LinkageModule:
  import descriptorModule17.*
  override type CSymbol = Addressable
  private val linker = CLinker.getInstance().nn
  private val lookup = CLinker.systemLookup().nn
  private val ts = Scope17(linker)

  override def defaultLookup(name: String): Option[CSymbol] =
    lookup.lookup(name).nn.toScala

  override def getDowncall(descriptor: FunctionDescriptor): MethodHandle =
    val fd = descriptor.outputDescriptor match
      case None =>
        JFunctionDescriptor.ofVoid(
          descriptor.inputDescriptors.view
            .map(toMemoryLayout)
            .concat(
              descriptor.variadicDescriptors.view
                .map(toMemoryLayout)
                .map(CLinker.asVarArg)
            )
            .toSeq*
        )
      case Some(value) =>
        JFunctionDescriptor.of(
          toMemoryLayout(value),
          descriptor.inputDescriptors.view
            .map(toMemoryLayout)
            .concat(
              descriptor.variadicDescriptors.view
                .map(toMemoryLayout)
                .map(CLinker.asVarArg)
            )
            .toSeq*
        )

    val mt = descriptor match
      case FunctionDescriptor(head +: tail, variadicDescriptors, None) =>
        VoidHelper.methodTypeV(
          toCarrierType(head),
          tail.view.concat(variadicDescriptors).map(toCarrierType).toSeq*
        )
      case FunctionDescriptor(
            head +: tail,
            variadicDescriptors,
            Some(outputDescriptor)
          ) =>
        MethodType.methodType(
          toCarrierType(outputDescriptor),
          toCarrierType(head),
          tail.view.concat(variadicDescriptors).map(toCarrierType).toSeq*
        )
      case FunctionDescriptor(_, _, None) => VoidHelper.methodTypeV()
      case FunctionDescriptor(_, _, Some(outputDescriptor)) =>
        MethodType.methodType(toCarrierType(outputDescriptor))

    linker.downcallHandle(mt, fd).nn
  end getDowncall

  def tempScope(): Scope = ts.createTempScope
