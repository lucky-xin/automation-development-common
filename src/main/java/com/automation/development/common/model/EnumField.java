package com.automation.development.common.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: Enum字段的属性信息
 * @date 2019-04-15 13:54
 */
@Data
@Accessors(chain = true)
public class EnumField {

    private String fieldName;

    private Object fieldValue;

    private int fieldCode;
}
