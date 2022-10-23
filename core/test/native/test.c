#include <stdlib.h>
#include <ctype.h>
#include <stdarg.h>
#include <stdio.h>

struct a_t
{
   int a;
   int b;
};

struct b_t
{
   int c;
   struct a_t d;
};

struct c_t
{
   int a[3];
   float b[3];
};

int int_test(int a)
{
   return a + 3;
}

short short_test(short a)
{
   return a + 2;
}

char byte_test(char a)
{
   return a + 1;
}

char char_test(char a)
{
   return toupper(a);
}

long long_test(long a)
{
   return a + 4;
}

float float_test(float a)
{
   return a + 4.0f;
}

double double_test(double a)
{
   return a + 3.0;
}

int bool_test(int a)
{
   if (a)
   {
      return !a;
   }
   else
   {
      return a;
   }
}

char string_test(char *str)
{
   return str[1];
}

char *badval;

void bad_method(const char *str)
{
   badval = str;
   return;
}

char *ibreak(char *str)
{
   return badval;
}

int sum(int n, ...)
{
   int Sum = 0;
   va_list ptr;
   va_start(ptr, n);
   for (int i = 0; i < n; i++)
      Sum += va_arg(ptr, int);

   va_end(ptr);

   return Sum;
}

struct b_t slinc_test_modify(struct b_t b)
{
   b.d.a += 6;
   return b;
}

struct c_t slinc_test_addone(struct c_t c)
{
   for (int i = 0; i < 3; i++)
   {
      c.a[i] += 1;
      c.b[i] += 1;
   }

   return c;
}

void slinc_test_passstaticarr(int res[3])
{
   return;
}

int *slinc_test_getstaticarr()
{

   int *ret = malloc(sizeof(int) * 3);
   ret[0] = 1;
   ret[1] = 2;
   ret[2] = 3;

   return ret;
}

int slinc_two_structs(struct a_t a, struct a_t b)
{
   return a.a * b.a;
}

int slinc_upcall(int (*zptr)())
{
   return zptr();
}

int slinc_upcall_a_t(struct a_t (*zptr)())
{
   struct a_t a = zptr();
   return a.a + a.b;
}

struct a_t get_a_struct()
{
   struct a_t a;
   a.a = 3;
   a.b = 2;
   return a;
}

typedef struct a_t (*a_fn_ptr)();
a_fn_ptr slinc_fptr_ret()
{
   return get_a_struct;
}

int adder(int a, int b)
{
   return a + b;
}

typedef int (*adderfn)(int, int);
adderfn slinc_fptr_ret2()
{
   return adder;
}
