package com.cmbc.infras.system.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SpotMapper {

    List<String> getDevicesSpotIds(List<String> devIds);

    List<String> getSpotsDevices(List<String> spots);

}
