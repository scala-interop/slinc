package io.gitlab.mhammons.slinc.components

enum LoggingLevel:
   case Info
   case Warn
   case Error

object LoggingLevel:
   inline given LoggingLevel = LoggingLevel.Error
