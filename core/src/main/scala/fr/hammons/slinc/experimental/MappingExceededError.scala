package fr.hammons.slinc.experimental

class MappingExceededError(unionTyp: String, typ: String)
    extends Error(s"Mapping was exceeded!$unionTyp cannot be fit into $typ")
