package com.automation.development.common.annotation;

import java.lang.annotation.*;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 字段关联信息注解。该注解用于表明某个对象之中某个属性的关联关系
 * @date 2019-05-05 10:40
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RelInfo {

    /**
     * 多对多或者一对多关联中间表名称
     */
    String midTable() default "";

    /**
     * 中间表实体类名称如监管机构-学校为SupervisorSchool
     */
    String midEntry() default "";

    /**
     * 中间表变量名称如监管机构-学校为supervisorSchool
     */
    String midProperty() default "";

    /**
     * 上一级或者说关联对象在中间表里面的列名。默认[关联表名称+_id]。如监管机构-学校关联关系之中此值为supervisor_id
     */
    String relColumn() default "";

    /**
     * 保留字段
     * 如表名SupervisorSchool则此字段为supervisor-school
     */
    String relUri() default "";

    /**
     * 上一级或者说关联对象表名。如监管机构-学校关联关系之中此值为supervisor
     */
    String relTable();

    /**
     * 上一级或者说关联对象表的主键列名。
     */
    String relTablePk() default "id";

    /**
     * 上一级或者说关联对象类名。如如监管机构-学校关联关系之中此值为Supervisor
     */
    String relEntry() default "";

    /**
     * 上一级或者说关联对象变量名。如如监管机构-学校关联关系之中此值为supervisor
     */
    String relProperty() default "";

    /**
     * 下级或者说被关联对象表名。如如监管机构-学校关联关系之中此值为school
     */
    String relToTable();

    /**
     * 下级级或者说被关联对象在中间表里面的列名。默认为[被关联表名称+_id]。如监管机构-学校关联关系之中此值为school_id
     */
    String relToColumn() default "";

    /**
     * 保留字段
     */
    String relToUri() default "";

    /**
     * 下级级或者说被关联对象类名称。如监管机构-学校关联关系之中此值为School
     */
    String relToEntry() default "";

    /**
     * 下级级或者说被关联对象变量名称。如监管机构-学校关联关系之中此值为school
     */
    String relToProperty() default "";

    /**
     * 下级级或者说被关联对象表的主键id
     */
    String relToTablePk() default "id";

    /**
     * 是否可达。可达表示在当前的对象之中是否能获取到关联对象的详细信息。
     * 获取到详细信息就是不管嵌套结构有多深都把被关联对象所有关联的对象全都查询出来。
     * 如果两个关联的对象要拿互相获取到对方的详细信息就会进入无限循环之中，因此用isReachable来控制。
     * isReachable为true则能获取到该关联对象所有关联信息。否则只能获取到基本信息
     */
    boolean isReachable() default true;

    /**
     * 当执行删除操作时是否删除此关联关系
     */
    boolean deleteRelation() default true;
}
