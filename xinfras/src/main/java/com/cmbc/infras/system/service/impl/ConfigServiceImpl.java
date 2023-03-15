package com.cmbc.infras.system.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.cmbc.infras.redis.DataRedisUtil;
import com.cmbc.infras.system.mapper.ConfigMapper;
import com.cmbc.infras.system.service.ConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ConfigServiceImpl implements ConfigService {

    //1小时超时
    private int EXPIRE_TIME = 60*1000;

    @Resource
    private ConfigMapper configMapper;

    /**
     * 前台告警等级可配置
     * 查询配置的推送告警等级
     */
    @Override
    public List<Integer> getAlarmLevelShow() {

        //默认使用admin配置等级
        String account = "admin";

        String key = "system:alarm_level:" + account;
        String val = DataRedisUtil.getStringFromRedis(key);
        if (StringUtils.isNotBlank(val)) {
            List<Integer> list = JSONArray.parseArray(val, Integer.class);
            if (!list.isEmpty()) {
                return list;
            }
        }
        List<Integer> levels = configMapper.getAlarmLevelShow(account);
        DataRedisUtil.addStringToRedis(key, JSONArray.toJSONString(levels), EXPIRE_TIME);
        return levels;
    }

}
