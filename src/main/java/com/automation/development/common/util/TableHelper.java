package com.automation.development.common.util;

import com.automation.development.common.model.*;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.xin.utils.StringUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
public class TableHelper {

	public static XTableInfo getTableInfo(Class<?> clazz) {
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

	private static void addField(Class<?> clazz, XTableInfo xTableInfo) {
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
				XTableField tableField = buildTableField(tableId.value(), Constants.PRIMARY_KEY_TYPE, true);
				tableField.setComment(comment);
				xTableInfo.addField(tableField);
				continue;
			}
			XTableField tableField = buildTableField(getColumnName(declaredField.getName()), DbTypeMapper.getDbColumnType(declaredField.getType()), false);
			tableField.setComment(comment);
			xTableInfo.addField(tableField);

		}
	}

	public static List<String> getCreateTableSql(XTableInfo tableInfo) {
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

	private static List<String> getCreateMySqlStatements(XTableInfo tableInfo) {
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

	public static List<String> getCreatePgSqlStatements(XTableInfo tableInfo) {
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

	private static String getTableComment(XTableInfo tableInfo) {
		if (tableInfo == null) {
			return "";
		}
		if (!StringUtil.isEmpty(tableInfo.getComment())) {
			return tableInfo.getComment();
		}
		return tableInfo.getName();
	}

	public static String getAddColumnSql(com.baomidou.mybatisplus.generator.config.po.TableField field, DbType dbType) {
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

	public static String getAddCommentSql(String tableName, String fieldName, String comment) {
		String sql = String.format(" comment on column %s.%s is '%s'", tableName, fieldName, comment);
		return sql;
	}

	public static void initTable(JsonNode schema, String currentTable, XTableInfo tableInfo) {
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

	public static void initTable(String currentTable, XTableInfo tableInfo) {
		initTable(null, currentTable, tableInfo);
	}

	/**
	 * 判断是否为主键默认字段名称为id为表的主键或者指定isPrimaryKey为true
	 *
	 * @param name
	 * @param jsonNode
	 * @return
	 */
	public static boolean isPrimaryKey(String name, JsonNode jsonNode) {
		if (isPrimaryKey(name)) {
			return true;
		}
		if (jsonNode == null) {
			return false;
		}
		String text = null;
		if (jsonNode.get("isPrimaryKey") instanceof TextNode) {
			text = jsonNode.get("isPrimaryKey").asText();
		}
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
	public static XTableField buildTableField(String fieldName, DbColumnType dbColumnType, boolean keyFlag) {
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
	public static XTableField buildTableField(String fieldName, JsonNode jsonNode, boolean keyFlag) {
		fieldName = getColumnName(fieldName);
		if (!keyFlag) {
			keyFlag = "id".equals(fieldName);
		}
		DbColumnType iColumnType = keyFlag ? Constants.PRIMARY_KEY_TYPE : getDbColumnType(fieldName, jsonNode);
		return doBuildTableField(fieldName, iColumnType, keyFlag);
	}

	private static XTableField doBuildTableField(String fieldName, DbColumnType dbColumnType, boolean keyFlag) {
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
	public static void addTableField(XTableInfo tableInfo, JsonNode jsonNode, String fieldName) {
		if (tableInfo == null) {
			return;
		}
		/**
		 * 把字段名称转化为小写下斜杠格式
		 */
		fieldName = getColumnName(fieldName);
		boolean isPrimaryKey = isPrimaryKey(fieldName, jsonNode);
		XTableField tableField = buildTableField(fieldName, jsonNode, isPrimaryKey);
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
			JsonNode enumNode = jsonNode.get("enum");
			if (enumNode != null && enumNode.isArray()) {
				addEnumInfo(tableField, enumNode);
			}
		}
		if (tableField.getIsEnumField()) {
			tableField.setColumnType(DbColumnType.INTEGER);
		}
		tableInfo.addField(tableField);
	}

	public static void addDefaultPrimaryKey(XTableInfo tableInfo) {
		XTableField idField = TableHelper.buildTableField("id", Constants.PRIMARY_KEY_TYPE, true);
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
	private static DbColumnType getDbColumnType(String fieldName, JsonNode jsonNode) {
		/**
		 * 如果为主键直接返回主键类型
		 */
		if (isPrimaryKey(fieldName)) {
			return Constants.PRIMARY_KEY_TYPE;
		}
		boolean isDate = isDateJsonNode(fieldName, jsonNode);
		if (isDate) {
			return DbColumnType.TIMESTAMP;
		}
		DbColumnType dbColumnType = DbColumnType.STRING;
		if (jsonNode == null || jsonNode.get("type") == null) {
			return dbColumnType;
		}
		JsonNode typeNode = jsonNode.get("type");
		return getDbColumnType(typeNode.asText());
	}

	private static DbColumnType getDbColumnType(String nodeType) {
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

	private static boolean isPrimaryKey(String fieldName) {
		return "id".equals(fieldName);
	}

	private static Pattern englishTextPattern = Pattern.compile("(^[a-zA-Z]+)");

	private static void addEnumInfo(XTableField tableField, JsonNode enumNode) {
		if (tableField == null || enumNode == null || !enumNode.isArray()) {
			return;
		}
		ArrayNode arrayNode = (ArrayNode) enumNode;
		Iterator<JsonNode> iterator = arrayNode.elements();
		List<EnumField> fields = new ArrayList<>();
		String fieldName = tableField.getName();
		EnumInfo enumInfo = new EnumInfo().setFieldName(fieldName);
		DbColumnType dbColumnType = null;

		while (iterator.hasNext()) {
			JsonNode element = iterator.next();
			EnumField enumField = new EnumField();
			if (element.isTextual()) {
				String textValue = element.asText();
				if (englishTextPattern.matcher(textValue).find()) {
					String entryName = getColumnName(textValue);
					if (StringUtil.isEmpty(entryName)) {
						continue;
					}
					enumField.setFieldName(entryName.toUpperCase()).setFieldValue(textValue).setFieldCode(fields.size() + 1);
					fields.add(enumField);
					dbColumnType = DbColumnType.STRING;
					continue;
				}
			}

			if (element.isInt() || element.isLong()) {
				dbColumnType = DbColumnType.LONG;
				enumField.setFieldValue(element.asLong(0));
			} else if (element.isFloat() || element.isDouble()) {
				enumField.setFieldValue(element.asDouble(0));
				dbColumnType = DbColumnType.DOUBLE;
			}
			fields.add(enumField);
		}
		enumInfo.setValueType(dbColumnType)
				.setEntryName(getEntryName(fieldName))
				.setPropertyName(getPropertyName(fieldName))
				.setFields(fields);
		tableField.setEnumInfo(enumInfo);
	}

	private static Pattern commentPattern = Pattern.compile("(^[\bThe\b.*\bSchema\b])");

	private static boolean isParentId(String fieldName) {
		return "parentId".equals(fieldName) || "parent_id".equals(fieldName);
	}

	public static String getComment(String fieldName, JsonNode jsonNode) {
		if (jsonNode == null) {
			if (!StringUtil.isEmpty(fieldName)) {
				fieldName = getColumnName(fieldName);
				return fieldName.replace("_", " ");
			}
			return "";
		}

		JsonNode titleNode = jsonNode.get("title");
		if (titleNode == null) {
			return "";
		}
		String text = titleNode.asText();
		if (commentPattern.matcher(text).find()) {
			if (StringUtil.isEmpty(fieldName)) {
				return text.replace("The", "").replace("Schema", "").trim();
			}
			return fieldName.replace("_", " ");
		}
		return titleNode.asText();
	}

	public static String getComment(String fieldName) {
		if (!StringUtil.isEmpty(fieldName)) {
			fieldName = getColumnName(fieldName);
			return fieldName.replace("_", " ");
		}
		return "";
	}

	public static String getEntryName(String fieldName) {
		if (StringUtil.isEmpty(fieldName)) {
			return "";
		}
		if (fieldName.contains("_")) {
			fieldName = StringUtil.underlineToLowerCamel(fieldName);
		}
		return StringUtil.upperCaseFirstChar(fieldName);
	}

	public static String getPropertyName(String name) {
		if (StringUtil.isEmpty(name)) {
			return "";
		}
		return StringUtil.underlineToLowerCamel(name);
	}

	public static String getUri(String fieldName) {
		if (StringUtil.isEmpty(fieldName)) {
			return "";
		}
		String columnName = getColumnName(fieldName);
		return columnName.replace("_", "-");
	}

	public static String getEndWithIdFieldName(String field) {
		if (field == null) {
			return null;
		}

		if (field.endsWith("Id")) {
			String text = field.substring(0, field.lastIndexOf("Id"));
			return StringUtil.lowerCamelToUnderline(text);
		}

		if (field.endsWith("_id")) {
			String text = field.substring(0, field.lastIndexOf("_id"));
			return StringUtil.lowerCamelToUnderline(text);
		}
		return null;
	}

	public static boolean isDateJsonNode(String fieldName, JsonNode jsonNode) {
		if (!StringUtil.isEmpty(fieldName) && fieldName.endsWith("_time")) {
			return true;
		}
		if (jsonNode == null) {
			return false;
		}
		if ((jsonNode.get("javaType") instanceof TextNode)) {
			String text = jsonNode.get("javaType").asText().toLowerCase();
			if (text.contains("date")) {
				return true;
			}
		}

		JsonNode formatNode = jsonNode.get("format");
		if (formatNode != null) {
			String format = formatNode.asText();
			if ("date-time".equals(format) || "time".equals(format) || "date".equals(format)) {
				return true;
			}
		}
		return false;
	}

	private static Pattern pattern = Pattern.compile("^[A-Z_]+$");

	public static String getColumnName(String relationProperty) {
		if (pattern.matcher(relationProperty).find()) {
			return relationProperty.toLowerCase();
		}
		return StringUtil.lowerCamelToUnderline(relationProperty);
	}

	public static RelationInfoHolder buildRelationInfoHolder(String currentTable,
															 String fieldName,
															 Iterator<JsonNode> elements,
															 Set<String> cacheTableNames,
															 JsonNode parentProperties,
															 JsonNode currentProperties) {
		fieldName = getColumnName(fieldName);
		RelationInfoHolder relationInfoHolder = new RelationInfoHolder()
				.setRelationTable(currentTable).setFieldName(fieldName);
		while (elements.hasNext()) {
			JsonNode requiredNode = elements.next();
			String requireText = requiredNode.asText();
			if (StringUtil.isEmpty(requireText)) {
				continue;
			}
			/**
			 *  获取以_id或者Id结尾的字段，如companyId返回结果为company如果不是这两种情况则返回null
			 */

			String propertyField = TableHelper.getEndWithIdFieldName(requireText);
			/**
			 *  添加该字段
			 */
			XTableField tableField = null;
			if (currentProperties != null) {
				JsonNode requireNode = currentProperties.get(requireText);
				tableField = TableHelper.buildTableField(requireText, requireNode, false);
			}

			boolean bool = currentTable.equals(propertyField);
			String field = getColumnName(requireText);

			if (bool) {
				/**
				 * 如果获取的字段和当前节点名称一致则有可能是中间表，添加中间表信息
				 */
				relationInfoHolder.setMiddleTableName(fieldName)
						.setRelationColumn(field);
				if (tableField != null) {
					tableField.setKeyFlag(true);
					tableField.setColumnType(Constants.PRIMARY_KEY_TYPE);
				}
			} else if (!StringUtil.isEmpty(propertyField)) {
				relationInfoHolder.setRelationToTable(propertyField);
				/**
				 * 判断父节点是否有该字段或者缓存之中是否又该表
				 */
				boolean isRelationField = (parentProperties != null && parentProperties.has(propertyField))
						|| cacheTableNames != null && cacheTableNames.contains(propertyField);
				if (isRelationField && tableField != null) {
					tableField.setKeyFlag(true);
					tableField.setColumnType(Constants.PRIMARY_KEY_TYPE);
					relationInfoHolder.addRelationToColumnField(tableField);
				}
			}

			if (tableField != null) {
				relationInfoHolder.addTableField(tableField);
			}
		}

		return relationInfoHolder;
	}

	public static RelationInfo buildRelationInfo(boolean isMany, String relationTable, String relationColumn, String relationToTable, String relationToColumn) {
		RelationInfo relationInfo = new RelationInfo()
				.setMany(isMany)
				.setRelationEntry(getEntryName(relationTable))
				.setRelationProperty(getPropertyName(relationTable))
				.setRelationTable(relationTable)
				.setRelationColumn(relationColumn)
				.setRelationTablePk("id")
				.setRelationUri(getUri(relationTable))
				.setRelationToEntry(getEntryName(relationToTable))
				.setRelationToProperty(getPropertyName(relationToTable))
				.setRelationToTable(relationToTable)
				.setRelationToColumn(relationToColumn)
				.setRelationToTablePk("id")
				.setRelationToUri(getUri(relationToTable));
		return relationInfo;
	}

	public static String getMiddleTableName(String relationTable, String relationToTable) {
		return relationTable + "_" + relationToTable;
	}

	public static RelationInfoHolder builderRelationInfoHolder(boolean isMany, String currentTable, String relationToTable, String middleTableName) {
		String relationColumn = currentTable + "_id";
		String relationToColumn = relationToTable + "_id";
		XTableField relationToField = TableHelper.buildTableField(relationToColumn, Constants.PRIMARY_KEY_TYPE, true);
		middleTableName = StringUtil.isEmpty(middleTableName) ? TableHelper.getMiddleTableName(currentTable, relationToTable) : middleTableName;
		RelationInfoHolder manyToManyRelationInfoHolder = new RelationInfoHolder()
				.setRelationTable(currentTable)
				.setRelationColumn(relationColumn)
				.setMiddleTableName(middleTableName)
				.addRelationToColumnField(relationToField)
				.setRelationToTable(relationToTable)
				.setMany(isMany);
		XTableField relationField = TableHelper.buildTableField(relationColumn, Constants.PRIMARY_KEY_TYPE, true);
		manyToManyRelationInfoHolder.addTableField(relationField);
		manyToManyRelationInfoHolder.addTableField(relationToField);
		return manyToManyRelationInfoHolder;
	}
}
