package com.automation.development.common.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 服务实现类成员变量名称
 * @date 2019-04-20 20:41
 */
@Data
@Accessors(chain = true)
public class ServiceImplField implements Comparable<ServiceImplField> {

    /**
     * 属性驼峰名称
     */
    private String propertyName;

    /**
     *
     */
    private String entryName;

    @Override
    public int compareTo(ServiceImplField o) {
        if ( o == null) {
            return 0;
        }

        if (this.getPropertyName().equals(o.getPropertyName())) {
            return 0;
        }

        return this.getPropertyName().hashCode() - o.getPropertyName().hashCode();
    }
}
