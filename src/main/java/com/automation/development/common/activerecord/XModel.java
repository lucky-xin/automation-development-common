package com.automation.development.common.activerecord;

import com.automation.development.common.annotation.RelInfo;
import com.automation.development.common.model.ModelFactory;
import com.automation.development.common.service.DistributeIdService;
import com.automation.development.common.util.FieldHelper;
import com.automation.development.common.util.TableHelper;
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

    private void handle(XModel<T> entry, Consumer<ExtensionRelationInfo> consumer) {

        String tableName = getTableName();

        for (Field declaredField : getClass().getDeclaredFields()) {
            TableField tableFieldAnnotation = declaredField.getAnnotation(TableField.class);
            RelInfo relationInfo = declaredField.getAnnotation(RelInfo.class);
            if (tableFieldAnnotation == null || tableFieldAnnotation.exist() || relationInfo == null) {
                continue;
            }
            String relationTable = relationInfo.relTable();
            String relationToTable = relationInfo.relToTable();
            String relationEntry = StringUtil.isEmpty(relationInfo.relEntry()) ? FieldHelper.getEntryName(relationTable) : relationInfo.relEntry();
            String relationToEntry = StringUtil.isEmpty(relationInfo.relToEntry()) ? FieldHelper.getEntryName(relationToTable) : relationInfo.relToEntry();
            ExtensionRelationInfo<T> extensionRelationInfo = new ExtensionRelationInfo<T>().setModel(entry).setReachable(relationInfo.isReachable());

            Type genericType = declaredField.getGenericType();
            try {
                Class<?> relationEntryClass = null;
                Class<?> relationToEntryClass = null;
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

                if (tableName.equals(relationTable)) {
                    relationEntryClass = getClass();
                    relationToEntryClass = fieldActualClass;
                } else {
                    relationToEntryClass = getClass();
                    relationEntryClass = fieldActualClass;
                }

                // 如果没有继承自Model则跳过
                if (!(XModel.class.isAssignableFrom(relationToEntryClass)
                        && XModel.class.isAssignableFrom(relationEntryClass))) {
                    continue;
                }

                if (isCollection) {
                    // 为多对多关系
                    Assert.notEmpty(relationInfo.midTable(), "middleTable must not be null.");
                    String middleEntry = StringUtil.isEmpty(relationInfo.midEntry()) ?
                            FieldHelper.getEntryName(relationInfo.midTable()) : relationInfo.midEntry();
                    Class<?> middleEntryClass = getEntryClass(middleEntry);
                    // 如果没有继承自Model则跳过
                    if (!XModel.class.isAssignableFrom(middleEntryClass)) {
                        continue;
                    }
                    String middleProperty = StringUtil.isEmpty(relationInfo.midProperty()) ?
                            FieldHelper.getPropertyName(relationInfo.midTable()) : relationInfo.midProperty();
                    extensionRelationInfo.setMiddleEntryClass((Class<T>) middleEntryClass)
                            .setMiddleEntry(middleEntry)
                            .setMiddleProperty(middleProperty)
                            .setMiddleTable(relationInfo.midTable())
                            .setMany(true)
                            .setFieldActualClass((Class<T>) fieldActualClass);
                }
                declaredField.setAccessible(true);
                String relationProperty = StringUtil.isEmpty(relationInfo.relProperty()) ?
                        FieldHelper.getPropertyName(relationTable) : relationInfo.relProperty();

                String relationToProperty = StringUtil.isEmpty(relationInfo.relToProperty()) ?
                        FieldHelper.getPropertyName(relationToTable) : relationInfo.relToProperty();
                String relationColumn = StringUtil.isEmpty(relationInfo.relColumn()) ? relationTable + "_id" : relationInfo.relColumn();
                String relationToColumn = StringUtil.isEmpty(relationInfo.relToColumn()) ? relationToTable + "_id" : relationInfo.relToColumn();
                extensionRelationInfo.setDeclaredField(declaredField)
                        .setRelationEntryClass((Class<T>) relationEntryClass)
                        .setRelationToEntryClass((Class<T>) relationToEntryClass);

                extensionRelationInfo.setRelationTable(relationTable)
                        .setRelationEntry(relationEntry)
                        .setRelationProperty(relationProperty)
                        .setRelationColumn(relationColumn)
                        .setRelationTablePk(relationInfo.relTablePk())
                        .setRelationToTable(relationToTable)
                        .setRelationToEntry(relationToEntry)
                        .setRelationToProperty(relationToProperty)
                        .setRelationToColumn(relationToColumn)
                        .setRelationToTablePk(relationInfo.relToTablePk());
                consumer.accept(extensionRelationInfo);
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
            Consumer<ExtensionRelationInfo> consumer = (extensionRelationInfo) -> {
                Field declaredField = extensionRelationInfo.getDeclaredField();
                // 为Collection泛型成员
                try {
                    // 获取关联成员变量的值
                    String getMethodName = BeanUtil.getGetMethodName(declaredField.getName());
                    Method getMethod = ModelFactory.getInstance().getMethod(getClass(), getMethodName);
                    Object fieldValue = getMethod.invoke(xModel);
                    if (fieldValue == null) {
                        return;
                    }

                    if (extensionRelationInfo.getIsMany()) {
                        // 此model关联其他多个model
                        boolean isRelationToEntry = extensionRelationInfo.getFieldActualClass()
                                .equals(extensionRelationInfo.getRelationToEntryClass());
                        Collection<T> entityList = (Collection<T>) fieldValue;
                        // 把成员对象存入数据库
                        save(distributeIdService, entityList);
                        // 获取关联对象id，并插入中间表
                        Set<Serializable> ids = entityList.stream().map(item -> item.pkVal()).collect(Collectors.toSet());
                        String relationToEntry = extensionRelationInfo.getRelationEntry();
                        String relationEntry = extensionRelationInfo.getRelationToEntry();
                        if (isRelationToEntry) {
                            relationEntry = extensionRelationInfo.getRelationEntry();
                            relationToEntry = extensionRelationInfo.getRelationToEntry();
                        }
                        // 反射获取中间表关联id创建关联关系对象
                        Class<T> middleEntryClass = extensionRelationInfo.getMiddleEntryClass();
                        Method method1 = ModelFactory.getInstance().getMethod(middleEntryClass, "set" + relationEntry + "Id", Long.class);
                        Method method2 = ModelFactory.getInstance().getMethod(middleEntryClass, "set" + relationToEntry + "Id", Long.class);
                        List<T> middleModels = new ArrayList<>(ids.size());
                        Object[] args = new Object[]{xModel.pkVal()};
                        for (Serializable id : ids) {
                            T middleModel = middleEntryClass.newInstance();
                            method1.invoke(middleModel, args);
                            Object[] params = new Object[]{id};
                            method2.invoke(middleModel, params);
                            middleModels.add(middleModel);
                        }
                        save(distributeIdService, middleModels);
                    } else if (XModel.class.isAssignableFrom(declaredField.getType())) {
                        if (getClass().getSimpleName().equals(extensionRelationInfo.getRelationEntry())) {
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
        Consumer<ExtensionRelationInfo> consumer = (extensionRelationInfo) -> {
            Field declaredField = extensionRelationInfo.getDeclaredField();
            String setMethodName = BeanUtil.getSetMethodName(declaredField.getName());
            boolean isRelationEntry = getClass().equals(extensionRelationInfo.getRelationEntryClass());
            if (!isRelationEntry && !getClass().equals(extensionRelationInfo.getRelationToEntryClass())) {
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
                    String middleTableName = extensionRelationInfo.getMiddleTable();
                    // 此对象被declaredField关联
                    String selectedColumn = extensionRelationInfo.getRelationColumn();
                    String conditionColumn = extensionRelationInfo.getRelationToColumn();
                    Class<T> entryClass = extensionRelationInfo.getRelationEntryClass();
                    String entryTablePk = extensionRelationInfo.getRelationTablePk();

                    if (isRelationEntry) {
                        // 此对象关联declaredField
                        selectedColumn = extensionRelationInfo.getRelationToColumn();
                        conditionColumn = extensionRelationInfo.getRelationColumn();
                        entryClass = extensionRelationInfo.getRelationToEntryClass();
                        entryTablePk = extensionRelationInfo.getRelationToTablePk();
                    }
                    String sql = String.format("SELECT %s FROM %s where %s = {0}", selectedColumn, middleTableName, conditionColumn);
                    List<Object> ids = SqlRunner.db(extensionRelationInfo.getMiddleEntryClass()).selectObjs(sql, this.primaryKey());
                    if (CollectionUtil.isEmpty(ids)) {
                        return;
                    }
                    XModel entryModel = ModelFactory.getInstance().getObject(entryClass);
                    // 根据中间表获取被关联对象id
                    if (extensionRelationInfo.isReachable()) {
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
                } else if (declaredField.getName().equals(extensionRelationInfo.getRelationToProperty())) {
                    // 为1对1 关联关系
                    XModel relationToEntryModel = (XModel) ModelFactory.getInstance().getObject(extensionRelationInfo.getRelationToEntryClass());
                    Method getMethod = ModelFactory.getInstance().getMethod(getClass(), BeanUtil.getGetMethodName(extensionRelationInfo.getRelationToProperty() + "Id"));
                    Serializable relationToPkValue = (Serializable) getMethod.invoke(extensionRelationInfo.getModel());
                    if (relationToPkValue != null) {
                        if (extensionRelationInfo.isReachable()) {
                            fieldValue = relationToEntryModel.selectById(relationToPkValue);
                        } else {
                            TableInfo tableInfo = SqlHelper.table(extensionRelationInfo.getRelationToEntryClass());
                            fieldValue = relationToEntryModel.selectOne(Wrappers.query().eq(tableInfo.getKeyColumn(), relationToPkValue));
                        }
                    }
                }
                if (fieldValue != null) {
                    if (Set.class.isAssignableFrom(declaredField.getType())) {
                        Set<T> entrySet = new HashSet<>((Collection) fieldValue);
                        Object[] args = new Object[]{entrySet};
                        setMethod.invoke(extensionRelationInfo.getModel(), args);
                    } else {
                        Object[] args = new Object[]{fieldValue};
                        setMethod.invoke(extensionRelationInfo.getModel(), args);
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
            Consumer<ExtensionRelationInfo> consumer = (extensionRelationInfo) -> {
                try {
                    RelInfo relationInfo = extensionRelationInfo.getDeclaredField().getAnnotation(RelInfo.class);
                    if (!relationInfo.deleteRelation()) {
                        return;
                    }

                    if (Collection.class.isAssignableFrom(extensionRelationInfo.getDeclaredField().getType())) {
                        boolean isRelationEntry = getClass().equals(extensionRelationInfo.getRelationEntryClass());
                        if (!isRelationEntry && !getClass().equals(extensionRelationInfo.getRelationToEntryClass())) {
                            return;
                        }
                        String conditionColumn = extensionRelationInfo.getRelationToColumn();
                        String selectedColumn = extensionRelationInfo.getRelationColumn();
                        Class<T> entryClass = extensionRelationInfo.getRelationEntryClass();
                        if (isRelationEntry) {
                            // 该成员为被此model关联
                            conditionColumn = extensionRelationInfo.getRelationColumn();
                            selectedColumn = extensionRelationInfo.getRelationToColumn();
                            entryClass = extensionRelationInfo.getRelationToEntryClass();
                        }
                        // 根据中间表获取关联id
                        String sql = String.format("SELECT %s FROM %s where %s = {0}", selectedColumn, extensionRelationInfo.getMiddleTable(), conditionColumn);
                        Class<?> middleEntryClass = extensionRelationInfo.getMiddleEntryClass();
                        List<Object> ids = SqlRunner.db(middleEntryClass).selectObjs(sql, id);
                        // 删除中间表数据
                        delete(middleEntryClass, sqlSession, SqlMethod.DELETE, Wrappers.query().eq(conditionColumn, id));
                        if (CollectionUtil.isEmpty(ids)) {
                            return;
                        }
                        // 删除关联对象信息
                        XModel model = ModelFactory.getInstance().getObject(entryClass);
                        ids.forEach(pk -> model.deleteById((Serializable) pk));
                    } else if (!extensionRelationInfo.getIsMany() && extensionRelationInfo.getDeclaredField().getType().equals(extensionRelationInfo.getDeclaredField().getGenericType())) {
                        if (getClass().getSimpleName().equals(extensionRelationInfo.getRelationEntry())) {
                            Method getMethod = ModelFactory.getInstance().getMethod(getClass(), BeanUtil.getGetMethodName(extensionRelationInfo.getRelationToProperty() + "Id"));
                            Serializable relationToPkValue = (Serializable) getMethod.invoke(extensionRelationInfo.getModel());
                            if (relationToPkValue != null) {
                                XModel model = (XModel) ModelFactory.getInstance().getObject(extensionRelationInfo.getRelationToEntryClass());
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
