package com.auto.development.common.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.auto.development.common.model.*;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.xin.utils.StringUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.experimental.UtilityClass;
import org.apache.ibatis.type.JdbcType;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 构建tableInfo 帮助类
 * @date 2019-04-08 17:20
 */
@UtilityClass
public class TableHelper {

    public XTableInfo getTableInfo(Class<?> clazz) {
        XTableInfo xTableInfo = new XTableInfo();
        /* 设置表名 */
        TableName table = clazz.getAnnotation(TableName.class);
        ApiModel apiModel = clazz.getAnnotation(ApiModel.class);
        String tableName = clazz.getSimpleName();
        if (table != null && !StringUtil.isEmpty(table.value())) {
            tableName = table.value();
        } else {
            tableName = getColumnName(tableName);
        }
        xTableInfo.setName(tableName);

        if (apiModel != null && !StringUtil.isEmpty(apiModel.description())) {
            xTableInfo.setComment(apiModel.description());
        } else {
            xTableInfo.setComment(getComment(tableName));
        }

        addField(clazz, xTableInfo);
        return xTableInfo;
    }

    private void addField(Class<?> clazz, XTableInfo xTableInfo) {
        for (Field declaredField : clazz.getDeclaredFields()) {
            TableField tableFieldAnnotation = declaredField.getAnnotation(TableField.class);
            TableId tableId = declaredField.getAnnotation(TableId.class);
            boolean shouldSkip = declaredField.getName().equals("serialVersionUID")
                    || (tableId == null && (tableFieldAnnotation != null && !tableFieldAnnotation.exist()));
            if (shouldSkip) {
                continue;
            }
            ApiModelProperty apiModelProperty = declaredField.getAnnotation(ApiModelProperty.class);
            String comment = null;
            if (apiModelProperty != null) {
                comment = apiModelProperty.value();
            }
            if (StringUtil.isEmpty(comment)) {
                comment = getComment(declaredField.getName());
            }

            if (tableId != null) {
                XTableField tableField = buildField(tableId.value(), Constants.PRIMARY_KEY_TYPE, true);
                tableField.setComment(comment);
                xTableInfo.addField(tableField);
                continue;
            }
            XTableField tableField = buildField(getColumnName(declaredField.getName()), DbTypeMapper.getDbColumnType(declaredField.getType()), false);
            tableField.setComment(comment);
            xTableInfo.addField(tableField);

        }
    }

    public List<String> getCreateTableSql(XTableInfo tableInfo) {
        switch (tableInfo.getDbType()) {
            case POSTGRE_SQL:
                return getCreatePgSqlStatements(tableInfo);

            case MYSQL:
                return getCreateMySqlStatements(tableInfo);

            default:
                break;
        }


        return null;
    }

    private List<String> getCreateMySqlStatements(XTableInfo tableInfo) {
        List<com.baomidou.mybatisplus.generator.config.po.TableField> fields = tableInfo.getFields();
        List<String> result = new ArrayList<>(2 + fields.size());
        StringBuilder deleteTableSql = new StringBuilder("drop table if exists ").append(tableInfo.getName());
        result.add(deleteTableSql.toString());
        com.baomidou.mybatisplus.generator.config.po.TableField idField = null;
        Set<String> primaryKeys = new HashSet<>();
        StringBuilder createTableSql = new StringBuilder("CREATE TABLE ").append(tableInfo.getName()).append(" (");
        for (com.baomidou.mybatisplus.generator.config.po.TableField field : fields) {
            String fieldName = field.getName();
            if (field.getColumnType() instanceof DbColumnType) {
                DbColumnType dbColumnType = (DbColumnType) field.getColumnType();
                createTableSql.append(fieldName)
                        .append(DbTypeMapper.getMySqlType(dbColumnType));
            }
            if (field.isKeyFlag()) {
                primaryKeys.add(fieldName);
                createTableSql.append(" not null");
            }
            createTableSql.append(" COMMENT '").append(field.getComment()).append("',");
            if ("id".equals(fieldName)) {
                idField = field;
            }
        }
        //没有主键
        if (primaryKeys.size() == 0) {
            primaryKeys.add("id");
            createTableSql.append(" id bigint not null,");
            //没有id字段
            if (idField == null) {
                TableHelper.addDefaultPrimaryKey(tableInfo);
            } else {
                idField.setColumnType(Constants.PRIMARY_KEY_TYPE);
                tableInfo.setPk(idField);
            }
        }
        createTableSql.append(String.format("PRIMARY KEY (%s) USING BTREE",
                StringUtil.collectionToCommaDelimitedList(primaryKeys)))
                .append(") ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='")
                .append(tableInfo.getComment()).append("';");
        result.add(createTableSql.toString());
        return result;
    }

    public List<String> getCreatePgSqlStatements(XTableInfo tableInfo) {
        List<com.baomidou.mybatisplus.generator.config.po.TableField> fields = tableInfo.getFields();
        List<String> result = new ArrayList<>(2 + fields.size());
        List<String> commentSqls = new ArrayList<>(fields.size());
        String tableName = tableInfo.getName();
        StringBuilder deleteTableSql = new StringBuilder("drop table if exists ").append(tableName);
        result.add(deleteTableSql.toString());
        StringBuilder createTableSql = new StringBuilder();
        createTableSql.append("CREATE TABLE ").append(tableName).append("(");
        Set<String> primaryKeys = new HashSet<>();
        com.baomidou.mybatisplus.generator.config.po.TableField idField = null;

        for (com.baomidou.mybatisplus.generator.config.po.TableField field : fields) {
            String fieldName = field.getName();
            if (field.getColumnType() instanceof DbColumnType) {
                DbColumnType dbColumnType = (DbColumnType) field.getColumnType();

                createTableSql.append(fieldName).append(DbTypeMapper.getPgSqlType(dbColumnType));
                if (field.isKeyFlag()) {
                    primaryKeys.add(fieldName);
                    createTableSql.append(" not null");
                }

                if (!StringUtil.isEmpty(field.getComment())) {
                    commentSqls.add(TableHelper.getAddCommentSql(tableName, fieldName, field.getComment()));
                }
                createTableSql.append(",");
                if ("id".equals(fieldName)) {
                    idField = field;
                }
            }
        }

        //没有主键
        if (primaryKeys.size() == 0) {
            primaryKeys.add("id");
            createTableSql.append(" id bigint not null,");
            //没有id字段
            if (idField == null) {
                TableHelper.addDefaultPrimaryKey(tableInfo);
            } else {
                idField.setColumnType(Constants.PRIMARY_KEY_TYPE);
                tableInfo.setPk(idField);
            }
        }
        createTableSql.append(" CONSTRAINT ")
                .append(tableName)
                .append("_pkey PRIMARY KEY (")
                .append(StringUtil.collectionToCommaDelimitedList(primaryKeys)).append(")");
        createTableSql.append(")");
        result.add(createTableSql.toString());
        //comment on table user is 'Our session logs';
        StringBuilder tableComment = new StringBuilder("comment on table ")
                .append(tableName).append(" is '")
                .append(getTableComment(tableInfo)).append("'");
        result.add(tableComment.toString());
        result.addAll(commentSqls);
        return result;
    }

    private String getTableComment(XTableInfo tableInfo) {
        if (tableInfo == null) {
            return "";
        }
        if (!StringUtil.isEmpty(tableInfo.getComment())) {
            return tableInfo.getComment();
        }
        return tableInfo.getName();
    }

    public String getAddColumnSql(com.baomidou.mybatisplus.generator.config.po.TableField field, DbType dbType) {
        String type = null;
        switch (dbType) {
            case MYSQL:
                type = DbTypeMapper.getMySqlType((DbColumnType) field.getColumnType());
                break;
            case POSTGRE_SQL:
                type = DbTypeMapper.getPgSqlType((DbColumnType) field.getColumnType());
                break;
            default:
                type = "varchar(100)";
                break;

        }
        String sql = String.format(" ADD COLUMN %s %s", field.getName(), type);
        return sql;
    }

    public String getAddCommentSql(String tableName, String fieldName, String comment) {
        String sql = String.format(" comment on column %s.%s is '%s'", tableName, fieldName, comment);
        return sql;
    }

    public void initTable(JSONObject schema, String currentTable, XTableInfo tableInfo) {
        if (tableInfo == null) {
            return;
        }
        if (tableInfo.getEntityName() == null) {
            tableInfo.setEntityName(getEntryName(currentTable));
        }
        if (StringUtil.isEmpty(tableInfo.getComment())) {
            String tableComment = getComment(currentTable, schema);
            tableInfo.setComment(tableComment);
        }
        if (StringUtil.isEmpty(tableInfo.getName())) {
            tableInfo.setName(getColumnName(currentTable));
        }
        if (tableInfo.getUri() == null) {
            tableInfo.setUri(currentTable);
        }
    }

    public void initTable(String currentTable, XTableInfo tableInfo) {
        initTable(null, currentTable, tableInfo);
    }

    /**
     * 判断是否为主键默认字段名称为id为表的主键或者指定isPrimaryKey为true
     *
     * @param name
     * @param jsonNode
     * @return
     */
    public boolean isPrimaryKey(String name, JSONObject jsonNode) {
        if (isPrimaryKey(name)) {
            return true;
        }
        if (jsonNode == null) {
            return false;
        }
        String text = jsonNode.getString("isPrimaryKey");
        return "true".equals(text);
    }

    /**
     * 构建字段 如果字段名称为id则设置为主键类型
     *
     * @param fieldName    字段名称
     * @param dbColumnType 字段类型
     * @param keyFlag      指定字段是否为主键
     * @return
     */
    public XTableField buildField(String fieldName, DbColumnType dbColumnType, boolean keyFlag) {
        fieldName = getColumnName(fieldName);
        if (!keyFlag) {
            keyFlag = "id".equals(fieldName);
        }
        if (keyFlag && Constants.PRIMARY_KEY_TYPE != dbColumnType) {
            dbColumnType = Constants.PRIMARY_KEY_TYPE;
        }
        return doBuildTableField(fieldName, dbColumnType, keyFlag);
    }

    /**
     * 构建字段 如果字段名称为id则设置为主键类型
     *
     * @param fieldName 字段名称
     * @param jsonNode  字段节点信息
     * @param keyFlag   指定字段是否为主键
     * @return
     */
    public XTableField buildField(String fieldName, JSONObject jsonNode, boolean keyFlag) {
        fieldName = getColumnName(fieldName);
        if (!keyFlag) {
            keyFlag = "id".equals(fieldName);
        }
        DbColumnType iColumnType = keyFlag ? Constants.PRIMARY_KEY_TYPE : getDbColumnType(fieldName, jsonNode);
        return doBuildTableField(fieldName, iColumnType, keyFlag);
    }

    private XTableField doBuildTableField(String fieldName, DbColumnType dbColumnType, boolean keyFlag) {
        JdbcType jdbcType = JdbcType.VARCHAR;
        switch (dbColumnType) {
            case STRING:
                jdbcType = JdbcType.VARCHAR;
                break;
            case TIMESTAMP:
                jdbcType = JdbcType.TIMESTAMP;
                break;
            case LONG:
                jdbcType = JdbcType.BIGINT;
                break;
            case INTEGER:
                jdbcType = JdbcType.INTEGER;
                break;
            case FLOAT:
                jdbcType = JdbcType.FLOAT;
                break;
            case DOUBLE:
                jdbcType = JdbcType.DOUBLE;
                break;
            case DATE:
                jdbcType = JdbcType.DATE;
                break;
            default:
                break;
        }
        XTableField tableField = new XTableField();
        if (dbColumnType == DbColumnType.TIMESTAMP || dbColumnType == DbColumnType.DATE) {
            String lowerCaseFieldName = fieldName.toLowerCase();
            if ("create_time".equals(lowerCaseFieldName)) {
                tableField.setFill(FieldFill.INSERT.name());
            }
            if ("update_time".equals(lowerCaseFieldName) || "edit_time".equals(lowerCaseFieldName)) {
                tableField.setFill(FieldFill.INSERT_UPDATE.name());
            }
        }

        tableField.setName(fieldName);
        tableField.setPropertyName(getPropertyName(fieldName));
        tableField.setColumnType(dbColumnType);
        tableField.setKeyFlag(keyFlag);
        tableField.setJdbcType(jdbcType);
        return tableField;
    }

    /**
     * 添加字段到表中
     *
     * @param tableInfo
     * @param jsonNode
     * @param fieldName
     */
    public void addField(XTableInfo tableInfo, JSONObject jsonNode, String fieldName) {
        if (tableInfo == null) {
            return;
        }
        /**
         * 把字段名称转化为小写下斜杠格式
         */
        fieldName = getColumnName(fieldName);
        boolean isPrimaryKey = isPrimaryKey(fieldName, jsonNode);
        XTableField tableField = buildField(fieldName, jsonNode, isPrimaryKey);
        /**
         * 判断是否为树结构表
         */
        boolean isTree = isParentId(fieldName);
        if (isTree) {
            tableField.setColumnType(Constants.PRIMARY_KEY_TYPE);
            tableInfo.setTree(true);
        }

        if (isPrimaryKey) {
            tableInfo.setPk(tableField);
        }

        String comment = getComment(fieldName, jsonNode);
        tableField.setComment(comment);
        if (jsonNode != null) {
            ValidateConditionUtil.setPattern(tableField, jsonNode);
            ValidateConditionUtil.setMinLength(tableField, jsonNode);
            ValidateConditionUtil.setMaxLength(tableField, jsonNode);
            ValidateConditionUtil.setMinValue(tableField, jsonNode);
            ValidateConditionUtil.setMaxValue(tableField, jsonNode);
            ValidateConditionUtil.addValid(tableField, getPropertyName(fieldName), tableField.getValidate());
            JSONArray enumNode = jsonNode.getJSONArray("enum");
            if (enumNode != null) {
                addEnumInfo(tableField, enumNode);
            }
        }
        if (tableField.getIsEnumField()) {
            tableField.setColumnType(DbColumnType.INTEGER);
        }
        tableInfo.addField(tableField);
    }

    public void addDefaultPrimaryKey(XTableInfo tableInfo) {
        XTableField idField = buildField("id", Constants.PRIMARY_KEY_TYPE, true);
        tableInfo.addField(idField);
        tableInfo.setPk(idField);
    }

    /**
     * 获取字段类型如果名称为id则为主键类型 如果为update_time,create_time,edit_time则为日期类型
     *
     * @param fieldName
     * @param jsonNode
     * @return
     */
    private DbColumnType getDbColumnType(String fieldName, JSONObject jsonNode) {
        /**
         * 如果为主键直接返回主键类型
         */
        if (isPrimaryKey(fieldName)) {
            return Constants.PRIMARY_KEY_TYPE;
        }
        boolean isDate = isDateNode(fieldName, jsonNode);
        if (isDate) {
            return DbColumnType.TIMESTAMP;
        }
        DbColumnType dbColumnType = DbColumnType.STRING;
        if (jsonNode == null || jsonNode.get("type") == null) {
            return dbColumnType;
        }
        String type = jsonNode.getString("type");
        return getDbColumnType(type);
    }

    private DbColumnType getDbColumnType(String nodeType) {
        DbColumnType dbColumnType = DbColumnType.STRING;
        switch (nodeType) {
            case "string":
                dbColumnType = DbColumnType.STRING;
                break;

            case "boolean":
                dbColumnType = DbColumnType.BOOLEAN;
                break;

            case "integer":
                dbColumnType = DbColumnType.INTEGER;
                break;

            case "number":
                dbColumnType = DbColumnType.LONG;
                break;
            default:
                break;
        }
        return dbColumnType;
    }

    private boolean isPrimaryKey(String fieldName) {
        return "id".equals(fieldName);
    }

    private Pattern englishTextPattern = Pattern.compile("(^[a-zA-Z]+)");

    /**
     * TODO 枚举类型待处理
     *
     * @param tableField
     * @param arrayNode
     */
    private void addEnumInfo(XTableField tableField, JSONArray arrayNode) {
        if (tableField == null || arrayNode == null) {
            return;
        }
        Iterator<Object> iterator = arrayNode.iterator();
        List<EnumField> fields = new ArrayList<>();
        String fieldName = tableField.getName();
        EnumInfo enumInfo = new EnumInfo().setFieldName(fieldName);
        DbColumnType dbColumnType = null;
//
//        while (iterator.hasNext()) {
//            JsonNode element = iterator.next();
//            EnumField enumField = new EnumField();
//            if (element.isTextual()) {
//                String textValue = element.asText();
//                if (englishTextPattern.matcher(textValue).find()) {
//                    String entryName = getColumnName(textValue);
//                    if (StringUtil.isEmpty(entryName)) {
//                        continue;
//                    }
//                    enumField.setFieldName(entryName.toUpperCase()).setFieldValue(textValue).setFieldCode(fields.size() + 1);
//                    fields.add(enumField);
//                    dbColumnType = DbColumnType.STRING;
//                    continue;
//                }
//            }
//
//            if (element.isInt() || element.isLong()) {
//                dbColumnType = DbColumnType.LONG;
//                enumField.setFieldValue(element.asLong(0));
//            } else if (element.isFloat() || element.isDouble()) {
//                enumField.setFieldValue(element.asDouble(0));
//                dbColumnType = DbColumnType.DOUBLE;
//            }
//            fields.add(enumField);
//        }
//        enumInfo.setValueType(dbColumnType)
//                .setEntryName(getEntryName(fieldName))
//                .setPropertyName(getPropertyName(fieldName))
//                .setFields(fields);
//        tableField.setEnumInfo(enumInfo);
    }

    private Pattern commentPattern = Pattern.compile("(^[\bThe\b.*\bSchema\b])");

    private boolean isParentId(String fieldName) {
        return "parentId".equals(fieldName) || "parent_id".equals(fieldName);
    }

    public String getComment(String fieldName, JSONObject jsonNode) {
        if (jsonNode == null) {
            if (!StringUtil.isEmpty(fieldName)) {
                fieldName = getColumnName(fieldName);
                return fieldName.replace("_", " ");
            }
            return "";
        }

        String title = jsonNode.getString("title");
        if (title == null) {
            return "";
        }
        if (commentPattern.matcher(title).find()) {
            if (StringUtil.isEmpty(fieldName)) {
                return title.replace("The", "")
                        .replace("Schema", "").trim();
            }
            return fieldName.replace("_", " ");
        }
        return title;
    }

    /**
     * 获取数据库comment
     *
     * @param fieldName
     * @return
     */
    public String getComment(String fieldName) {
        if (!StringUtil.isEmpty(fieldName)) {
            fieldName = getColumnName(fieldName);
            return fieldName.replace("_", " ");
        }
        return "";
    }

    /**
     * 获取java类名
     *
     * @param fieldName
     * @return
     */
    public String getEntryName(String fieldName) {
        if (StringUtil.isEmpty(fieldName)) {
            return "";
        }
        if (fieldName.contains("_")) {
            fieldName = StringUtil.underlineToLowerCamel(fieldName);
        }
        return StringUtil.upperCaseFirstChar(fieldName);
    }

    /**
     * 获取java 驼峰式命名字段
     *
     * @param name
     * @return
     */
    public String getPropertyName(String name) {
        if (StringUtil.isEmpty(name)) {
            return "";
        }
        return StringUtil.underlineToLowerCamel(name);
    }

    public String getUri(String fieldName) {
        if (StringUtil.isEmpty(fieldName)) {
            return "";
        }
        String columnName = getColumnName(fieldName);
        return columnName.replace("_", "-");
    }

    public String getEndWithIdField(String field) {
        if (field == null) {
            return null;
        }
        int index = field.lastIndexOf("Id");
        if (index > 0 && index == (field.length() - 2)) {
            String text = field.substring(0, index);
            return StringUtil.lowerCamelToUnderline(text);
        }
        index = field.lastIndexOf("_id");
        if (index > 0 && index == (field.length() - 3)) {
            String text = field.substring(0, index);
            return StringUtil.lowerCamelToUnderline(text);
        }
        return null;
    }

    /**
     * 判断是否为时间字段
     *
     * @param fieldName
     * @param jsonNode
     * @return
     */
    public boolean isDateNode(String fieldName, JSONObject jsonNode) {
        if (!StringUtil.isEmpty(fieldName) && fieldName.endsWith("_time")) {
            return true;
        }
        if (jsonNode == null) {
            return false;
        }
        String javaType = jsonNode.getString("javaType");
        if (StringUtil.isNotEmpty(javaType)) {
            String text = javaType.toLowerCase();
            if (text.contains("date")) {
                return true;
            }
        }
        String format = jsonNode.getString("format");
        if (format != null) {
            if ("date-time".equals(format) || "time".equals(format) || "date".equals(format)) {
                return true;
            }
        }
        return false;
    }


    private Pattern pattern = Pattern.compile("^[A-Za-z_]+$");

    public String getColumnName(String relationProperty) {
        if (pattern.matcher(relationProperty).find()) {
            return relationProperty.toLowerCase();
        }
        return StringUtil.lowerCamelToUnderline(relationProperty);
    }

    /**
     * 构建1对1关联关系
     *
     * @return
     */
    public RelInfo build1To1Rel(String relTable, String relColumn, String relToTable) {
        return buildRel(false, relTable, relColumn,
                relToTable, null, null);
    }

    /**
     * 构建n对n关联关系
     *
     * @param relTable    关联表信息 如果学校(school)关联学生(student)，则relTable为学校，学生为relToTable
     * @param relColumn   关联字段信息 如果学校(school)关联学生(student)，则relColumn为学校id为school_id，
     *                    relToColumn为学生表id,为student_id
     * @param relToTable
     * @param relToColumn
     * @return
     */
    public RelInfo buildN2NRel(String relTable, String relColumn, String relToTable, String relToColumn) {
        return buildRel(true, relTable, relColumn, relToTable, relToColumn, getMidTableName(relTable, relToTable));
    }


    private RelInfo buildRel(boolean isMany, String relTable, String relColumn, String relToTable,
                             String relToColumn,
                             String middleTableName) {
        RelInfo relInfo = new RelInfo()
                .setMany(isMany)
                .setRelEntry(getEntryName(relTable))
                .setRelProperty(getPropertyName(relTable))
                .setRelTable(relTable)
                .setRelColumn(relColumn)
                .setRelUri(getUri(relTable))
                .setRelToEntry(getEntryName(relToTable))
                .setRelToProperty(getPropertyName(relToTable))
                .setRelToUri(getUri(relToTable))
                .setRelToTable(relToTable);

        if (StringUtil.isNotEmpty(relToColumn)) {
            relInfo.setRelToColumn(relToColumn);
        }

        if (StringUtil.isNotEmpty(middleTableName)) {
            relInfo.setMidTable(middleTableName)
                    .setMidProperty(getPropertyName(middleTableName))
                    .setMidEntry(getEntryName(middleTableName));
        }
        return relInfo;
    }

    public String getMidTableName(String relTable, String relToTable) {
        return relTable + "_" + relToTable;
    }

    public RelInfoHolder builderRelHolder(boolean isMany, String relTable,
                                          String relToTable, String middleTableName) {
        String relColumn = getRelColumn(relTable);
        String relToColumn = getRelColumn(relToTable);
        RelInfoHolder manyToManyRelInfoHolder = new RelInfoHolder()
                .setRelTable(relTable)
                .setRelColumn(relColumn)
                .setRelToColumn(relToColumn)
                .setRelColumn(relColumn)
                .setMidTableName(middleTableName)
                .setRelToTable(relToTable)
                .setMany(isMany);
        return manyToManyRelInfoHolder;
    }

    public String getRelColumn(String tableName) {
        return tableName + "_id";
    }

    /**
     * 判断是否为数据库create_time字段
     *
     * @return
     */
    public boolean isCreateTimeCol(String fieldName) {
        return "create_time".equals(fieldName);
    }

    /**
     * 判断是否为数据库的update_time活着edit_time
     *
     * @return
     */
    public boolean isUpdateTimeCol(String fieldName) {
        return "update_time".equals(fieldName) || "edit_time".equals(fieldName);
    }

    /**
     * 生成数据库create_time字段
     *
     * @return
     */
    public XTableField buildCreateTimeField() {
        return doBuildTableField("create_time", DbColumnType.DATE, false);
    }

    /**
     * 生成数据库update_time字段
     *
     * @return
     */
    public XTableField buildUpdateTimeField() {
        return doBuildTableField("update_time", DbColumnType.DATE, false);
    }

    /**
     * 生成数据库主键字段
     *
     * @return
     */
    public XTableField buildPkField(String fieldName) {
        return doBuildTableField(fieldName, Constants.PRIMARY_KEY_TYPE, true);
    }

    /**
     * 生成数据库外键字段
     *
     * @return
     */
    public XTableField buildFkField(String fieldName) {
        return doBuildTableField(fieldName, Constants.PRIMARY_KEY_TYPE, false);
    }
}
