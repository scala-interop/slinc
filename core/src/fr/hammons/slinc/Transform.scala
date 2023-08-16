package fr.hammons.slinc

trait Transform[A, B](using val desc: DescriptorOf[B])(
    _transformFrom: B => A,
    _transformTo: A => B
) extends DescriptorOf[A]:
  val descriptor
      : TransformDescriptor { type Inner = A; val cRep: desc.descriptor.type } =
    new TransformDescriptor:
      val cRep: desc.descriptor.type = desc.descriptor
      type Inner = A
      val transformFrom = _transformFrom
      val transformTo = _transformTo
