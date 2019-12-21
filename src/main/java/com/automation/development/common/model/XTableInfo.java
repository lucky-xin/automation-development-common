package com.automation.development.common.model;


import biz.datainsights.utils.CollectionUtil;
import biz.datainsights.utils.StringUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.*;


/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 自定义TableInfo
 * @date 2019-03-24 10:46
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class XTableInfo extends TableInfo implements Comparable<TableInfo> {

    private DbType dbType = DbType.POSTGRE_SQL;

    /**
     * 所有关联关系信息
     */
    private Set<RelationInfo> relationInfos = new TreeSet<>();

    /**
     * 所有被关联的表信息
     */
    private Set<RelationInfo> beRelatedInfos = new TreeSet<>();

    /**
     * 所有mapper,service属性名称
     */
    private Set<ServiceImplField> serviceFields = new TreeSet<>();

    /**
     * 所有可达表信息譬如 国家可达省，省可达县 国家->省->县
     */
    private Set<XTableInfo> reachable = new HashSet<>();

    private boolean intermediateTable;

    private boolean dto = false;

    private TableField pk;

    private String uri;

    private Set<String> tableFieldNames = new HashSet<>();

    private boolean tree;

    private String primaryKeyColumnName;

    private String primaryKeyPropertyName;

    private String primaryKeyEntryName;

    public XTableInfo addRelationInfo(RelationInfo relationInfo) {
        if (!dto && relationInfo.isMany()) {
            this.dto = true;
        }
        this.relationInfos.add(relationInfo);
        return this;
    }

    public XTableInfo addBeRelationInfo(RelationInfo beRelatedTableInfo) {
        this.beRelatedInfos.add(beRelatedTableInfo);
        return this;
    }

    public XTableInfo addField(TableField tableField) {
        if (tableField == null || !tableFieldNames.add(tableField.getName())) {
            return this;
        }
        if (null != tableField.getColumnType() && null != tableField.getColumnType().getPkg()) {
            getImportPackages().add(tableField.getColumnType().getPkg());
        }
        if (tableField.isKeyFlag()) {
            // 主键
            if (tableField.isConvert() || tableField.isKeyIdentityFlag()) {
                getImportPackages().add(com.baomidou.mybatisplus.annotation.TableId.class.getCanonicalName());
            }
            // 自增
            if (tableField.isKeyIdentityFlag()) {
                getImportPackages().add(com.baomidou.mybatisplus.annotation.IdType.class.getCanonicalName());
            }
        } else if (tableField.isConvert()) {
            // 普通字段
            getImportPackages().add(com.baomidou.mybatisplus.annotation.TableField.class.getCanonicalName());
        }
        if (null != tableField.getFill()) {
            // 填充字段
            getImportPackages().add(com.baomidou.mybatisplus.annotation.TableField.class.getCanonicalName());
            getImportPackages().add(com.baomidou.mybatisplus.annotation.FieldFill.class.getCanonicalName());
        }

        if (super.getFields() == null) {
            super.setFields(new ArrayList<TableField>() {{
                add(tableField);
            }});
        } else {
            super.getFields().add(tableField);
        }
        return this;
    }

    public XTableInfo addFields(Collection<XTableField> tableFields) {
        if (CollectionUtil.isEmpty(tableFields)) {
            return this;
        }
        Iterator<XTableField> iterator = tableFields.iterator();
        while (iterator.hasNext()) {
            XTableField field = iterator.next();
            if (field == null) {
                continue;
            }
            String fieldName = field.getName();
            if (tableFieldNames.add(fieldName)) {
                addField(field);
            }
        }
        return this;
    }

    public XTableInfo addReachable(XTableInfo xTableInfo) {
        reachable.add(xTableInfo);
        return this;
    }

    public boolean getHasBeRelationInfo() {
        return !beRelatedInfos.isEmpty();
    }

    public boolean getHasRelationInfo() {
        return !relationInfos.isEmpty();
    }

    @Override
    public TableInfo setFields(List<TableField> fields) {
        if (CollectionUtil.isEmpty(fields)) {
            return this;
        }
        List<TableField> existFields = getFields();
        if (CollectionUtil.isEmpty(existFields)) {
            super.setFields(fields);
            return this;
        }

        Iterator<TableField> iterator = fields.iterator();
        while (iterator.hasNext()) {
            TableField field = iterator.next();
            if (field == null) {
                continue;
            }
            String fieldName = field.getName();
            for (TableField existField : existFields) {
                if (existField == null) {
                    continue;
                }
                if (existField.getName().equals(fieldName)) {
                    iterator.remove();
                }
            }
        }
        existFields.addAll(fields);
        return this;
    }

    public String getUri() {
        if (!StringUtil.isEmpty(uri)) {
            return uri.replace("_" , "-");
        }

        if (!StringUtil.isEmpty(getName())) {
            return getName().replace("_" , "-");
        }
        return null;
    }

    public boolean getHasPrimaryKey() {
        return null != pk;
    }

    public String getPrimaryKeyPropertyName() {
        if (StringUtil.isEmpty(primaryKeyPropertyName)) {
            this.primaryKeyPropertyName = null != pk && !StringUtil.isEmpty(pk.getPropertyName()) ? pk.getPropertyName() : "id";
        }
        return this.primaryKeyPropertyName;
    }

    public String getPrimaryKeyColumnName() {
        if (StringUtil.isEmpty(primaryKeyColumnName)) {
            this.primaryKeyColumnName = null != pk && !StringUtil.isEmpty(pk.getName()) ? StringUtil.lowerCamelToUnderline(pk.getName()) : "id";
        }
        return this.primaryKeyColumnName;
    }

    public String getPrimaryKeyEntryName() {
        if (StringUtil.isEmpty(primaryKeyEntryName)) {
            this.primaryKeyEntryName = null != pk && !StringUtil.isEmpty(pk.getPropertyName()) ? StringUtil.upperCaseFirstChar(pk.getPropertyName()) : "Id";
        }
        return this.primaryKeyEntryName;
    }

    public XTableInfo addServiceField(ServiceImplField serviceImplField) {
        serviceFields.add(serviceImplField);
        return this;
    }

    @Override
    public int compareTo(TableInfo o) {
        return 0;
    }
}
