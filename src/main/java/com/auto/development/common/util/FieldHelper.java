package com.auto.development.common.util;


import com.xin.utils.StringUtil;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 构建tableInfo 帮助类
 * @date 2019-04-08 17:20
 */
public class FieldHelper {

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
        return columnName.replace("_" , "-");
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

    public static String getColumnName(String relationProperty) {
        return StringUtil.lowerCamelToUnderline(relationProperty);
    }

}
