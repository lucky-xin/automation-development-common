package com.auto.development.common.model;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: Model缓存类
 * @date 2019-06-01 19:33
 */
public final class ModelFactory {

    private static ModelFactory instance = null;

    private ConcurrentHashMap<Class<?>, Object> objectCache = new ConcurrentHashMap<>(100);

    private ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>(500);

    private ModelFactory() {

    }

    public static ModelFactory getInstance() {
        if (instance == null) {
            synchronized (ModelFactory.class) {
                if (instance == null) {
                    instance = new ModelFactory();
                }
            }
        }
        return instance;
    }

    public <T> T getObject(Class<T> clazz) throws IllegalAccessException, InstantiationException {
        Object obj = objectCache.get(clazz);
        if (obj == null) {
            synchronized (clazz) {
                obj = objectCache.get(clazz);
                if (obj == null) {
                    obj = clazz.newInstance();
                    objectCache.put(clazz, obj);
                }
            }
        }
        return (T) obj;
    }

    public Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        String methodCacheKey = clazz.getCanonicalName() + "." + methodName;
        Method method = methodCache.get(methodCacheKey);
        if (method == null) {
            synchronized (clazz) {
                method = methodCache.get(methodCacheKey);
                if (method == null) {
                    method = clazz.getMethod(methodName, parameterTypes);
                    method.setAccessible(true);
                    methodCache.put(methodCacheKey, method);
                }
            }
        }
        return method;
    }
}
