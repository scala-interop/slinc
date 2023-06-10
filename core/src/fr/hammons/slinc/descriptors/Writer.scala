package fr.hammons.slinc.descriptors

import fr.hammons.slinc.jitc.OptimizableFn
import fr.hammons.slinc.Mem
import fr.hammons.slinc.Bytes
import fr.hammons.slinc.modules.MemWriter

type Writer[A] = OptimizableFn[MemWriter[A], WriterContext]
