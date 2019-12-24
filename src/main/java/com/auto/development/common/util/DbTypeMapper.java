package com.auto.development.common.util;


import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: TODO
 * @date 2019-05-09 17:27
 */
public class DbTypeMapper {

    public static String getPgSqlType(DbColumnType dbColumnType) {
        switch (dbColumnType) {
            case INTEGER:
                return " integer";
            case BOOLEAN:
                return " boolean";
            case LONG:
                return " bigint";
            case DOUBLE:
                return " float8";
            case STRING:
                return " character varying(100) COLLATE pg_catalog.\"default\"";
            case TIMESTAMP:
                return " timestamp(6) with time zone";
            default:
                return " character varying(100) COLLATE pg_catalog.\"default\"";
        }
    }

    public static String getMySqlType(DbColumnType dbColumnType) {
        switch (dbColumnType) {
            case INTEGER:
                return " int(11) ";
            case LONG:
                return " bigint";
            case STRING:
                return " varchar(100) ";
            case TIMESTAMP:
                return " timestamp default CURRENT_TIMESTAMP ";
            default:
                return " varchar(100) ";
        }
    }

    public static DbColumnType getDbColumnType(Class<?> clazz) {
        switch (clazz.getName()) {
            case "java.lang.Integer":
                return DbColumnType.INTEGER;

            case "java.lang.Boolean":
                return DbColumnType.BOOLEAN;

            case "java.lang.Long":
                return DbColumnType.LONG;

            case "java.util.Date":
            case "java.sql.Timestamp":
                return DbColumnType.TIMESTAMP;

            case "java.math.BigInteger":
                return DbColumnType.BIG_INTEGER;

            case "java.math.BigDecimal":
                return DbColumnType.BIG_DECIMAL;

            case "java.lang.Double":
                return DbColumnType.DOUBLE;

            case "java.lang.Float":
                return DbColumnType.FLOAT;

            case "java.time.LocalDateTime":
                return DbColumnType.LOCAL_TIME;

            default:
                return DbColumnType.STRING;
        }
    }
}
