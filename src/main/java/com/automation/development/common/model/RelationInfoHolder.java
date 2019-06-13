package com.automation.development.common.model;

import com.xin.utils.CollectionUtil;
import com.xin.utils.StringUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.*;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 中间表信息
 * @date 2019-03-26 19:43
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class RelationInfoHolder {

    private String middleTableName;

    private String relationColumn;

    private String relationToTable;

    private String relationTable;

    private String fieldName;

    private boolean isMany;

    private boolean isReachable;

    private boolean isCustomMiddleTable = false;

    private List<XTableField> tableFields = new ArrayList<>();

    private Map<String, XTableField> relationToColumnFields = new HashMap<>();

    public RelationInfoHolder addTableFields(Collection<XTableField> collection) {
        if (CollectionUtil.isEmpty(collection)) {
            return this;
        }
        tableFields.addAll(collection);
        return this;
    }

    public RelationInfoHolder addTableField(XTableField xTableField) {
        if (null == xTableField) {
            return this;
        }
        tableFields.add(xTableField);
        return this;
    }

    public RelationInfoHolder addRelationToColumnField(XTableField xTableField) {
        if (null == xTableField) {
            return this;
        }
        relationToColumnFields.put(xTableField.getName(), xTableField);
        return this;
    }

    public boolean isManyToMany() {
        boolean isValid = StringUtil.isEmpty(middleTableName) || StringUtil.isEmpty(relationColumn)
                || StringUtil.isEmpty(relationTable) || StringUtil.isEmpty(relationToTable)
                || relationToColumnFields.size() != 1;
        return !isValid;
    }

    public boolean hasMoreRelationToColumn() {
        return relationToColumnFields.size() >= 1;
    }
}
