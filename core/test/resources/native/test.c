#ifdef _WIN32
#   define EXPORTED  __declspec( dllexport )
#else
# define EXPORTED
#endif

EXPORTED int identity_int(int i) {
  return i;
}