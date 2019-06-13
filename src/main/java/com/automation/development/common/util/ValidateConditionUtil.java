package com.automation.development.common.util;

import com.automation.development.common.model.ValidateCondition;
import com.automation.development.common.model.XTableField;
import com.fasterxml.jackson.databind.JsonNode;
import com.xin.utils.StringUtil;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: ValidateCondition工具类
 * @date 2019-03-28 11:19
 */
public class ValidateConditionUtil {

    public static void setPattern(XTableField tableField, JsonNode jsonNode) {
        ValidateCondition validateCondition = tableField.getValidate();
        if (validateCondition == null) {
            validateCondition = new ValidateCondition();
            tableField.setValidate(validateCondition);
        }
        JsonNode patternJsonNode = jsonNode.get("pattern");
        if (patternJsonNode != null) {
            String pattern = patternJsonNode.asText();
            if (!StringUtil.isEmpty(pattern) && !"^(.*)$".equals(pattern)) {
                validateCondition.setPattern(pattern);
            }
        }
    }

    public static void setMinLength(XTableField tableField, JsonNode jsonNode) {
        ValidateCondition validateCondition = tableField.getValidate();
        if (validateCondition == null) {
            validateCondition = new ValidateCondition();
            tableField.setValidate(validateCondition);
        }
        JsonNode lengthJsonNode = jsonNode.get("minLength");
        if (lengthJsonNode != null) {
            int minLength = lengthJsonNode.asInt(0);
            if (minLength > 0) {
                validateCondition.setMinLength(minLength);
            }
        }
    }

    public static void setMaxLength(XTableField tableField, JsonNode jsonNode) {
        ValidateCondition validateCondition = tableField.getValidate();
        if (validateCondition == null) {
            validateCondition = new ValidateCondition();
            tableField.setValidate(validateCondition);
        }
        JsonNode lengthJsonNode = jsonNode.get("maxLength");
        if (lengthJsonNode != null) {
            int minLength = lengthJsonNode.asInt(0);
            if (minLength > 0) {
                validateCondition.setMaxLength(minLength);
            }
        }
    }

    public static void setMinValue(XTableField tableField, JsonNode jsonNode) {
        ValidateCondition validateCondition = tableField.getValidate();
        if (validateCondition == null) {
            validateCondition = new ValidateCondition();
            tableField.setValidate(validateCondition);
        }
        JsonNode minValueJsonNode = jsonNode.get("minimum");
        if (minValueJsonNode != null) {
            long minValue = minValueJsonNode.asLong(0);
            if (minValue > 0) {
                validateCondition.setMinValue(minValue);
            }
        }
    }

    public static void setMaxValue(XTableField tableField, JsonNode jsonNode) {
        ValidateCondition validateCondition = tableField.getValidate();
        if (validateCondition == null) {
            validateCondition = new ValidateCondition();
            tableField.setValidate(validateCondition);
        }
        JsonNode maxValueJsonNode = jsonNode.get("maximum");
        if (maxValueJsonNode != null) {
            long minValue = maxValueJsonNode.asLong(0);
            if (minValue > 0) {
                validateCondition.setMaxValue(minValue);
            }
        }
    }

    public static void addValid(XTableField tableField, String propertyName, ValidateCondition validate) {
        if (validate != null) {
            if (validate.getHasLengthRange()) {
                int minLen = validate.getMinLength() == -1 ? 0 : validate.getMinLength();
                int maxLen = validate.getMaxLength() == -1 ? Integer.MAX_VALUE : validate.getMaxLength();
                String sizeValid = String.format("@Size(min = %d, max = %d, message = \"The length of the %s must be between %d and %d.\")", minLen, maxLen, propertyName, minLen, maxLen);
                tableField.addValidateAnnotations(sizeValid);
            }

            if (validate.getHasValueRange()) {
                long minValue = validate.getMinValue() == -1 ? 0 : validate.getMinValue();
                if (validate.getMinValue() != -1) {
                    String minValueValid = String.format("@Min(value = %d, message = \"The minimum must of the %s be greater than %d.\")", minValue, propertyName, minValue);
                    tableField.addValidateAnnotations(minValueValid);
                }
                if (validate.getMaxValue() != -1) {
                    long maxValue = validate.getMaxValue() == -1 ? Long.MAX_VALUE : validate.getMaxValue();
                    String maxValueValid = String.format("@Max(value = %d, message = \"The maximum must of the %s be less than %d.\")", maxValue, propertyName, maxValue);
                    tableField.addValidateAnnotations(maxValueValid);
                }
            }
            if (validate.getHasPattern()) {
                String PatternValid = String.format("@Pattern(regexp = \"%s\", message = \"The value of field %s is not valid.\")", validate.getPattern(), propertyName);
                tableField.addValidateAnnotations(PatternValid);
            }
        }
    }
}
