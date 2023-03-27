package fr.hammons.slinc.modules

import java.lang.invoke.MethodHandle
import java.lang.foreign.Linker
import java.lang.foreign.{FunctionDescriptor as JFunctionDescriptor}
import scala.jdk.OptionConverters.*
import java.lang.foreign.MemorySegment
import fr.hammons.slinc.*
import java.lang.foreign.MemoryLayout

object LinkageModule19 extends LinkageModule:
  import descriptorModule19.*
  override type CSymbol = MemorySegment
  private val linker = Linker.nativeLinker().nn
  private val lookup = linker.defaultLookup().nn
  private val ts = Scope19(linker)

  override def defaultLookup(name: String): Option[CSymbol] =
    lookup.lookup(name).nn.toScala

  override def getDowncall(
      descriptor: CFunctionDescriptor,
      varargs: Seq[Variadic]
  ): MethodHandle =
    val fdGen = (
        argDescriptors: Seq[MemoryLayout],
        varArgDescriptors: Seq[MemoryLayout]
    ) =>
      descriptor.returnDescriptor match
        case None =>
          JFunctionDescriptor
            .ofVoid(
              argDescriptors*
            )
            .nn
            .asVariadic(varArgDescriptors*)
        case Some(value) =>
          JFunctionDescriptor
            .of(
              toMemoryLayout(value),
              argDescriptors*
            )
            .nn
            .asVariadic(varArgDescriptors*)

    val fd = fdGen(
      descriptor.inputDescriptors
        .map(toMemoryLayout),
      varargs.view
        .map(_.use[DescriptorOf](dc ?=> _ => dc.descriptor))
        .map(toMemoryLayout)
        .toList
    )

    linker.downcallHandle(fd).nn
  end getDowncall

  def tempScope(): Scope = ts.createTempScope
