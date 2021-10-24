package io.gitlab.mhammons.slinc;

import java.lang.invoke.MethodType;

public class VoidHelper {
    public static Class<?> v = void.class;
    public static MethodType methodTypeV(Class<?> ptype0, Class<?>... ptypes) { return MethodType.methodType(void.class, ptype0, ptypes); }
    public static MethodType methodTypeV(){ return MethodType.methodType(void.class);}
    public static MethodType methodTypeV(Class<?> ptype0) { return MethodType.methodType(void.class, ptype0); }
}