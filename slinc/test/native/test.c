struct a_t{
   int a;
   int b;
};

struct b_t{
   int c;
   struct a_t d;
};

struct b_t slinc_test_modify(struct b_t b) {
   b.d.a += 6;
   return b;
}