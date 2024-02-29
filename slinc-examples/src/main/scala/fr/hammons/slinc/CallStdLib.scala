import fr.hammons.slinc.runtime.given
import fr.hammons.slinc.types.*
import fr.hammons.slinc.*

case class div_t(quot: CInt, rem: CInt) derives Struct

trait MyLib derives FSet:
  def div(numer: CInt, denom: CInt): div_t
val myLib = FSet.instance[MyLib]

@main def calc =
  val div_t(quot, rem) = myLib.div(5, 2)
  assert((quot, rem) == (2, 1), s"Unexpected (quot, rem): ($quot, $rem)")
