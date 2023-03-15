package com.cmbc.infras.system.service.impl;

import com.cmbc.infras.dto.BaseParam;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.ops.BankTime;
import com.cmbc.infras.system.mapper.BankTimeMapper;
import com.cmbc.infras.system.service.BankService;
import com.cmbc.infras.system.service.BankTimeService;
import com.cmbc.infras.util.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class BankTimeServiceImpl implements BankTimeService {

    @Resource
    private BankTimeMapper bankTimeMapper;

    @Resource
    private BankService bankService;

    @Override
    public BaseResult<BankTime> getBankTime(BaseParam param) {
        String bankId = param.getBankId();
        if (StringUtils.isBlank(bankId)) bankId = UserContext.getUserBankId();
        BankTime bankTime = bankTimeMapper.getBankTime(bankId);
        return BaseResult.success(bankTime);
    }
}
