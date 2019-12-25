package com.auto.development.common.activerecord;

import com.auto.development.common.util.TableHelper;
import com.auto.development.common.annotation.RelInfo;
import com.auto.development.common.model.ModelFactory;
import com.auto.development.common.service.DistributeIdService;
import com.auto.development.common.util.FieldHelper;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import com.xin.utils.BeanUtil;
import com.xin.utils.CollectionUtil;
import com.xin.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 拓展Model，反射获取关联字段查询数据库，并且设置该字段值
 * @date 2019-05-05 10:20
 */
@Slf4j
public abstract class XModel<T extends XModel<?>> extends Model<T> {

    @TableField(exist = false)
    private String tableName;

    private String getTableName() {
        if (StringUtil.isEmpty(tableName)) {
            synchronized (this) {
                if (StringUtil.isEmpty(tableName)) {
                    TableName tableNameAnn = getClass().getAnnotation(TableName.class);
                    if (tableName != null && !StringUtil.isEmpty(tableNameAnn.value())) {
                        tableName = tableNameAnn.value();
                    }
                    tableName = TableHelper.getColumnName(getClass().getSimpleName());
                }
            }
        }

        return tableName;
    }

    private void handle(XModel<T> entry, Consumer<ExtRelInfo> consumer) {

        String tableName = getTableName();

        for (Field declaredField : getClass().getDeclaredFields()) {
            TableField tableFieldAnnotation = declaredField.getAnnotation(TableField.class);
            RelInfo relInfo = declaredField.getAnnotation(RelInfo.class);
            if (tableFieldAnnotation == null || tableFieldAnnotation.exist() || relInfo == null) {
                continue;
            }
            String reTable = relInfo.relTable();
            String relToTable = relInfo.relToTable();
            String relEntry = StringUtil.isEmpty(relInfo.relEntry()) ? FieldHelper.getEntryName(reTable) : relInfo.relEntry();
            String relToEntry = StringUtil.isEmpty(relInfo.relToEntry()) ? FieldHelper.getEntryName(relToTable) : relInfo.relToEntry();
            ExtRelInfo<T> extRelInfo = new ExtRelInfo<T>().setModel(entry).setReachable(relInfo.reachable());

            Type genericType = declaredField.getGenericType();
            try {
                Class<?> relEntryClass = null;
                Class<?> relToEntryClass = null;
                Class<?> fieldActualClass = declaredField.getType();
                boolean isCollection = Collection.class.isAssignableFrom(declaredField.getType());
                if (isCollection && (genericType instanceof ParameterizedType)) {
                    // 此model关联其他多个model
                    Type[] types = ((ParameterizedType) genericType).getActualTypeArguments();
                    boolean shouldReturn = types.length != 1
                            || !(types[0] instanceof Class)
                            || !XModel.class.isAssignableFrom((Class<?>) types[0]);
                    if (shouldReturn) {
                        continue;
                    }
                    fieldActualClass = (Class<?>) types[0];
                }

                if (tableName.equals(reTable)) {
                    relEntryClass = getClass();
                    relToEntryClass = fieldActualClass;
                } else {
                    relToEntryClass = getClass();
                    relEntryClass = fieldActualClass;
                }

                // 如果没有继承自Model则跳过
                if (!(XModel.class.isAssignableFrom(relToEntryClass)
                        && XModel.class.isAssignableFrom(relEntryClass))) {
                    continue;
                }

                if (isCollection) {
                    // 为多对多关系
                    Assert.notEmpty(relInfo.midTable(), "middleTable must not be null.");
                    String middleEntry = StringUtil.isEmpty(relInfo.midEntry()) ?
                            FieldHelper.getEntryName(relInfo.midTable()) : relInfo.midEntry();
                    Class<?> middleEntryClass = getEntryClass(middleEntry);
                    // 如果没有继承自Model则跳过
                    if (!XModel.class.isAssignableFrom(middleEntryClass)) {
                        continue;
                    }
                    String middleProperty = StringUtil.isEmpty(relInfo.midProperty()) ?
                            FieldHelper.getPropertyName(relInfo.midTable()) : relInfo.midProperty();
                    extRelInfo.setMidEntryClass((Class<T>) middleEntryClass)
                            .setMidEntry(middleEntry)
                            .setMidProperty(middleProperty)
                            .setMidTable(relInfo.midTable())
                            .setMany(true)
                            .setFieldActualClass((Class<T>) fieldActualClass);
                }
                declaredField.setAccessible(true);
                String relProperty = StringUtil.isEmpty(relInfo.relProperty()) ?
                        FieldHelper.getPropertyName(reTable) : relInfo.relProperty();

                String relToProperty = StringUtil.isEmpty(relInfo.relToProperty()) ?
                        FieldHelper.getPropertyName(relToTable) : relInfo.relToProperty();
                String relColumn = StringUtil.isEmpty(relInfo.relColumn()) ? reTable + "_id" : relInfo.relColumn();
                String relToColumn = StringUtil.isEmpty(relInfo.relToColumn()) ? relToTable + "_id" : relInfo.relToColumn();
                extRelInfo.setDeclaredField(declaredField)
                        .setRelEntryClass((Class<T>) relEntryClass)
                        .setRelToEntryClass((Class<T>) relToEntryClass);

                extRelInfo.setRelTable(reTable)
                        .setRelEntry(relEntry)
                        .setRelProperty(relProperty)
                        .setRelColumn(relColumn)
                        .setRelTablePk(relInfo.relTablePk())
                        .setRelToTable(relToTable)
                        .setRelToEntry(relToEntry)
                        .setRelToProperty(relToProperty)
                        .setRelToColumn(relToColumn)
                        .setRelToTablePk(relInfo.relToTablePk());
                consumer.accept(extRelInfo);
            } catch (ClassNotFoundException e) {
                log.error("找不到class", e);
            }
        }
    }

    private void addPrimaryKey(XModel<T> xModel, DistributeIdService distributeIdService) {
        if (xModel == null) {
            return;
        }
        try {
            TableInfo tableInfo = SqlHelper.table(xModel.getClass());
            if (xModel.pkVal() == null) {
                Method method = ModelFactory.getInstance().getMethod(xModel.getClass(),
                        BeanUtil.getSetMethodName(tableInfo.getKeyProperty()), Long.class);
                method.invoke(xModel, distributeIdService.nextId(tableInfo.getTableName(), 1));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 插入（字段选择插入）
     */
    public boolean insertWithRelation(DistributeIdService distributeIdService) {
        Assert.notNull(distributeIdService, "DistributeIdService must not be null.");
        SqlSession sqlSession = sqlSession();
        try {
            XModel<T> xModel = this;
            addPrimaryKey(xModel, distributeIdService);
            Consumer<ExtRelInfo> consumer = (extRelInfo) -> {
                Field declaredField = extRelInfo.getDeclaredField();
                // 为Collection泛型成员
                try {
                    // 获取关联成员变量的值
                    String getMethodName = BeanUtil.getGetMethodName(declaredField.getName());
                    Method getMethod = ModelFactory.getInstance().getMethod(getClass(), getMethodName);
                    Object fieldValue = getMethod.invoke(xModel);
                    if (fieldValue == null) {
                        return;
                    }

                    if (extRelInfo.getIsMany()) {
                        // 此model关联其他多个model
                        boolean isRelationToEntry = extRelInfo.getFieldActualClass()
                                .equals(extRelInfo.getRelToEntryClass());
                        Collection<T> entityList = (Collection<T>) fieldValue;
                        // 把成员对象存入数据库
                        save(distributeIdService, entityList);
                        // 获取关联对象id，并插入中间表
                        Set<Serializable> ids = entityList.stream().map(item -> item.pkVal()).collect(Collectors.toSet());
                        String relToEntry = extRelInfo.getRelEntry();
                        String relEntry = extRelInfo.getRelToEntry();
                        if (isRelationToEntry) {
                            relEntry = extRelInfo.getRelEntry();
                            relToEntry = extRelInfo.getRelToEntry();
                        }
                        // 反射获取中间表关联id创建关联关系对象
                        Class<T> midEntryClass = extRelInfo.getMidEntryClass();
                        Method method1 = ModelFactory.getInstance().getMethod(midEntryClass, "set" + relEntry + "Id", Long.class);
                        Method method2 = ModelFactory.getInstance().getMethod(midEntryClass, "set" + relToEntry + "Id", Long.class);
                        List<T> middleModels = new ArrayList<>(ids.size());
                        Object[] args = new Object[]{xModel.pkVal()};
                        for (Serializable id : ids) {
                            T middleModel = midEntryClass.newInstance();
                            method1.invoke(middleModel, args);
                            Object[] params = new Object[]{id};
                            method2.invoke(middleModel, params);
                            middleModels.add(middleModel);
                        }
                        save(distributeIdService, middleModels);
                    } else if (XModel.class.isAssignableFrom(declaredField.getType())) {
                        if (getClass().getSimpleName().equals(extRelInfo.getRelEntry())) {
                            // 此model关联其他1个model
                            String methodName = BeanUtil.getSetMethodName(declaredField.getName() + "Id");
                            Method setFieldIdMethod = ModelFactory.getInstance().getMethod(getClass(), methodName, Long.class);
                            XModel fieldModel = (XModel) fieldValue;
                            addPrimaryKey(fieldModel, distributeIdService);
                            Object[] args = new Object[]{fieldModel.primaryKey()};
                            setFieldIdMethod.invoke(xModel, args);
                            fieldModel.insertWithRelation(distributeIdService);
                        }
                    }
                } catch (IllegalAccessException | InstantiationException e) {
                    log.error("反射创建对象异常", e);
                    throw new RuntimeException(e);
                } catch (NoSuchMethodException e) {
                    log.error("反射获取方法异常", e);
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    log.error("反射调用方法异常", e);
                    throw new RuntimeException(e);
                }
            };
            handle(this, consumer);
            return SqlHelper.retBool(sqlSession.insert(sqlStatement(SqlMethod.INSERT_ONE), this));
        } finally {
            closeSqlSession(sqlSession);
        }
    }

    private boolean save(DistributeIdService distributeIdService, Collection<T> entityList) {
        for (T entry : entityList) {
            if (entry == null) {
                continue;
            }
            addPrimaryKey((XModel<T>) entry, distributeIdService);
            entry.insertWithRelation(distributeIdService);
        }
        return true;
    }

    /**
     * 根据 ID 查询
     *
     * @param id 主键ID
     */
    @Override
    public T selectById(Serializable id) {
        try (SqlSession sqlSession = sqlSession()) {
            T entry = sqlSession.selectOne(sqlStatement(SqlMethod.SELECT_BY_ID), id);
            if (entry == null) {
                return null;
            }
            entry.addRelationInfo();
            return entry;
        }
    }
    
    /**
     * 给查询添加关联关系成员
     */
    public void addRelationInfo() {
        Consumer<ExtRelInfo> consumer = (extRelInfo) -> {
            Field declaredField = extRelInfo.getDeclaredField();
            String setMethodName = BeanUtil.getSetMethodName(declaredField.getName());
            boolean isRelationEntry = getClass().equals(extRelInfo.getRelEntryClass());
            if (!isRelationEntry && !getClass().equals(extRelInfo.getRelToEntryClass())) {
                return;
            }
            try {
                Method setMethod = ModelFactory.getInstance().getMethod(getClass(), setMethodName, declaredField.getType());
                setMethod.setAccessible(true);
                Object fieldValue = null;
                if (declaredField.getGenericType() instanceof ParameterizedType) {
                    // 为Collection泛型成员
                    Type genericType = declaredField.getGenericType();
                    if (!Collection.class.isAssignableFrom(declaredField.getType())) {
                        return;
                    }
                    Type[] types = ((ParameterizedType) genericType).getActualTypeArguments();
                    if (types.length != 1
                            || !(types[0] instanceof Class)
                            || !XModel.class.isAssignableFrom((Class<?>) types[0])) {
                        return;
                    }
                    String middleTableName = extRelInfo.getMidTable();
                    // 此对象被declaredField关联
                    String selectedColumn = extRelInfo.getRelColumn();
                    String conditionColumn = extRelInfo.getRelToColumn();
                    Class<T> entryClass = extRelInfo.getRelEntryClass();
                    String entryTablePk = extRelInfo.getRelTablePk();

                    if (isRelationEntry) {
                        // 此对象关联declaredField
                        selectedColumn = extRelInfo.getRelToColumn();
                        conditionColumn = extRelInfo.getRelColumn();
                        entryClass = extRelInfo.getRelToEntryClass();
                        entryTablePk = extRelInfo.getRelToTablePk();
                    }
                    String sql = String.format("SELECT %s FROM %s where %s = {0}", selectedColumn, middleTableName, conditionColumn);
                    List<Object> ids = SqlRunner.db(extRelInfo.getMidEntryClass()).selectObjs(sql, this.primaryKey());
                    if (CollectionUtil.isEmpty(ids)) {
                        return;
                    }
                    XModel entryModel = ModelFactory.getInstance().getObject(entryClass);
                    // 根据中间表获取被关联对象id
                    if (extRelInfo.isReachable()) {
                        List<Object> result = new ArrayList<>(ids.size());
                        for (Object primaryKey : ids) {
                            if (primaryKey == null) {
                                continue;
                            }
                            result.add(entryModel.selectById((Serializable) primaryKey));
                        }
                        fieldValue = result;
                    } else {
                        //根据被关联的id批量查询
                        fieldValue = entryModel.selectList(Wrappers.query().in(entryTablePk, ids));
                    }
                } else if (declaredField.getName().equals(extRelInfo.getRelToProperty())) {
                    // 为1对1 关联关系
                    XModel relationToEntryModel = (XModel) ModelFactory.getInstance().getObject(extRelInfo.getRelToEntryClass());
                    Method getMethod = ModelFactory.getInstance().getMethod(getClass(),
                            BeanUtil.getGetMethodName(extRelInfo.getRelToProperty() + "Id"));
                    Serializable relationToPkValue = (Serializable) getMethod.invoke(extRelInfo.getModel());
                    if (relationToPkValue != null) {
                        if (extRelInfo.isReachable()) {
                            fieldValue = relationToEntryModel.selectById(relationToPkValue);
                        } else {
                            TableInfo tableInfo = SqlHelper.table(extRelInfo.getRelToEntryClass());
                            fieldValue = relationToEntryModel.selectOne(Wrappers.query().eq(tableInfo.getKeyColumn(), relationToPkValue));
                        }
                    }
                }
                if (fieldValue != null) {
                    if (Set.class.isAssignableFrom(declaredField.getType())) {
                        Set<T> entrySet = new HashSet<>((Collection) fieldValue);
                        Object[] args = new Object[]{entrySet};
                        setMethod.invoke(extRelInfo.getModel(), args);
                    } else {
                        Object[] args = new Object[]{fieldValue};
                        setMethod.invoke(extRelInfo.getModel(), args);
                    }
                }
            } catch (NoSuchMethodException e) {
                log.error("获取方法:" + setMethodName + "异常", e);
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                log.error("反射调用方法设置属性异常", e);
                throw new RuntimeException(e);
            } catch (IllegalAccessException | InstantiationException e) {
                log.error("反射创建对象异常", e);
                throw new RuntimeException(e);
            } catch (Exception e) {
                log.error("未知异常", e);
                throw new RuntimeException(e);
            }
        };
        handle(this, consumer);
    }

    private Class<?> getEntryClass(String entry) throws ClassNotFoundException {
        return Class.forName(getClass().getPackage().getName() + "." + entry);
    }

    /**
     * 根据 ID 删除
     *
     * @param id 主键ID
     */
    @Override
    public boolean deleteById(Serializable id) {
        SqlSession sqlSession = sqlSession();
        try {
            Consumer<ExtRelInfo> consumer = (extRelInfo) -> {
                try {
                    RelInfo relationInfo = extRelInfo.getDeclaredField().getAnnotation(RelInfo.class);
                    if (!relationInfo.delRel()) {
                        return;
                    }

                    if (Collection.class.isAssignableFrom(extRelInfo.getDeclaredField().getType())) {
                        boolean isRelationEntry = getClass().equals(extRelInfo.getRelEntryClass());
                        if (!isRelationEntry && !getClass().equals(extRelInfo.getRelToEntryClass())) {
                            return;
                        }
                        String conditionColumn = extRelInfo.getRelToColumn();
                        String selectedColumn = extRelInfo.getRelColumn();
                        Class<T> entryClass = extRelInfo.getRelEntryClass();
                        if (isRelationEntry) {
                            // 该成员为被此model关联
                            conditionColumn = extRelInfo.getRelColumn();
                            selectedColumn = extRelInfo.getRelToColumn();
                            entryClass = extRelInfo.getRelToEntryClass();
                        }
                        // 根据中间表获取关联id
                        String sql = String.format("SELECT %s FROM %s where %s = {0}", selectedColumn, extRelInfo.getMidTable(), conditionColumn);
                        Class<?> middleEntryClass = extRelInfo.getMidEntryClass();
                        List<Object> ids = SqlRunner.db(middleEntryClass).selectObjs(sql, id);
                        // 删除中间表数据
                        delete(middleEntryClass, sqlSession, SqlMethod.DELETE, Wrappers.query().eq(conditionColumn, id));
                        if (CollectionUtil.isEmpty(ids)) {
                            return;
                        }
                        // 删除关联对象信息
                        XModel model = ModelFactory.getInstance().getObject(entryClass);
                        ids.forEach(pk -> model.deleteById((Serializable) pk));
                    } else if (!extRelInfo.getIsMany() && extRelInfo.getDeclaredField().getType().equals(extRelInfo.getDeclaredField().getGenericType())) {
                        if (getClass().getSimpleName().equals(extRelInfo.getRelEntry())) {
                            Method getMethod = ModelFactory.getInstance().getMethod(getClass(), BeanUtil.getGetMethodName(extRelInfo.getRelToProperty() + "Id"));
                            Serializable relationToPkValue = (Serializable) getMethod.invoke(extRelInfo.getModel());
                            if (relationToPkValue != null) {
                                XModel model = (XModel) ModelFactory.getInstance().getObject(extRelInfo.getRelToEntryClass());
                                model.deleteById(relationToPkValue);
                            }
                        }
                    }
                } catch (InstantiationException | IllegalAccessException e) {
                    log.error("反射创建对象异常", e);
                    throw new RuntimeException(e);
                } catch (NoSuchMethodException e) {
                    log.error("获取方法异常", e);
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    log.error("反射删除数据异常", e);
                }
            };
            handle(this, consumer);
            return SqlHelper.retBool(sqlSession.delete(sqlStatement(SqlMethod.DELETE_BY_ID), id));
        } finally {
            closeSqlSession(sqlSession);
        }
    }

    public boolean delete(Class<?> clazz, SqlSession sqlSession, SqlMethod sqlMethod, Wrapper queryWrapper) {
        Map<String, Object> map = new HashMap<>(1);
        map.put(Constants.WRAPPER, queryWrapper);
        String sqlStatement = SqlHelper.table(clazz).getSqlStatement(sqlMethod.getMethod());
        return SqlHelper.retBool(sqlSession.delete(sqlStatement, map));
    }

    public Serializable primaryKey() {
        return pkVal();
    }
}
