package com.cmbc.infras.system.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ConfigMapper {

    List<Integer> getAlarmLevelShow(String account);

}
