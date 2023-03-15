package com.cmbc.infras.system.controller;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.BaseParam;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.system.service.BranchOpsService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 地图展示分行controller
 * 分行基础设施运行状态
 */
@RestController
public class BankMapController {

    @Resource
    private BranchOpsService branchOpsService;

    /**
     * 总行主界面-3D图-有PUE
     * 总行运维-查银行-有PUE
     */
    @RequestMapping("/map/lowerBanks")
    public BaseResult<List<Bank>> getLowerBanks(BaseParam param) {
        return branchOpsService.getLowerBank(param);
    }

    /**
     * 资产情况
     */
    @RequestMapping("/map/bankAsset")
    public BaseResult<JSONObject> getBankAsset(BaseParam param) {
        try {
            return branchOpsService.getBankAsset(param);
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

}
