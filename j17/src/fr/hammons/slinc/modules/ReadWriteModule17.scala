package fr.hammons.slinc.modules

import fr.hammons.slinc.*
import scala.collection.concurrent.TrieMap
import java.lang.invoke.MethodHandle
import scala.reflect.ClassTag
import fr.hammons.slinc.fnutils.Fn
import scala.quoted.*
import java.lang.reflect.Modifier

given readWriteModule17: ReadWriteModule with
  // todo: eliminate this

  def writeFn(
      typeDescriptor: TypeDescriptor
  ): MemWriter[typeDescriptor.Inner] = ???

  override val intWritingExpr: (Quotes) ?=> Expr[MemWriter[Int]] = ???

  val fnCache: TrieMap[CFunctionDescriptor, Mem => ?] =
    TrieMap.empty

  val readerCache = DependentTrieMap[Reader]

  val arrayReaderCache = DependentTrieMap[ArrayReader]

  val writerCache = DependentTrieMap[MemWriter]

  val arrayWriterCache = DependentTrieMap[[I] =>> MemWriter[Array[I]]]

  val byteWriter = (mem, offset, value) => mem.writeByte(value, offset)
  val shortWriter = (mem, offset, value) => mem.writeShort(value, offset)

  val intWriter = (mem, offset, value) => mem.writeInt(value, offset)

  val longWriter = (mem, offset, value) => mem.writeLong(value, offset)

  val floatWriter = (mem, offset, value) => mem.writeFloat(value, offset)

  val doubleWriter = (mem, offset, value) => mem.writeDouble(value, offset)

  val memWriter = (mem, offset, value) => mem.writeAddress(value, offset)

  val byteReader = (mem, offset) => mem.readByte(offset)
  val shortReader = (mem, offset) => mem.readShort(offset)
  val intReader = (mem, offset) => mem.readInt(offset)
  val longReader = (mem, offset) => mem.readLong(offset)

  val floatReader = (mem, offset) => mem.readFloat(offset)
  val doubleReader = (mem, offset) => mem.readDouble(offset)

  val memReader = (mem, offset) => mem.readAddress(offset)

  def unionReader(
      typeDescriptor: TypeDescriptor
  ): Reader[CUnion[? <: NonEmptyTuple]] =
    val size = descriptorModule17.sizeOf(typeDescriptor)
    (mem, offset) =>
      Scope17.createInferredScope(alloc ?=>
        val newMem = alloc.allocate(typeDescriptor, 1)
        newMem.copyFrom(mem.offset(offset).resize(size))

        new CUnion(newMem)
      )

  def unionWriter(td: TypeDescriptor): MemWriter[CUnion[? <: NonEmptyTuple]] =
    val size = descriptorModule17.sizeOf(td)
    (mem, offset, value) => mem.offset(offset).resize(size).copyFrom(value.mem)

  arrayWriterCache
    .addOne(
      ByteDescriptor,
      (mem: Mem, offset: Bytes, value: Array[Byte]) =>
        mem.writeByteArray(value, offset)
    )
  arrayWriterCache
    .addOne(
      IntDescriptor,
      (mem: Mem, offset: Bytes, value: Array[Int]) =>
        mem.writeIntArray(value, offset)
    )

  override def read(
      memory: Mem,
      offset: Bytes,
      typeDescriptor: TypeDescriptor
  ): typeDescriptor.Inner = readerCache
    .getOrElseUpdate(typeDescriptor, typeDescriptor.reader)(memory, offset)

  override def readFn[A](
      mem: Mem,
      descriptor: CFunctionDescriptor,
      fn: => MethodHandle => Mem => A
  )(using Fn[A, ?, ?]): A =
    fnCache
      .getOrElseUpdate(
        descriptor,
        fn(LinkageModule17.getDowncall(descriptor, Nil))
      )
      .asInstanceOf[Mem => A](mem)

  override def readArray[A](memory: Mem, offset: Bytes, size: Int)(using
      DescriptorOf[A],
      ClassTag[A]
  ): Array[A] =
    val desc = DescriptorOf[A]
    arrayReaderCache.getOrElseUpdate(desc, desc.arrayReader)(
      memory,
      offset,
      size
    )

  override def write(
      memory: Mem,
      offset: Bytes,
      typeDescriptor: TypeDescriptor,
      value: typeDescriptor.Inner
  ): Unit = ???

  def asExprOf[A](expr: Expr[Any])(using Quotes, Type[A]) =
    if expr.isExprOf[A] then expr.asExprOf[A]
    else '{ $expr.asInstanceOf[A] }.asExprOf[A]

  def canBeUsedDirectly(clazz: Class[?]): Boolean =
    val enclosingClass = clazz.getEnclosingClass()
    if clazz.getCanonicalName() == null then false
    else if enclosingClass == null && clazz
        .getEnclosingConstructor() == null && clazz.getEnclosingMethod() == null
    then true
    else if canBeUsedDirectly(enclosingClass.nn) && Modifier.isStatic(
        clazz.getModifiers()
      ) && Modifier.isPublic(clazz.getModifiers())
    then true
    else false

  def writeExprHelper(
      typeDescriptor: TypeDescriptor,
      mem: Expr[Mem],
      offset: Expr[Bytes],
      value: Expr[Any]
  )(using Quotes): Expr[Unit] =
    import quotes.reflect.*
    typeDescriptor match
      case ByteDescriptor  => ???
      case ShortDescriptor => ???
      case IntDescriptor =>
        '{
          $mem.writeInt(${ asExprOf[Int](value) }, $offset)
        }
      case LongDescriptor   => ???
      case FloatDescriptor  => ???
      case DoubleDescriptor => ???
      case PtrDescriptor    => ???
      case sd: StructDescriptor if canBeUsedDirectly(sd.clazz) =>
        val fields =
          Symbol.classSymbol(sd.clazz.getCanonicalName().nn).caseFields

        val offsets =
          descriptorModule17.memberOffsets(sd.members.map(_.descriptor))

        val fns = sd.members.zip(offsets).zipWithIndex.map {
          case (
                (StructMemberDescriptor(childDescriptor, name), childOffset),
                index
              ) =>
            (nv: Expr[Product]) =>
              val childField = Select(nv.asTerm, fields(index)).asExpr
              val totalOffset = offset.value
                .map(_ + childOffset)
                .map(Expr(_))
                .getOrElse('{ $offset + ${ Expr(childOffset) } })

              writeExprHelper(childDescriptor, mem, totalOffset, childField)
        }
      case sd: StructDescriptor =>
        val offsets =
          descriptorModule17.memberOffsets(sd.members.map(_.descriptor))
        val fns = sd.members
          .zip(offsets)
          .zipWithIndex
          .map {
            case ((StructMemberDescriptor(td, name), childOffset), index) =>
              (nv: Expr[Product]) =>
                val childField = '{ $nv.productElement(${ Expr(index) }) }
                val totalOffset = offset.value
                  .map(_ + childOffset)
                  .map(Expr(_))
                  .getOrElse('{ $offset + ${ Expr(childOffset) } })

                writeExprHelper(td, mem, totalOffset, childField)
          }
          .toList

        '{
          val a: Product = ${ asExprOf[Product](value) }

          ${
            Expr.block(fns.map(_('a)), '{})
          }
        }

      case AliasDescriptor(real)           => ???
      case VaListDescriptor                => ???
      case CUnionDescriptor(possibleTypes) => ???
      case SetSizeArrayDescriptor(td, x)   => ???

    ???

  def writeExpr(
      typeDescriptor: TypeDescriptor
  )(using Quotes): Expr[MemWriter[Any]] =
    '{ (mem: Mem, offset: Bytes, value: Any) =>
      ${
        writeExprHelper(typeDescriptor, 'mem, 'offset, 'value)
      }
    }

  def writeArrayExpr(typeDescriptor: TypeDescriptor)(using
      Quotes
  ): Expr[MemWriter[Array[Any]]] =
    val elemLength = Expr(typeDescriptor.size)
    '{ (mem: Mem, offset: Bytes, value: Array[Any]) =>
      var x = 0
      while x < value.length do
        ${
          writeExprHelper(
            typeDescriptor,
            'mem,
            '{
              ($elemLength * x) + offset
            },
            '{ value(x) }
          )
        }
        x += 1
    }

  def writeArray[A](memory: Mem, offset: Bytes, value: Array[A])(using
      DescriptorOf[A]
  ): Unit =
    val desc = DescriptorOf[A]
    arrayWriterCache.getOrElseUpdate(desc, desc.arrayWriter)(
      memory,
      offset,
      value
    )
