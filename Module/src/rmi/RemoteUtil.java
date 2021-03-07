package rmi;

import rmi.RMIException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * This utility class provides the common methods used throughout the <code>rmi</code> package. Note that, an instance
 * of this class cannot be created.
 *
 * @author Brijesh Lakkad
 * @version 1.0
 */
public class RemoteUtil {
    /**
     * List of the name of the method which can not be called on a remote object.
     */
    private static final List<String> d_LocalMethods;

    static {
        String[] l_LocalMethods = new String[]{"equals", "hashCode", "toString"};
        d_LocalMethods = Arrays.asList(l_LocalMethods);
    }

    /**
     * Throws an exception upon trying to create an instance of this class.
     */
    public RemoteUtil() {
        throw new UnsupportedOperationException("An instance of this class cannot be created!");
    }

    /**
     * Checks if all the methods of the provided <code>Class</code> throws the <code>RMIException</code>.
     *
     * @param c <code>Class</code> which will be validated.
     * @return True if all the methods throws the required exception.
     * @throws Error If any of the methods haven't declared throwing the required exception in the method signature.
     */
    public static boolean isAssignableFromRemote(Class<?> c) throws Error {
        Method[] declaredMethods = c.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            List<Class<?>> exceptionTypes = Arrays.asList(declaredMethod.getExceptionTypes());
            if (!exceptionTypes.contains(RMIException.class)) {
                throw new Error();
            }
        }
        return true;
    }

    /**
     * Checks if the method is remote method or not.
     *
     * @param c      Class which will be used to check if the provided method belongs to it.
     * @param method Method to be checked.
     * @return True if the method is remote method; false otherwise.
     */
    public static boolean isRemoteMethod(Class<?> c, Method method) {
        for (Method m : c.getMethods()) {
            if (m.equals(method)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the method is local. It uses the list of local methods.
     *
     * @param p_class     <code>Class</code> providing the methods.
     * @param p_method    Method to be checked.
     * @param p_arguments Argument list to check the method signature.
     * @return True if the method is local; false otherwise.
     */
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

    /**
     * Checks if the method is overridden or not.
     *
     * @param p_method Method to be checked.
     * @param p_class  <code>Class</code> providing the methods.
     * @return True if the method overrides the base class method; false otherwise.
     */
    public static boolean hasOverriddenMethod(Method p_method, Class<?> p_class) {
        return p_class == p_method.getDeclaringClass();
    }
}
