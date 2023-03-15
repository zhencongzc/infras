package com.cmbc.infras.system.service.impl;

import com.cmbc.infras.constant.AlarmStatisEnum;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.event.CountDoneResult;
import com.cmbc.infras.dto.event.SubBankGroup;
import com.cmbc.infras.dto.ops.AlarmStatistic;
import com.cmbc.infras.system.service.FlowFormService;
import com.cmbc.infras.system.service.HeadBankOpsService;
import com.cmbc.infras.system.service.HistoryAlarmService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class HeadBankOpsServiceImpl implements HeadBankOpsService {

    @Resource
    private FlowFormService flowFormService;

    @Resource
    private HistoryAlarmService historyAlarmService;

    @Override
    public BaseResult<List<AlarmStatistic>> getBankAlarmStatisticNew(String bankId) {
        List<AlarmStatistic> statis = new ArrayList<>();//结果
        SubBankGroup group = flowFormService.getSubBanksGroup(bankId);//银行层级结构
        //一级分行
        CountDoneResult result = historyAlarmService.countDoneWithLevel(Arrays.asList(group.getBankId()), "1,2,3");
        AlarmStatistic mainStatic = new AlarmStatistic(AlarmStatisEnum.MAIN, result.getCount(), result.getCountDone());
        statis.add(mainStatic);
        //二级分行
        if (group.getBranchs() != null && group.getBranchs().size() > 0) {
            CountDoneResult branchCdr = historyAlarmService.countDoneWithLevel(group.getBranchs(), "1,2,3");
            AlarmStatistic branchStatic = new AlarmStatistic(AlarmStatisEnum.BRANCH, branchCdr.getCount(), branchCdr.getCountDone());
            statis.add(branchStatic);
        }
        //三级支行
        if (group.getSubs() != null && group.getSubs().size() > 0) {
            CountDoneResult subCdr = historyAlarmService.countDoneWithLevel(group.getSubs(), "1,2,3");
            AlarmStatistic subStatic = new AlarmStatistic(AlarmStatisEnum.SUB, subCdr.getCount(), subCdr.getCountDone());
            statis.add(subStatic);
        }
        //四级村镇
        if (group.getTowns() != null && group.getTowns().size() > 0) {
            CountDoneResult townCdr = historyAlarmService.countDoneWithLevel(group.getTowns(), "1,2,3");
            AlarmStatistic townStatic = new AlarmStatistic(AlarmStatisEnum.TOWN, townCdr.getCount(), townCdr.getCountDone());
            statis.add(townStatic);
        }
        return BaseResult.success(statis);
    }

}
