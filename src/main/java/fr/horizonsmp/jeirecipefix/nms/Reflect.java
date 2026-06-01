package fr.horizonsmp.jeirecipefix.nms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class Reflect {

    private Reflect() {
    }

    static Class<?> clazz(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Missing class " + name, e);
        }
    }

    /**
     * Resolves the first of {@code names} that exists, in order. Lets a single call site span versions
     * where a Mojang-mapped class was renamed (e.g. {@code ResourceLocation} became {@code Identifier} in 26.x).
     */
    static Class<?> clazzAny(String... names) {
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
                // try the next candidate
            }
        }
        throw new IllegalStateException("Missing class (tried " + String.join(", ", names) + ")");
    }

    /** Resolves the first method named in {@code names} (in order) with the given params; for renamed members. */
    static Method methodAny(Class<?> owner, String[] names, Class<?>... params) {
        for (String name : names) {
            try {
                return method(owner, name, params);
            } catch (IllegalStateException ignored) {
                // try the next candidate name
            }
        }
        throw new IllegalStateException("Missing method " + owner.getName() + "#" + String.join("|", names));
    }

    /** Resolves the first constructor whose params match one of {@code paramSets} (in order); for changed signatures. */
    static Constructor<?> ctorAny(Class<?> owner, Class<?>[]... paramSets) {
        for (Class<?>[] params : paramSets) {
            try {
                Constructor<?> c = owner.getDeclaredConstructor(params);
                c.setAccessible(true);
                return c;
            } catch (NoSuchMethodException ignored) {
                // try the next candidate signature
            }
        }
        throw new IllegalStateException("Missing constructor " + owner.getName() + "(...)");
    }

    static Constructor<?> ctor(Class<?> owner, Class<?>... params) {
        try {
            Constructor<?> c = owner.getDeclaredConstructor(params);
            c.setAccessible(true);
            return c;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Missing constructor " + owner.getName() + "(...)", e);
        }
    }

    static Method method(Class<?> owner, String name, Class<?>... params) {
        try {
            Method m = owner.getMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException ignored) {
            for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
                try {
                    Method m = c.getDeclaredMethod(name, params);
                    m.setAccessible(true);
                    return m;
                } catch (NoSuchMethodException ignored2) {
                    // keep walking up
                }
            }
            throw new IllegalStateException("Missing method " + owner.getName() + "#" + name);
        }
    }

    static Object call(Method m, Object target, Object... args) {
        try {
            return m.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Invoke failed: " + m, e);
        }
    }

    static Object getField(Object target, String name) {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException ignored) {
                // keep walking up
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("Missing field " + name + " on " + target.getClass());
    }

    static Object staticField(Class<?> owner, String name) {
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Missing static field " + owner.getName() + "#" + name, e);
        }
    }
}
