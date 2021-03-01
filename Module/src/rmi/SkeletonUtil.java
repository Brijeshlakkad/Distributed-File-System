package rmi;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @author Brijesh Lakkad
 * @version 1.0
 */
public class SkeletonUtil {
    public static boolean isAssignableFromRemote(Class<?> c){
        Method[] declaredMethods = c.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            List<Class<?>> exceptionTypes = Arrays.asList(declaredMethod.getExceptionTypes());
            if (!exceptionTypes.contains(RMIException.class)) {
                throw new Error();
            }
        }
        return true;
    }
}
