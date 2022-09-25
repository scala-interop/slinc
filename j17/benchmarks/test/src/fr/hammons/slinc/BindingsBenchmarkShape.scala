package fr.hammons.slinc

import org.openjdk.jmh.annotations.*,
Mode.{SampleTime, SingleShotTime, Throughput}
import java.util.concurrent.TimeUnit
import fr.hammons.slinc.Scope

case class div_t(quot: Int, rem: Int)
trait BindingsBenchmarkShape(val s: Slinc):
  import s.{given, *}

  object Cstd derives Library:
    def abs(i: Int): Int = Library.binding
    def div(numer: Int, denom: Int): div_t = Library.binding

  given Struct[div_t] = Struct.derived

  @Benchmark 
  def abs =
    Cstd.abs(6)


  @Benchmark 
  def div = 
    Cstd.div(5,2)