package com.cmbc.infras.system.controller;

import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.BaseParam;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.ops.OpsBankInfo;
import com.cmbc.infras.dto.rpc.event.AlarmEvent;
import com.cmbc.infras.system.service.BranchOpsService;
import com.cmbc.infras.system.service.FlowFormService;
import com.cmbc.infras.util.UserContext;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 分行运维
 */
@RestController
public class BranchOpsController {

    /**
     * 替代BankService
     * Bank相关查询切换到流程表单
     */
    @Resource
    private FlowFormService flowFormService;

    @Resource
    private BranchOpsService branchOpsService;

    /**
     * 分行3D图-查银行-有PUE
     * 分行运维-查银行-无PUE
     */
    @RequestMapping("/braops/allBanks")
    public BaseResult<List<Bank>> getAllBank(BaseParam param) {
        return branchOpsService.getAllBanks(param);
    }

    /**
     * 分行运维-下级分行信息
     */
    @RequestMapping("/braops/bankInfos")
    public BaseResult<OpsBankInfo> getBankInfos(BaseParam param) {
        try {
            BaseResult<OpsBankInfo> bankInfos = branchOpsService.getBankInfos(param);
            return bankInfos;
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 实时告警
     * 查询当天的实时告警，有bankId进行过虑
     * 默认[紧急,重要]
     */
    @RequestMapping("/braops/alarmInfos")
    public BaseResult<List<AlarmEvent>> getAlarmInfos(BaseParam baseParam) {
        return branchOpsService.getAlarmInfos(baseParam);
    }

    /**
     * 分行账号取得分行名称-专用接口
     * 总行运维跳转分行运维时-能展示分行名称
     * 分行账户直接进入分行运维界面-没有分行名称
     * 提供一个接口,供前端取得分行账号的分行名称用
     */
    @RequestMapping("/braops/bankName")
    public BaseResult<String> getUserBankName() {
        String bankId = UserContext.getUserBankId();
        String sessionId = UserContext.getAuthToken();
        Assert.hasLength(bankId, "UserContext取得bankId为空！");
        Bank bank = flowFormService.getBankById(bankId, sessionId);
        Assert.notNull(bank, "bankId[" + bankId + "]查询银行为空");
        String bankName = bank.getBankName();
        return BaseResult.success(bankName);
    }

}
