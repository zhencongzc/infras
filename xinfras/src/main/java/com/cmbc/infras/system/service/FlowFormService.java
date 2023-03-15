package com.cmbc.infras.system.service;

import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.event.SubBankGroup;

import java.util.HashSet;
import java.util.List;

public interface FlowFormService {

    SubBankGroup getSubBanksGroup(String bankId);

    String getCacheSubBankIdstr(String bankId, String sessionId);

    List<String> getCacheSubBankIds(String bankId, String sessionId);

    List<Bank> getCacheSubBanks(String bankId, String sessionId);

    List<Bank> getSubBanks(String bankId, String sessionId);

    String getUserBankId(String account);

    Bank getBankById(String bankId, String sessionId);

    HashSet<String> getAllBankIds(String sessionId);

    List<Bank> getSubBanksById(String bankId, String sessionId);

    List<Bank> getBanksByLevel(int level, String sessionId);
}
