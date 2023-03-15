package com.cmbc.infras.system.service.impl;

import com.cmbc.infras.constant.DeviceTypeEnum;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.Device;
import com.cmbc.infras.dto.monitor.MonitorParam;
import com.cmbc.infras.dto.monitor.Spot;
import com.cmbc.infras.system.mapper.MonitorMapper;
import com.cmbc.infras.system.service.AriService;
import com.cmbc.infras.system.service.MonitorService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class AriServiceImpl implements AriService {

    @Resource
    private MonitorMapper monitorMapper;

    @Resource
    private MonitorService monitorService;

    /**
     * 取得某账号下某银行下空调设备测点的resourceIds
     */
    public BaseResult<List<String>> getUserAirSpot(String bankId) {
        List<String> resourceIds = new ArrayList<>();
        List<Device> devices = new ArrayList<>();
        if (StringUtils.isNotBlank(bankId))
            devices = monitorService.getBankDevice(bankId, DeviceTypeEnum.AIR.getType());
        if (devices.size() == 0) return BaseResult.success(resourceIds);
        List<String> devIds = new ArrayList<>();
        for (Device dev : devices) {
            devIds.add(dev.getDeviceId());
        }
        List<Spot> spots = monitorMapper.getDevsSpots(devIds);
        if (spots.size() == 0) return BaseResult.success(resourceIds);
        for (Spot spot : spots) {
            resourceIds.add(spot.getSpotId());
        }
        return BaseResult.success(resourceIds, resourceIds.size());
    }

}
