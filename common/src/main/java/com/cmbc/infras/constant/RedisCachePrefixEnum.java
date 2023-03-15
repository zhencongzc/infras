package com.cmbc.infras.constant;

import java.util.StringJoiner;

/**
 * @desc 缓存key 模块前缀定义
 */
public enum RedisCachePrefixEnum {

    /**
     * 所属项目
     */
    PROJECT("infras"),
    CODE("code");
    //USER("user");

    public String module;

    RedisCachePrefixEnum(String module){
        this.module = module;
    }

    /**
     * 创建Redis缓存的key
     * @param redisCachePrefixEnum 所属的模块
     * @param key Redis的key
     * @return 创建好的key
     */
    public static String createRedisKey(RedisCachePrefixEnum redisCachePrefixEnum,String key){
        StringJoiner joiner = new StringJoiner(":");
        joiner.add(PROJECT.module);
        joiner.add(redisCachePrefixEnum.module).add(key);
        return joiner.toString();
    }

}
