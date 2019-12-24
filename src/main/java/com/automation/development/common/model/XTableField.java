package com.automation.development.common.model;

import com.automation.development.common.util.TableHelper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.xin.utils.StringUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.ibatis.type.JdbcType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: TableField拓展
 * @date 2019-03-28 9:51
 */

@Getter
@Setter
@ToString
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class XTableField extends TableField {

    private ValidateCondition validate;

    private EnumInfo enumInfo;

    private JdbcType jdbcType;

    private Long length;

    /**
     * 所有校验注解
     */
    private List<String> validateAnnotations = new ArrayList<>();

    private String relLabel;

    /**
     * 按JavaBean规则来生成get和set方法
     */
    @Override
    public String getCapitalName() {
        String propertyName = getPropertyName();
        if (StringUtil.isEmpty(propertyName)) {
            propertyName = TableHelper.getPropertyName(getName());
        }
        if (propertyName.length() <= 1) {
            return propertyName.toUpperCase();
        }
        String setGetName = propertyName;
        if (DbColumnType.BASE_BOOLEAN.getType().equalsIgnoreCase(getColumnType().getType())) {
            setGetName = StringUtils.removeIsPrefixIfBoolean(setGetName, Boolean.class);
        }
        // 第一个字母 小写、 第二个字母 大写 ，特殊处理
        String firstChar = setGetName.substring(0, 1);
        if (Character.isLowerCase(firstChar.toCharArray()[0])
                && Character.isUpperCase(setGetName.substring(1, 2).toCharArray()[0])) {
            return firstChar.toLowerCase() + setGetName.substring(1);
        }
        return firstChar.toUpperCase() + setGetName.substring(1);
    }

    public XTableField addValidateAnnotations(String validateAnnotation) {
        validateAnnotations.add(validateAnnotation);
        return this;
    }

    public boolean getIsEnumField() {
        return enumInfo != null && enumInfo.getValueType() == DbColumnType.STRING;
    }

    public String getEnumEntryName() {
        if (getIsEnumField()) {
            return enumInfo.getEntryName();
        }
        return "";
    }

    public String getJdbcTypeText() {
        return jdbcType.name();
    }
}
