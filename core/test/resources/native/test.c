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

EXPORTED int i30_pass_va_list(int count, va_list args) {
  int i = 0;
  int sum = 0;
  while(i < count) {
    sum += va_arg(args, int);
    i++;
  }
  va_end(args);
  return sum;
}

EXPORTED long long i30_interspersed_ints_and_longs_va_list(int count, va_list args) {
  int i = 0;
  long long sum = 0;
  while(i < count) {
    if(i % 2 != 0) {
      sum += va_arg(args, long);
    } else {
      sum += va_arg(args, int);
    }
    i += 1;
  }
  va_end(args);
  return sum;
}

typedef int (*Adder)(int count, va_list args);

EXPORTED int i30_function_ptr_va_list(int count, Adder adder, ...) {
  va_list my_args;
  va_start(my_args, count);
  int res = adder(count, my_args);
  va_end(my_args);
  return res;
}

typedef struct {
  va_list* list;
} i30_struct;

EXPORTED int i30_struct_va_list_input(i30_struct my_struct) {
  va_list my_list;
  va_copy(my_list, *(my_struct.list));
  int i = va_arg(my_list, int);
  va_end(my_list);
  return i;
}