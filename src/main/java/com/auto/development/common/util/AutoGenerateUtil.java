package com.auto.development.common.util;

import com.auto.development.common.model.EnumField;
import com.auto.development.common.model.XTableField;
import com.auto.development.common.model.XTableInfo;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.xin.utils.CollectionUtil;
import com.xin.utils.DateUtil;
import com.xin.utils.StringUtil;

import java.util.*;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: TODO
 * @date 2019-05-11 10:22
 */
public class AutoGenerateUtil {

    /**
     * 获取创建PgSql表sql语句
     *
     * @return
     */
    public static List<String> getCreateTableSql(Map<String, XTableInfo> xTableInfoMap) {
        List<String> result = new ArrayList<>();
        Iterator<Map.Entry<String, XTableInfo>> iterator = xTableInfoMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, XTableInfo> entry = iterator.next();
            XTableInfo tableInfo = entry.getValue();
            result.addAll(TableHelper.getCreateTableSql(tableInfo));
        }
        return result;
    }

    public static List<String> generatePgSQLDataSqls(int size, Map<String, XTableInfo> tableInfos) {
        List<String> generatePgSQLDataSqls = new ArrayList<>(tableInfos.size());
        Iterator<Map.Entry<String, XTableInfo>> iterator = tableInfos.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, XTableInfo> entry = iterator.next();
            String tableName = entry.getKey();
            XTableInfo tableInfo = entry.getValue();
            if (tableInfo == null || tableInfo.getFields() == null) {
                continue;
            }
            StringBuilder insertSql = new StringBuilder("insert into ").append(tableName).append("(");
            StringBuilder valuesSql = new StringBuilder(" ) values ");
            List<TableField> fields = tableInfo.getFields();
            List<DbColumnType> dbColumnTypes = new ArrayList<>(fields.size());

            for (int i = 0; i < fields.size(); i++) {
                TableField field = fields.get(i);
                insertSql.append(field.getName());
                if (i != fields.size() - 1) {
                    insertSql.append(",");
                }
                if (field instanceof XTableField) {
                    XTableField xTableField = (XTableField) field;
                    if (xTableField.getColumnType() instanceof DbColumnType) {
                        DbColumnType dbColumnType = (DbColumnType) xTableField.getColumnType();
                        dbColumnTypes.add(dbColumnType);
                    }
                }
            }

            for (int i = 0; i < size; i++) {

                for (int j = 0; j < fields.size(); j++) {
                    TableField field = fields.get(j);
                    if (j == 0) {
                        valuesSql.append("(");
                    }

                    if (field instanceof XTableField) {
                        XTableField xTableField = (XTableField) field;
                        if (xTableField.getIsEnumField()) {
                            List<EnumField> enumFields = xTableField.getEnumInfo().getFields();
                            if (!CollectionUtil.isEmpty(enumFields)) {
                                int index = (int) (Math.random() * (enumFields.size()));
                                valuesSql.append("'").append(enumFields.get(index).getFieldCode()).append("'");
                                if (j == fields.size() - 1) {
                                    valuesSql.append(")");
                                } else {
                                    valuesSql.append(",");
                                }
                                continue;
                            }
                        }
                        if (xTableField.getColumnType() instanceof DbColumnType) {
                            DbColumnType dbColumnType = (DbColumnType) xTableField.getColumnType();
                            switch (dbColumnType) {
                                case INTEGER:
                                case LONG:
                                    if (tableInfo.isMidTable()) {
                                        valuesSql.append("'").append(i + 1).append("'");
                                    } else {
                                        valuesSql.append("'").append(i + 1).append("'");
                                    }
                                    break;
                                case STRING:
                                    valuesSql.append("'").append(StringUtil.getRandomString(6)).append("'");
                                    break;
                                case BOOLEAN:
                                    valuesSql.append("'").append(false).append("'");
                                    break;
                                case TIMESTAMP:
                                    valuesSql.append("'").append(DateUtil.format(new Date())).append("'");
                                    break;

                                default:
                                    break;
                            }
                        }
                    }

                    if (j == fields.size() - 1) {
                        valuesSql.append(")");
                    } else {
                        valuesSql.append(",");
                    }
                }

                if (i != size - 1) {
                    valuesSql.append(",");
                }
            }
            generatePgSQLDataSqls.add(insertSql.append(valuesSql).toString());
        }
        return generatePgSQLDataSqls;
    }
}
