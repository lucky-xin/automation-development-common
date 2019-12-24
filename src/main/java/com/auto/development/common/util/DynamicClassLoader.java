package com.auto.development.common.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 动态加载器
 * @date 2019-05-12 13:04
 */
public class DynamicClassLoader extends ClassLoader {

    /**
     * 动态加载.class文件
     *
     * @param name          java 类名称
     * @param classFullPath .class绝对路径
     * @return
     * @throws IOException
     */
    public Class<?> loadClass(String name, String classFullPath) throws IOException {
        try {
            Class<?> cls = findLoadedClass(name);
            if (cls != null) {
                return cls;
            }
        } catch (Exception ignore) {

        }
        try (FileInputStream fis = new FileInputStream(classFullPath);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            byte[] data = baos.toByteArray();
            return defineClass(null, data, 0, data.length);
        }
    }
}
