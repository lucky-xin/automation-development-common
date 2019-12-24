package com.auto.development.common.service;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 分布式id服务
 * @date 2019-01-16 9:58
 */
public interface DistributeIdService {

    Long nextId(String tableName, long step);

    String getDistributeIdKey(String name);
}
