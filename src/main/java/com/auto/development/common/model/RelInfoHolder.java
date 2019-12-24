package com.auto.development.common.model;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

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
public class RelInfoHolder {

    private String relName;

    private String midTableName;

    private String relColumn;

    private String relToColumn;

    private String relToTable;

    private String relTable;

    private boolean isMany;

    private boolean isReachable;
}
