package com.cmbc.infras.system.service;

import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.ops.AlarmStatistic;

import java.util.List;

public interface HeadBankOpsService {

    BaseResult<List<AlarmStatistic>> getBankAlarmStatisticNew(String bankId);

}
