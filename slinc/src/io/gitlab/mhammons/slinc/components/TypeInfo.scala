package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import scala.util.chaining.*
import io.gitlab.mhammons.slinc.Ptr

sealed trait TypeInfo:
   def rename(name: String): TypeInfo
   val name: String
case class ProductInfo(
    name: String,
    caseMembers: Seq[TypeInfo],
    myType: Type[?]
) extends TypeInfo:
   def rename(name: String) = copy(name = name)
case class PrimitiveInfo(name: String, myType: Type[?]) extends TypeInfo:
   def rename(name: String) = copy(name = name)
case class PtrInfo(name: String, underlying: TypeInfo, myType: Type[?])
    extends TypeInfo:
   def rename(name: String) = copy(name = name)

object TypeInfo:
   def apply[A: Type](using Quotes): TypeInfo =
      import quotes.reflect.*

      Type.of[A] match
         case '[Product] =>
            val originalType = TypeRepr.of[A]
            ProductInfo(
              "",
              originalType.typeSymbol.caseFields.map(s =>
                 originalType.memberType(s).asType.pipe { case '[a] =>
                    TypeInfo[a].rename(s.name)
                 }
              ),
              Type.of[A]
            )
         case '[Int] | '[Float] | '[Long] | '[Short] | '[Byte] | '[Char] |
             '[Boolean] =>
            PrimitiveInfo("", Type.of[A])
         case '[Ptr[a]] =>
            PtrInfo("", TypeInfo[a], Type.of[A])
