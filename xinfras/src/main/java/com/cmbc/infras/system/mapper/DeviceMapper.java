package com.cmbc.infras.system.mapper;

import com.cmbc.infras.dto.Device;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DeviceMapper {

    //List<String> getBanksBeatDeviceIds(List<String> bankIds);

    Device getBanksBeatDeviceId(String bankId);

    List<String> getDevicesBanks(List<String> deviceIds);

}
