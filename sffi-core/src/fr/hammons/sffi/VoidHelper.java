package fr.hammons.sffi;

import java.lang.invoke.MethodType;

public class VoidHelper {
	public static MethodType methodTypeV(Class<?> ptype0, Class<?>... ptypes) {
		return MethodType.methodType(void.class, ptype0, ptypes);
	}

	public static MethodType methodTypeV() {
		return MethodType.methodType(void.class);
	}
}