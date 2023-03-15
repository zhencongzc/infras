package com.cmbc.infras.system.controller;

import com.cmbc.infras.dto.BaseParam;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.ops.BankTime;
import com.cmbc.infras.system.service.BankTimeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 机房安全运行时间
 */
@RestController
public class BankTimeController {

    @Resource
    private BankTimeService bankTimeService;

    /**
     * 机房安全运行时间
     */
    //@ExecuteAnnotation
    @RequestMapping("/bank/bankTime")
    public BaseResult<BankTime> getBankTime(BaseParam param) {
        return bankTimeService.getBankTime(param);
    }

}
