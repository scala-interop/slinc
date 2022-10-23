package fr.hammons.slinc;

import java.lang.invoke.MethodHandle;

public class MethodHandleFacade {
  public static Object call0(MethodHandle mh) throws Throwable {
    return mh.invoke();
  }

  public static Object call1(MethodHandle mh, Object a) throws Throwable {
    return mh.invoke(a);
  }

  public static int call1Int(MethodHandle mh, int a) throws Throwable {
    return (int) mh.invoke(a);
  }

  public static Object call2Int(MethodHandle mh, Object sa, int a, int b) throws Throwable {
    return mh.invoke(sa,a,b);
  }

  public static int callExact(MethodHandle mh, int a) throws Throwable {
    return (int) mh.invokeExact(a);
  }

  public static Object call2(MethodHandle mh, Object a, Object b) throws Throwable {
    return mh.invoke(a,b);
  }

  public static Object call3(MethodHandle mh, Object a, Object b, Object c) throws Throwable {
    return mh.invoke(a,b,c);
  }

  public static Object call4(MethodHandle mh, Object a, Object b, Object c, Object d) throws Throwable {
    return mh.invoke(a,b,c,d);
  }

  public static Object call5(MethodHandle mh, Object a, Object b, Object c, Object d, Object e) throws Throwable {
    return mh.invoke(a,b,c,d,e);
  }

  public static Object call6(MethodHandle mh, Object a, Object b, Object c, Object d, Object e, Object f) throws Throwable {
    return mh.invoke(a,b,c,d,e,f);
  }
  
  public static Object call7(MethodHandle mh, Object a, Object b, Object c, Object d, Object e, Object f, Object g) throws Throwable {
    return mh.invoke(a,b,c,d,e,f,g);
  }

  public static Object callVariadic(MethodHandle mh, Object... a) throws Throwable {
    return mh.invokeWithArguments(a);
  }
}
