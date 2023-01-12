#ifdef _WIN32
#   define EXPORTED  __declspec( dllexport )
#else
# define EXPORTED
#endif

EXPORTED int identity_int(int i) {
  return i;
}

struct I31Struct {
  const char* field;

};

EXPORTED const char* i31test(struct I31Struct i31Struct) {
  return i31Struct.field;
}