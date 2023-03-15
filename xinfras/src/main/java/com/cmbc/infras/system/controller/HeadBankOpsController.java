package com.cmbc.infras.system.controller;

import com.cmbc.infras.dto.BaseParam;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.ops.AlarmStatistic;
import com.cmbc.infras.system.service.HeadBankOpsService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 总行运维视图
 * 分行,二级分行,支行,村镇-信息
 */
@RestController
public class HeadBankOpsController {

    @Resource
    private HeadBankOpsService headBankOpsService;

    /**
     * 分行信息（总行运维界面）
     */
    @RequestMapping("/braops/subsBanksInfo")
    public BaseResult<List<AlarmStatistic>> getSubsBankInfos(BaseParam param) {
        return headBankOpsService.getBankAlarmStatisticNew(param.getBankId());
    }

}
