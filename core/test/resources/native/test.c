#ifdef _WIN32
#   define EXPORTED  __declspec( dllexport )
#else
# define EXPORTED
#endif

#include <stdarg.h>

EXPORTED int identity_int(int i) {
  return i;
}

struct I31Struct {
  const char* field;

};

EXPORTED const char* i31test(struct I31Struct i31Struct) {
  return i31Struct.field;
}

typedef struct I36Struct {
    int i;
    const char *c;
} I36Struct;

static I36Struct _mylib_struct = {
    42,
    "mylib"
};

typedef struct I36Inner {
  int i;
} I36Inner;

typedef struct I36Outer {
  I36Inner* inner;
} I36Outer;

static I36Inner _i36Inner = {
  43
};

static I36Outer _i36Outer = {
  &_i36Inner
};

EXPORTED const I36Struct *i36_get_my_struct(void) {    
    return &_mylib_struct;
}

EXPORTED const I36Struct i36_get_mystruct_by_value(void) {
    return _mylib_struct;
}

EXPORTED void i36_copy_my_struct(I36Struct * my_struct) {
    my_struct->i = 42;
    my_struct->c = "mylib";
    return;
}

EXPORTED const I36Outer* i36_nested(void) {
  return &_i36Outer;
}

EXPORTED int i144_pass_va_list(va_list args) {
  va_list my_args;
  va_copy(args, my_args);
  int i = va_arg(my_args, int);
  va_end(my_args);
  return i;
}