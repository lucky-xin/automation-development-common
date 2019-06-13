package com.automation.development.common.service.impl;


import com.automation.development.common.activerecord.XModel;
import com.automation.development.common.service.DistributeIdService;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import com.xin.utils.BeanUtil;
import com.xin.utils.CollectionUtil;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 自定义ServiceImpl
 * @date 2019-05-06 19:47
 */
public class XServiceImpl<M extends BaseMapper<T>, T extends XModel> extends ServiceImpl<M, T> {

    @Autowired
    protected DistributeIdService distributeIdService;

    @Override
    public boolean save(T entity) {
        if (entity == null) {
            return false;
        }
        if (entity.primaryKey() == null) {
            addPrimaryKey(entity);
        }
        return entity.insert();
    }

    private void addPrimaryKey(T entity) {
        try {
            TableInfo tableInfo = SqlHelper.table(entity.getClass());
            Method method = entity.getClass().getMethod(BeanUtil.getSetMethodName(tableInfo.getKeyProperty()), Long.class);
            method.invoke(entity, distributeIdService.nextId(tableInfo.getTableName(), 1));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean insertWithRelation(T entity) {
        if (entity == null) {
            return false;
        }
        return entity.insertWithRelation(distributeIdService);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveBatch(Collection<T> entityList, int batchSize) {
        return saveBatch(entityList, null, batchSize);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<T> entityList, Function<T, T> function, int batchSize) {
        String sqlStatement = sqlStatement(SqlMethod.INSERT_ONE);
        try (SqlSession batchSqlSession = sqlSessionBatch()) {
            int i = 0;
            for (T anEntityList : entityList) {
                if (function != null) {
                    anEntityList = function.apply(anEntityList);
                }
                if (anEntityList.primaryKey() == null) {
                    addPrimaryKey(anEntityList);
                }
                batchSqlSession.insert(sqlStatement, anEntityList);
                if (i >= 1 && i % batchSize == 0) {
                    batchSqlSession.flushStatements();
                }
                i++;
            }
            batchSqlSession.flushStatements();
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatchWithRelation(Collection<T> entityList) {
        return saveBatchWithRelation(entityList, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatchWithRelation(Collection<T> entityList, Function<T, T> function) {
        if (CollectionUtil.isEmpty(entityList)) {
            return false;
        }
        for (T entry : entityList) {
            if (function != null) {
                entry = function.apply(entry);
            }
            if (entry != null) {
                entry.insertWithRelation(distributeIdService);
            }
        }
        return true;
    }

    @Override
    public T getById(Serializable id) {
        T entry = super.getById(id);
        if (entry == null) {
            return null;
        }
        entry.addRelationInfo();
        return entry;
    }

    @Override
    public boolean removeById(Serializable id) {
        T entry = super.getById(id);
        if (entry == null) {
            return false;
        }
        return entry.deleteById(id);
    }

    public boolean removeWithRelationById(Serializable id) {
        T entry = super.getById(id);
        if (entry == null) {
            return false;
        }
        return entry.deleteById(id);
    }

    public List<Object> selectObjs(String tableName, String selectColumn, String conditionColumn, Object conditionValue) {
        String sql = String.format("select %s from %s where %s = {0}", selectColumn, tableName, conditionColumn);
        return selectObjs(sql, conditionValue);
    }

    /**
     * 根据sql查询一个字段值的结果集
     * <p>注意：该方法只会返回一个字段的值， 如果需要多字段，请参考{@code selectList()}</p>
     *
     * @param sql  sql语句，可添加参数，格式：{0},{1}
     * @param args 只接受String格式
     * @return ignore
     */
    public List<Object> selectObjs(String sql, Object... args) {
        return new SqlRunner().selectObjs(sql, args);
    }
}
