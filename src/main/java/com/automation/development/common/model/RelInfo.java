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
public class RelInfo implements Comparable<RelInfo> {

    private String midTable;

    private String midEntry;

    private String midProperty;

    private String relColumn;

    private String relUri;

    private String relTable;

    private String relTablePk;

    private String relEntry;

    private String relProperty;

    private String relToTable;

    private String relToColumn;

    private String relToUri;

    private String relToEntry;

    private String relToProperty;

    private String relToTablePk;

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
        RelInfo that = (RelInfo) o;
        return Objects.equals(relColumn, that.relColumn) &&
                Objects.equals(relToColumn, that.relToColumn) &&
                Objects.equals(midTable, that.midTable) &&
                Objects.equals(relToTable, that.relToTable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relColumn, relToColumn, midTable, relToTable);
    }

    @Override
    public int compareTo(RelInfo o) {
        if (o == null) {
            return 0;
        }
        String text1 = this.relTable + this.relToTable;
        String text2 = o.relTable + o.relToTable;
        if (text1.equals(text2)) {
            return 0;
        }

        return text1.hashCode() - text2.hashCode();
    }
}
