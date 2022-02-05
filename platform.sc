import mill._, scalalib._

trait PlatformTypegen extends ScalaModule {
   val typeProtos = Set(
     "dev_t" -> "DevT",
     "size_t" -> "SizeT",
     "time_t" -> "TimeT",
     "clock_t" -> "ClockT",
     "intptr_t" -> "IntptrT",
     "uintptr_t" -> "UintptrT",
     "caddr_t" -> "CaddrT",
     "blkcnt_t" -> "BlkcntT",
     "blksize_t" -> "BlksizeT",
     "gid_t" -> "GidT",
     "in_addr_t" -> "InAddrT",
     "in_port_t" -> "InPortT",
     "ino_t" -> "InoT",
     "ino64_t" -> "Ino64T",
     "key_t" -> "KeyT",
     "nlink_t" -> "NlinkT",
     "id_t" -> "IdT",
     "pid_t" -> "PidT",
     "off_t" -> "OffT",
     "swblk_t" -> "SwblkT",
     "fsblkcnt_t" -> "FsblkcntT",
     "fsfilcnt_t" -> "FsfilcntT",
     "sa_family_t" -> "SaFamilyT",
     "socklen_t" -> "SocklenT",
     "rlim_t" -> "RlimT",
     "cc_t" -> "CcT",
     "speed_t" -> "SpeedT",
     "tcflag_t" -> "TcflagT",
     "mode_t" -> "ModeT",
     "ssize_t" -> "SsizeT",
     "uid_t" -> "UidT"
   )

   def template(typeName: String, cTypeName: String) =
      s"""|package io.gitlab.mhammons.slinc.components
          |
          |trait ${typeName}Proto: 
          | type $typeName
          | type $cTypeName = $typeName
          | val ${typeName}Integral: Integral[$typeName]
          | implicit class ${typeName}Ops(a: $typeName) extends ${typeName}Integral.IntegralOps(a)
          | implicit class ${typeName}Ord(a: $typeName) extends ${typeName}Integral.OrderingOps(a)
          | given ${typeName}NativeInfo: NativeInfo[$typeName]
          | given ${typeName}Immigrator: Immigrator[$typeName]
          | given ${typeName}Emigrator: Emigrator[$typeName]
          | given ${typeName}Reader: Reader[$typeName]
          | given ${typeName}Writer: Writer[$typeName]
          | given ${typeName}Exporter: Exporter[$typeName]
          | val ${typeName}Initializer: Initializer[$typeName]
          | object $typeName:
          |   export ${typeName}Initializer.*
          |
          |trait ${typeName}Impl[U](using 
          | val ${typeName}Integral: Integral[U], 
          | val ${typeName}NativeInfo: NativeInfo[U],
          | val ${typeName}Immigrator: Immigrator[U],
          | val ${typeName}Emigrator: Emigrator[U],
          | val ${typeName}Reader: Reader[U],
          | val ${typeName}Writer: Writer[U],
          | val ${typeName}Exporter: Exporter[U],
          | val ${typeName}Initializer: Initializer[U]
          |) extends ${typeName}Proto:
          | type $typeName = U""".stripMargin

   def generateProtos = T {
      val paths =
         for { (cTypeName, typeName) <- typeProtos } yield {
            val dest = T.dest / s"$typeName.scala"
            os.write(dest, template(typeName, cTypeName))
            PathRef(dest)
         }
      (paths, PathRef(T.dest))
   }

   override def generatedSources = T {
      val (_, destPathRef) = generateProtos()
      super.generatedSources() :+ destPathRef
   }

}
