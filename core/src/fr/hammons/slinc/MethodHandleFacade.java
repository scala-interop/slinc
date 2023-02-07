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
		return mh.invoke(sa, a, b);
	}

	public static int callExact(MethodHandle mh, int a) throws Throwable {
		return (int) mh.invokeExact(a);
	}

	public static Object call2(MethodHandle mh, Object a, Object b) throws Throwable {
		return mh.invoke(a, b);
	}

	public static Object call3(MethodHandle mh, Object a, Object b, Object c) throws Throwable {
		return mh.invoke(a, b, c);
	}

	public static Object call4(MethodHandle mh, Object a, Object b, Object c, Object d) throws Throwable {
		return mh.invoke(a, b, c, d);
	}

	public static Object call5(MethodHandle mh, Object a, Object b, Object c, Object d, Object e) throws Throwable {
		return mh.invoke(a, b, c, d, e);
	}

	public static Object call6(MethodHandle mh, Object a, Object b, Object c, Object d, Object e, Object f)
			throws Throwable {
		return mh.invoke(a, b, c, d, e, f);
	}

	public static Object call7(MethodHandle mh, Object a, Object b, Object c, Object d, Object e, Object f, Object g)
			throws Throwable {
		return mh.invoke(a, b, c, d, e, f, g);
	}

	public static Object call8(MethodHandle mh, Object a, Object b, Object c, Object d, Object e, Object f, Object g,
			Object h) throws Throwable {
		return mh.invoke(a, b, c, d, e, f, g, h);
	}

	public static Object call9(MethodHandle mh, Object a, Object b, Object c, Object d, Object e, Object f, Object g,
			Object h, Object i) throws Throwable {
		return mh.invoke(a, b, c, d, e, f, g, h, i);
	}

	public static Object call10(MethodHandle mh, Object a, Object b, Object c, Object d, Object e, Object f, Object g,
			Object h, Object i, Object j) throws Throwable {
		return mh.invoke(a, b, c, d, e, f, g, h, i, j);
	}

	public static Object call11(MethodHandle mh, Object a, Object b, Object c, Object d, Object e, Object f, Object g,
			Object h, Object i, Object j, Object k) throws Throwable {
		return mh.invoke(a, b, c, d, e, f, g, h, i, j, k);
	}

	public static Object call12(MethodHandle mh, Object a, Object b, Object c, Object d, Object e, Object f, Object g,
			Object h, Object i, Object j, Object k, Object l) throws Throwable {
		return mh.invoke(a, b, c, d, e, f, g, h, i, j, k, l);
	}

  public static Object call13(MethodHandle mh, Object a, Object b, Object c,
  Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m) throws Throwable {
    return mh.invoke(a,b,c,d,e,f,g,h,i,j,k,l);
  }

  public static Object call14(MethodHandle mh, Object a, Object b, Object c,
  Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n) throws Throwable {
    return mh.invoke(a,b,c,d,e,f,g,h,i,j,k,l,m,n);
  }

  public static Object call15(MethodHandle mh, Object a, Object b, Object c, 
  Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o) throws Throwable {
    return mh.invoke(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o);
  }

  public static Object call16(MethodHandle mh, Object a, Object b, Object c,
  Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p) throws Throwable {
    return mh.invoke(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p);
  }

  public static Object call17(MethodHandle mh, Object a, Object b, Object c,
  Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q) throws Throwable {
    return mh.invoke(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q);
  }

  public static Object call18(MethodHandle mh, Object a, Object b, Object c, 
  Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r) throws Throwable {
    return mh.invoke(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r);
  }

  public static Object call19(MethodHandle mh, Object a, Object b, Object c, 
  Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s) throws Throwable {
    return mh.invoke(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s);
  }

  public static Object call20(MethodHandle mh, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t) throws Throwable {
    return mh.invoke(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t);
  }

  public static Object call21(MethodHandle mh, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u) throws Throwable {
    return mh.invoke(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u);
  }

  public static Object call22(MethodHandle mh, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v) throws Throwable {
    return mh.invoke(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v);
  }

	public static Object callVariadic(MethodHandle mh, Object... a) throws Throwable {
		return mh.invokeWithArguments(a);
	}
}
