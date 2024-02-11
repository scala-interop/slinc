package fr.hammons.slinc.internal

opaque type LightOption[+A] = A | Null

object LightOption:
  val None: LightOption[Nothing] =
    null

  extension [A](lo: LightOption[A])
    def isDefined: Boolean = lo != None
    def map[B](fn: A => B): LightOption[B] =
      if lo != null then fn(lo) else None
    def flatMap[B](fn: A => LightOption[B]): LightOption[B] =
      if lo != null then fn(lo) else None
    def getOrThrow(): A =
      if lo != null then lo else throw new Error("Encountered null")
    inline def getOrElse(a: => A) = if lo != null then lo else a

  def apply[A](a: A): LightOption[A] = a

  def unapply[A](lo: LightOption[A]): Option[A] =
    if lo != null then Some(lo) else Option.empty
