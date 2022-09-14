package fr.hammons.sffi


trait FFI3(val basics: BasicsI, layoutImpl: Layout.PlatformSpecific[basics.Context], writeImpl: Write.PlatformSpecific[basics.RawMem, basics.Context]):
  val layout = Layout(layoutImpl)
  val writer = Write(writeImpl, layout, false)

  export layout.layoutOf
  export writer.write