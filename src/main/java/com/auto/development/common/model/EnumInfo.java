package com.auto.development.common.model;

import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: TODO
 * @date 2019-04-14 17:39
 */
@Data
@Accessors(chain = true)
public class EnumInfo {

    private String propertyName;

    private String entryName;

    private String fieldName;

    private List<EnumField> fields = new ArrayList<>();

    private DbColumnType valueType;

    public EnumInfo addField(EnumField enumField) {
        fields.add(enumField);
        return this;
    }

}
