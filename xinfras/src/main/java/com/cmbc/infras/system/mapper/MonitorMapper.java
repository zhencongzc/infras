package com.cmbc.infras.system.mapper;

import com.cmbc.infras.dto.monitor.Spot;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MonitorMapper {

    List<Spot> getDevSpots(String deviceId);

    List<Spot> getDevsSpots(List<String> ids);

}
