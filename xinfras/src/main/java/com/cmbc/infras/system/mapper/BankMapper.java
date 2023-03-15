package com.cmbc.infras.system.mapper;

import com.cmbc.infras.dto.Bank;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BankMapper {

    Bank getBankById(String bankId);
    /**
     * 通过父ID查询下级银行
     * bankService.getSubBanksById替代
     */
    List<Bank> getSubBanksById(String bankId);

    List<Bank> getSubBanks(List<String> bankIds);

    List<String> getSubBankIds(List<String> bankIds);

    /**
     * bankService.getBanksByLevel替代
     */
    List<Bank> getBanksByLevel(int level);

    /**
     * bankService.getBanksByLevel替代
     */
    List<Bank> getBanksByLevelLink(int level);

    /***历史告警统计改造***/
    /**
     * 根据银行ID查下级银行IDs
     * 与getSubBanksById相同
     * bankService.getSubBanksById替换
     */
    List<Bank> selectSubsById(String bankId);

    /**
     * 查询所有银行ID
     * 先查本地库-后期切到流程表单上
     * 替换为bankServiceImpl.doGetAllBankIds
     */
    List<String> getAllBankIds();

}
