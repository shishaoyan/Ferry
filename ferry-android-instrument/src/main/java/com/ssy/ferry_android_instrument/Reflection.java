package com.ssy.ferry_android_instrument;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 2019-09-26
 *
 * @author Mr.S
 */
public abstract class Reflection {
    public static <T> T getStaticFieldValue(Class<?> klass, String name) {
        if (null != klass && null != name) {
            try {
                Field field = getField(klass, name);
                if (null != field) {
                    field.setAccessible(true);
                    return (T) field.get(klass);
                }
            } catch (Throwable var3) {
                Log.w("ferry", "get field " + name + " of " + klass + " error", var3);
            }
        }

        return null;
    }

    public static boolean setStaticFieldValue(Class<?> klass, String name, Object value) {
        if (null != klass && null != name) {
            try {
                Field field = getField(klass, name);
                if (null != field) {
                    field.setAccessible(true);
                    field.set(klass, value);
                    return true;
                }
            } catch (Throwable var4) {
                Log.w("ferry", "set field " + name + " of " + klass + " error", var4);
            }
        }

        return false;
    }

    public static <T> T getFieldValue(Object obj, String name) {
        if (null != obj && null != name) {
            try {
                Field field = getField(obj.getClass(), name);
                if (null != field) {
                    field.setAccessible(true);
                    return (T) field.get(obj);
                }
            } catch (Throwable var3) {
                Log.w("ferry", "get field " + name + " of " + obj + " error", var3);
            }
        }

        return null;
    }

    public static <T> T getFieldValue(Object obj, Class<?> type) {
        if (null != obj && null != type) {
            try {
                Field field = getField(obj.getClass(), type);
                if (null != field) {
                    field.setAccessible(true);
                    return (T) field.get(obj);
                }
            } catch (Throwable var3) {
                Log.w("ferry", "get field with type " + type + " of " + obj + " error", var3);
            }
        }

        return null;
    }

    public static boolean setFieldValue(Object obj, String name, Object value) {
        if (null != obj && null != name) {
            try {
                Field field = getField(obj.getClass(), name);
                if (null != field) {
                    field.setAccessible(true);
                    field.set(obj, value);
                    return true;
                }
            } catch (Throwable var4) {
                Log.w("ferry", "set field " + name + " of " + obj + " error", var4);
            }
        }

        return false;
    }

    public static <T> T newInstance(String className, Object... args) {
        try {
            return newInstance(Class.forName(className), args);
        } catch (ClassNotFoundException var3) {
            Log.w("ferry", "new instance of " + className + " error", var3);
            return null;
        }
    }

    public static <T> T newInstance(Class<?> clazz, Object... args) {
        Constructor<?>[] ctors = clazz.getDeclaredConstructors();
        Constructor[] var3 = ctors;
        int var4 = ctors.length;

        label34:
        for(int var5 = 0; var5 < var4; ++var5) {
            Constructor<?> ctor = var3[var5];
            Class<?>[] types = ctor.getParameterTypes();
            if (types.length == args.length) {
                for(int i = 0; i < types.length; ++i) {
                    if (null != args[i] && !types[i].isAssignableFrom(args[i].getClass())) {
                        continue label34;
                    }
                }

                try {
                    ctor.setAccessible(true);
                    return (T) ctor.newInstance(args);
                } catch (Throwable var9) {
                    Log.w("ferry", "Invoke constructor " + ctor + " error", var9);
                    return null;
                }
            }
        }

        return null;
    }

    public static <T> T invokeStaticMethod(Class<?> klass, String name) {
        return invokeStaticMethod(klass, name, new Class[0], new Object[0]);
    }

    public static <T> T invokeStaticMethod(Class<?> klass, String name, Class[] types, Object[] args) {
        if (null != klass && null != name && null != types && null != args && types.length == args.length) {
            try {
                Method method = getMethod(klass, name, types);
                if (null != method) {
                    method.setAccessible(true);
                    return (T) method.invoke(klass, args);
                }
            } catch (Throwable var5) {
                Log.w("ferry", "Invoke " + name + "(" + Arrays.toString(types) + ") of " + klass + " error", var5);
            }
        }

        return null;
    }

    public static <T> T invokeMethod(Object obj, String name) {
        return invokeMethod(obj, name, new Class[0], new Object[0]);
    }

    public static <T> T invokeMethod(Object obj, String name, Class[] types, Object[] args) {
        if (null != obj && null != name && null != types && null != args && types.length == args.length) {
            try {
                Method method = getMethod(obj.getClass(), name, types);
                if (null != method) {
                    method.setAccessible(true);
                    return (T) method.invoke(obj, args);
                }
            } catch (Throwable var5) {
                Log.w("ferry", "Invoke " + name + "(" + Arrays.toString(types) + ") of " + obj + " error", var5);
            }
        }

        return null;
    }

    public static Field getField(Class<?> klass, String name) {
        try {
            return klass.getDeclaredField(name);
        } catch (NoSuchFieldException var4) {
            Class<?> parent = klass.getSuperclass();
            return null == parent ? null : getField(parent, name);
        }
    }

    public static Field getField(Class<?> klass, Class<?> type) {
        Field[] fields = klass.getDeclaredFields();
        if (fields.length <= 0) {
            Class<?> parent = klass.getSuperclass();
            return null == parent ? null : getField(parent, type);
        } else {
            Field[] var3 = fields;
            int var4 = fields.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                Field field = var3[var5];
                if (field.getType() == type) {
                    return field;
                }
            }

            return null;
        }
    }

    private static Method getMethod(Class<?> klass, String name, Class<?>[] types) {
        try {
            return klass.getDeclaredMethod(name, types);
        } catch (NoSuchMethodException var5) {
            Class<?> parent = klass.getSuperclass();
            return null == parent ? null : getMethod(parent, name, types);
        }
    }

    private Reflection() {
    }
}

