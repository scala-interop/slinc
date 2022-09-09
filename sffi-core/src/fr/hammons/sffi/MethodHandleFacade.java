package fr.hammons.sffi;

import java.lang.invoke.MethodHandle;

public class MethodHandleFacade {
	public static Object call(MethodHandle mh) throws Throwable {
		return mh.invoke();
	}

	public static Object call(MethodHandle mh, Object a) throws Throwable {
		return mh.invoke(a);
	}

	public static Object call(MethodHandle mh, Object a, Object b) throws Throwable {
		return mh.invoke(a, b);
	}

  public static Object call(MethodHandle mh, Object a, Object b, Object c) throws Throwable {
    return mh.invoke(a,b,c);
  }

	public static Object callVariadic(MethodHandle mh, Object... a) throws 
	Throwable {
		return mh.invokeWithArguments(a);
	}

	public static Object call(MethodHandle mh, Object a, Object b, Object c, Object d) throws Throwable {
		return mh.invoke(a,b,c,d);
	}

	public static Object call(MethodHandle mh, Object a, Object b, Object c, Object d, Object e) throws Throwable {
		return mh.invoke(a,b,c,d,e);
	}
  
}
