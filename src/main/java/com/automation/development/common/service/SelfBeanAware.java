package com.automation.development.common.service;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 用于设置自身bean aop对象接口
 * @date 2019-09-13 12:25
 */
public interface SelfBeanAware {

    /**
     * 设置自己的 aop 代理对象
     *
     * @param instance
     */
    void setSelfObj(Object instance);
}
