package com.automation.development.common.util;


import com.automation.development.common.activerecord.XModel;
import com.automation.development.common.annotation.RelationColumn;
import com.automation.development.common.annotation.RelationToColumn;
import com.automation.development.common.service.impl.XServiceImpl;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.toolkit.AopUtils;
import com.xin.utils.BeanUtil;
import com.xin.utils.CollectionUtil;
import com.xin.utils.StringUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 关联关系工具类
 * @date 2019-05-07 17:06
 */
public class RelationInfoUtil {

    public static <T extends XModel> List<Long> selectPkColumns(IService<T> xService, String selectColumn, String conditionColumn, Object conditionValue) {
        Function<Object, Long> function = (input) -> {
            return StringUtil.toLong(input, 0);
        };
        List<Long> objs = xService.listObjs(Wrappers.<T>query().select(selectColumn).eq(conditionColumn, conditionValue), function);
        return objs;
    }

    public static <T extends XModel> boolean saveMiddleEntry(IService<T> middleEntryService, Long relationId, Collection<Long> relationToIds) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Assert.notNull(middleEntryService, "iService must not be null." );
        Assert.isTrue(XServiceImpl.class.isAssignableFrom(middleEntryService.getClass()), "%s必须继承%s", middleEntryService.getClass().getCanonicalName(), XServiceImpl.class.getCanonicalName());
        Assert.notNull(relationId, "relationId must not be null.");
        if (relationId == null || CollectionUtil.isEmpty(relationToIds)) {
            return false;
        }

        Class<?> serviceClass = AopUtils.getTargetObject(middleEntryService).getClass();
        Class<IService<T>> iServiceClass = (Class<IService<T>>) serviceClass.getGenericInterfaces()[0];
        ParameterizedType parameterizedType = (ParameterizedType) iServiceClass.getGenericInterfaces()[0];
        Class<T> middleEntryClass = (Class<T>) (parameterizedType.getActualTypeArguments()[0]);
        Field[] fields = middleEntryClass.getDeclaredFields();
        if (CollectionUtil.isEmpty(fields)) {
            return false;
        }
        String relationColumn = null;
        String relationToColumn = null;
        String relationProperty = null;
        String relationToProperty = null;
        for (Field field : fields) {
            RelationColumn relationColumnAnnotation = field.getAnnotation(RelationColumn.class);
            RelationToColumn relationToColumnAnnotation = field.getAnnotation(RelationToColumn.class);
            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null && !tableField.exist()) {
                continue;
            }
            if (relationColumnAnnotation != null) {
                Assert.isNull(relationColumn, "不能含有多个RelationColumn");
                relationProperty = field.getName();
                relationColumn = FieldHelper.getColumnName(field.getName());
            }

            if (relationToColumnAnnotation != null) {
                Assert.isNull(relationToColumn, "不能含有多个RelationToColumn");
                relationToProperty = field.getName();
                relationToColumn = FieldHelper.getColumnName(field.getName());
            }
        }
        if (StringUtil.isEmpty(relationProperty) || StringUtil.isEmpty(relationToProperty)) {
            return false;
        }

        Method method1 = middleEntryClass.getMethod(BeanUtil.getSetMethodName(relationProperty), Long.class);
        Method method2 = middleEntryClass.getMethod(BeanUtil.getSetMethodName(relationToProperty), Long.class);
        Set<T> middleEntrySet = new HashSet<>();
        for (Long relationToId : relationToIds) {
            if (relationToId == null) {
                continue;
            }
            T middleEntry = middleEntryClass.newInstance();
            method1.invoke(middleEntry, relationId);
            method2.invoke(middleEntry, relationToId);
            middleEntrySet.add(middleEntry);
        }
        Wrapper<T> wrapper = Wrappers.<T>query().eq(relationColumn, relationId);
        List<T> existMiddleEntries = middleEntryService.list(wrapper);
        middleEntrySet.removeIf(classInfoStudent -> existMiddleEntries.contains(classInfoStudent));
        middleEntryService.saveBatch(middleEntrySet);
        return true;
    }

}
