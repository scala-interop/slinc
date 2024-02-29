#include <stdlib.h>
#include <string.h>
#include <stdio.h>

int add(int a,int b) {
    return a + b;
}

int add_by_callback(int a, int (*f)(void)) {
    int b = f();
    return a + b;
}

char* add_str(char* a, char* b) {
    int size = strlen(a) + strlen(b) + 1;
    char *buf = malloc (size);
    snprintf(buf, size, "%s%s", a, b);
    return buf;
}
