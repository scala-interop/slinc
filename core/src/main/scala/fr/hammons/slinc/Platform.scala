package fr.hammons.slinc

enum Platform:
  case WinX64
  case LinuxX64
  case MacX64

object Platform:
  type All = WinX64.type | LinuxX64.type | MacX64.type
  type WinX64 = WinX64.type
  type LinuxX64 = LinuxX64.type
  type MacX64 = MacX64.type

  type AllSet = (WinX64, LinuxX64, MacX64)
