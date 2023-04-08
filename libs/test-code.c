#ifdef _WIN32
#   define EXPORTED  __declspec( dllexport )
#else
# define EXPORTED
#endif

EXPORTED int test_fn(int i) {
  return i;
}
