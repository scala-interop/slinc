package fr.hammons.slinc.modules

import fr.hammons.slinc.{FunctionDescriptor as _, *}
import jdk.incubator.foreign.*
import scala.jdk.OptionConverters.*
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodType

object LinkageModule17 extends LinkageModule:
  import descriptorModule17.*
  override type CSymbol = Addressable
  private val linker = CLinker.getInstance().nn
  private val lookup = CLinker.systemLookup().nn
  private val lookupLoader = SymbolLookup.loaderLookup().nn
  private val ts = Scope17(linker)

  override def defaultLookup(name: String): Option[CSymbol] =
    lookup.lookup(name).nn.toScala

  override def loaderLookup(name: String): Option[CSymbol] =
    lookupLoader.lookup(name).nn.toScala

  override def getDowncall(
      descriptor: CFunctionDescriptor,
      varArgs: Seq[Variadic]
  ): MethodHandle =
    val variadicDescriptors =
      varArgs.view.map(_.use[DescriptorOf](d ?=> _ => d.descriptor))
    val fdConstructor = descriptor.returnDescriptor match
      case None        => FunctionDescriptor.ofVoid(_*)
      case Some(value) => FunctionDescriptor.of(toMemoryLayout(value), _*)

    val fd = fdConstructor(
      descriptor.inputDescriptors.view
        .map(toMemoryLayout)
        .concat(variadicDescriptors.map(toMemoryLayout).map(CLinker.asVarArg))
        .toSeq
    )

    val mtConstructor = (carriers: Seq[Class[?]]) =>
      descriptor.returnDescriptor.map(toCarrierType) match
        case None =>
          carriers match
            case head +: tail =>
              VoidHelper.methodTypeV(head, tail*)
            case _ =>
              VoidHelper.methodTypeV()
        case Some(returnCarrier) =>
          carriers match
            case head +: tail =>
              MethodType.methodType(returnCarrier, head, tail*)
            case _ =>
              MethodType.methodType(returnCarrier)

    val mt = mtConstructor(
      descriptor.inputDescriptors.view
        .concat(variadicDescriptors)
        .map(toCarrierType)
        .toSeq
    )

    linker.downcallHandle(mt, fd).nn
  end getDowncall

  lazy val tempScope: Scope = ts.createTempScope
