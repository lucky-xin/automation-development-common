package com.automation.development.common.annotation;

import java.lang.annotation.*;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 字段关联信息注解，该注解用于中间表之中。注解被关联表主键在中间表之中的字段列。反射操作关联关系用到
 * @date 2019-05-05 10:40
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RelToColumn {

}
