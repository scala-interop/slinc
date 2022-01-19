package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{CLinker, MemoryAddress}, CLinker.C_CHAR

object HelperTypes:
   opaque type AsciiChar <: Char = Char

   extension (c: Char)
      def asAscii: Option[AsciiChar] = AsciiChar(c)
      def asAsciiOrFail: AsciiChar = AsciiChar(c).getOrElse(
        throw new Exception(s"$c is not an Ascii character")
      )

   object AsciiChar:
      def apply(char: Char): Option[AsciiChar] =
         if char.toInt > 255 then None
         else Some(char)

      def apply(byte: Byte): AsciiChar = byte.toChar.asAsciiOrFail

      extension (ac: AsciiChar) def toChar = ac.toChar

      given NativeInfo[AsciiChar] with
         val layout = C_CHAR
         val carrierType = classOf[Byte]

      given Serializer[AsciiChar] =
         serializerOf[Byte].contramap[AsciiChar](_.toByte)

      given Deserializer[AsciiChar] = deserializerOf[Byte].map(AsciiChar.apply)

      given Exporter[AsciiChar] = Exporter.derive[AsciiChar]

      given Immigrator[AsciiChar] = immigrator[Byte].map(AsciiChar.apply)

      given Emigrator[AsciiChar] =
         emigrator[Byte].contramap[AsciiChar](_.toByte)

// given =:=[AsciiChar, AsciiChar] = summon[Char =:= Char]
