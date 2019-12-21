package com.automation.development.common.model;

import biz.datainsights.utils.StringUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 校验条件
 * @date 2019-03-28 9:58
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@EqualsAndHashCode
public class ValidateCondition {

    private int minLength = -1;

    private int maxLength = -1;

    private String pattern;

    private String format;

    private long minValue = -1;

    private long maxValue = -1;

    /**
     * hasPattern
     *
     * @return
     */
    public boolean getHasPattern() {
        return !StringUtil.isEmpty(pattern);
    }

    /**
     * hasLengthRange
     *
     * @return
     */
    public boolean getHasLengthRange() {
        return -1 != minLength || -1 != maxLength;
    }

    /**
     * isDateType
     */
    public boolean getIsDateType() {
        return !StringUtil.isEmpty(format);
    }


    /**
     * hasLengthRange
     *
     * @return
     */
    public boolean getHasValueRange() {
        return -1 != minValue || -1 != maxValue;
    }
}
