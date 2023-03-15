package com.cmbc.infras.system.mapper;

import com.cmbc.infras.dto.Device;
import com.cmbc.infras.dto.monitor.Spot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeviceSpotMapper {

    List<Device> getBanksDevices(Map<String, Object> param);

    List<Device> getDevices(Map<String, Object> param);

    List<Spot> getDevicesSpots(List<String> deviceIds);

    List<Device> getDevByIdType(Map<String, Object> map);

    List<Spot> getDevSpots(Device device);

    Device getBankPue(String bankId);

    List<Device> getBanksPues(List<String> bankIds);

    List<Spot> getDevicesPueSpots(List<String> deviceIds);

    @Select("select spot_id spotId from tl_device_spot where device_id=#{deviceId} and spot_type=#{type}")
    String findSpotByDeviceIdAndType(String deviceId, int type);
}
