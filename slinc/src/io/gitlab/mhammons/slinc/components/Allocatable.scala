package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.SegmentAllocator

trait Allocatable[A] extends Template[A]:
   def allocate(using SegmentAllocator): A