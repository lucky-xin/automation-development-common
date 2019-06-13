package com.automation.development.common.util;

import lombok.Data;

import java.util.Collection;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: json请求批量id参数
 * @date 2019-05-07
 */
@Data
public class IdParams {

    private Collection<Long> ids;
}
