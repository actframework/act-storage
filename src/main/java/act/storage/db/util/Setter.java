package act.storage.db.util;

import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public abstract class Setter {
    public abstract void set(Object obj, Object val);

    private static class MethodSetter extends Setter {
        private Method method;
        MethodSetter(Method m) {
            m.setAccessible(true);
            this.method = m;
        }

        @Override
        public void set(Object obj, Object val) {
            try {
                method.invoke(obj, val);
            } catch (Exception e) {
                throw E.unexpected(e);
            }
        }
    }

    private static class FieldSetter extends Setter {
        private Field field;
        FieldSetter(Field f) {
            f.setAccessible(true);
            this.field = f;
        }

        @Override
        public void set(Object obj, Object val) {
            try {
                field.set(obj, val);
            } catch (Exception e) {
                throw E.unexpected(e);
            }
        }
    }

    public static Setter of(Method method) {
        return new MethodSetter(method);
    }

    public static Setter of(Field field) {
        return new FieldSetter(field);
    }

    public static Setter probe(Class c, String fieldName) {
        // try setXxx(value) first
        String mn = S.builder("set").append(S.capFirst(fieldName)).toString();
        try {
            Method m = c.getMethod(mn);
            return Setter.of(m);
        } catch (NoSuchMethodException e) {
            // ignore
        }

        // try xxx(value)
        try {
            Method m = c.getMethod(fieldName);
            return Setter.of(m);
        } catch (NoSuchMethodException e) {
            // ignore
        }

        try {
            return Setter.of(c.getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            throw E.unexpected(e);
        }
    }
}
