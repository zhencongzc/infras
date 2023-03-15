package com.cmbc.infras.system.service;

import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.Device;
import com.cmbc.infras.dto.event.SubBankGroup;

import java.util.List;

public interface BankService {

    List<String> getBankIds(List<Bank> list);

    List<Device> getBankDevices(List<String> bankIds);

    List<String> getBanksSpots(List<Bank> banks);

    @Deprecated//切换到FlowFormService
    List<Bank> getSubBanks(String bankId);

    List<Bank> getSubBanks(List<String> bankIds);

    List<String> getSubBankIds(List<String> bankIds);

    @Deprecated
    List<Bank> getSubBanksBatch(List<String> bankIds);

    /**
     * 告警统计改造-部分********************************************
     * 统计数据逻辑变更-不在查解行/设备/测点
     * 查告警只根据银行ID,查银行ID下所有告警
     */

    @Deprecated//切换到FlowFormService
    SubBankGroup getSubBanksGroup(String bankId);

    Bank getBankById(String bankId);

//    @Deprecated//切换到FlowFormService
//    List<Bank> getSubBanksById(String bankId, String sessionId);
//
//    @Deprecated//切换到FlowFormService
//    List<Bank> getBanksByLevel(int level, String sessionId);
}
