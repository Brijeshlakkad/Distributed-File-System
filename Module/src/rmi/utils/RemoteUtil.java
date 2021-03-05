package rmi.utils;

import rmi.RMIException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @author Brijesh Lakkad
 * @version 1.0
 */
public class RemoteUtil {
    private static List<String> d_LocalMethods;

    static {
        String[] l_LocalMethods = new String[]{"equals", "hashCode", "toString"};
        d_LocalMethods = Arrays.asList(l_LocalMethods);
    }

    public static boolean isAssignableFromRemote(Class<?> c) {
        Method[] declaredMethods = c.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            List<Class<?>> exceptionTypes = Arrays.asList(declaredMethod.getExceptionTypes());
            if (!exceptionTypes.contains(RMIException.class)) {
                throw new Error();
            }
        }
        return true;
    }

    public static boolean isRemoteMethod(Class<?> c, Method method) {
        for (Method m : c.getMethods()) {
            if (m.equals(method)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLocalMethod(Class<?> p_class, Method p_method, Object[] p_arguments) {
        // check the outputs of the method.getName()
        if (d_LocalMethods.contains(p_method.getName())) {
            try {
                // Create an array to match the type with method's signature
                Class<?>[] l_parameterTypes;
                if (p_arguments != null && p_arguments.length > 0) {
                    l_parameterTypes = new Class[p_arguments.length];
                    for (int l_argIndex = 0; l_argIndex < p_arguments.length; l_argIndex++) {
                        l_parameterTypes[l_argIndex] = Object.class;
                    }
                } else {
                    l_parameterTypes = new Class[0];
                }
                Method l_foundMethod = p_class.getMethod(p_method.getName(), l_parameterTypes);

                // Check if the method is overridden method
                return hasOverriddenMethod(l_foundMethod, p_class);
            } catch (NoSuchMethodException p_e) {
                return false;
            }
        }
        return false;
    }

    public static boolean hasOverriddenMethod(Method p_method, Class<?> p_class) {
        return p_class == p_method.getDeclaringClass();
    }
}
