package com.automation.development.common.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 关联信息类
 * @date 2019-03-26 19:43
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class RelationInfo implements Comparable<RelationInfo> {

    private String middleTable;

    private String middleEntry;

    private String middleProperty;

    private String relationColumn;

    private String relationUri;

    private String relationTable;

    private String relationTablePk;

    private String relationEntry;

    private String relationProperty;

    private String relationToTable;

    private String relationToColumn;

    private String relationToUri;

    private String relationToEntry;

    private String relationToProperty;

    private String relationToTablePk;

    private boolean many;

    private boolean reachable;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RelationInfo that = (RelationInfo) o;
        return Objects.equals(relationColumn, that.relationColumn) &&
                Objects.equals(relationToColumn, that.relationToColumn) &&
                Objects.equals(middleTable, that.middleTable) &&
                Objects.equals(relationToTable, that.relationToTable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationColumn, relationToColumn, middleTable, relationToTable);
    }

    @Override
    public int compareTo(RelationInfo o) {
        if (o == null) {
            return 0;
        }
        String text1 = this.relationTable + this.relationToTable;
        String text2 = o.relationTable + o.relationToTable;
        if (text1.equals(text2)) {
            return 0;
        }

        return text1.hashCode() - text2.hashCode();
    }
}
