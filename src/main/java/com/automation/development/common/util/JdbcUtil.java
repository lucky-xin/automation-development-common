package com.automation.development.common.util;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: TODO
 * @date 2019-05-10 16:29
 */
public class JdbcUtil {

    public static String getSchemaName(String databaseUrl) {
        int index = databaseUrl.indexOf("?");
        if (index > 0) {
            return databaseUrl.substring(databaseUrl.lastIndexOf("/") + 1, index);
        }
        return databaseUrl.substring(databaseUrl.lastIndexOf("/") + 1);
    }

    public static <T> void executeQuery(DataSource dataSource
            , ResultSetConsumer<T> consumer
            , String sql
            , Object... parameters) throws SQLException {

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);) {

            for (int i = 0; i < parameters.length; ++i) {
                stmt.setObject(i + 1, parameters[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    if (consumer != null) {
                        T object = consumer.apply(rs);
                        consumer.accept(object);
                    }
                }
            }
        }
    }

    public static String getDbName(String url) {
        int startIndex = url.lastIndexOf("/");
        int endIndex = url.indexOf("?");
        if (endIndex == -1) {
            endIndex = url.length();
        }
        String dbName = url.substring(startIndex + 1, endIndex);
        return dbName;
    }
}
