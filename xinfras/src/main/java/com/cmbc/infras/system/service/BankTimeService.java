package com.cmbc.infras.system.service;

import com.cmbc.infras.dto.BaseParam;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.ops.BankTime;

public interface BankTimeService {

    BaseResult<BankTime> getBankTime(BaseParam param);

}
