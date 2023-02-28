package fr.hammons.slinc

import scala.sys.process.*
import java.nio.file.Path

object Tools:
  def compileNativeSources(src:Path,dest:Path): Path =
    val cmd = Seq(
      "clang",
      "-shared",
      "-fvisibility=default",
      "-Os",
      "-o",
      dest.nn.toAbsolutePath().nn.toString(),
      src.nn.toAbsolutePath().nn.toString()
    )
    if cmd.! != 0 then
      throw Error(s"failed to compile $src: ${cmd.mkString(" ")}")
    else
      dest

end Tools
