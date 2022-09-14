package fr.hammons.sffi

import scala.compiletime.erasedValue

import scala.quoted.*

class Layout[Context](
    private val platformSpecific: Layout.PlatformSpecific[Context]
) {
  import platformSpecific.withName

  given GenCache = GenCache()

  inline def layoutOf[A]: Context =
    inline erasedValue[A] match
      case _: Int   => platformSpecific.intLayout
      case _: Float => platformSpecific.floatLayout
      case _: Byte  => platformSpecific.byteLayout
      case _: Long  => platformSpecific.longLayout
      case _: Product =>
        GenCache.cacheForType[A, Context, Layout](
          groupLayout(layoutOfStruct[A])
        )

  inline def offsetsOf[A <: Product] =
    GenCache.cacheForType[A & "offsets", IArray[Long], Layout]{
      platformSpecific.offsetsOf(groupLayout(layoutOfStruct[A]))
    }

  private inline def layoutOfStruct[A]: List[(String, Context)] = ${
    Layout.layoutOfStructImpl[A, Context]('this)
  }

  private def groupLayout(list: List[(String, Context)]): Context =
    platformSpecific.groupLayout(
      list.map((name, context) => withName(context, name))
    )
}

object Layout:
  trait PlatformSpecific[Context]:
    val intLayout: Context
    val floatLayout: Context
    val byteLayout: Context
    val longLayout: Context

    def groupLayout(layouts: List[Context]): Context
    def withName(context: Context, name: String): Context
    def offsetsOf(context: Context): IArray[Long]

  def layoutOfStructImpl[A, Context](
      layoutExp: Expr[Layout[Context]]
  )(using Quotes, Type[A], Type[Context]) =
    import quotes.reflect.*

    val layouts = TypeRepr
      .of[A]
      .classSymbol
      .get
      .caseFields
      .map(s =>
        s.typeRef.asType match
          case '[a] =>
            '{
              val le = $layoutExp
              ${ Expr(s.name) } -> le.layoutOf[a]
            }
      )

    Expr.ofList(layouts)
