package com.cmbc.infras.system.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.event.SubBankGroup;
import com.cmbc.infras.redis.DataRedisUtil;
import com.cmbc.infras.system.mapper.BankMapper;
import com.cmbc.infras.system.mapper.DataConfigMapper;
import com.cmbc.infras.system.service.FlowFormService;
import com.cmbc.infras.system.util.BankGroupUtil;
import com.cmbc.infras.util.AccountBankUtil;
import com.cmbc.infras.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service
public class FlowFormServiceImpl implements FlowFormService {

    @Resource
    private BankMapper bankMapper;
    @Resource
    private DataConfigMapper dataConfigMapper;

    Logger LOG = LoggerFactory.getLogger("ExecuteAspect");

    @Override
    public SubBankGroup getSubBanksGroup(String bankId) {
        String sessionId = UserContext.getAuthToken();
        SubBankGroup group = new SubBankGroup();
        //封装一级分行
        Bank bank = getBankById(bankId, sessionId);
        group.setBankId(bankId);
        group.setBankName(bank.getBankName());
        log.info("BankServiceImpl.getSubBanksGroup bankId:" + bankId);
        //封装下级分行
        List<Bank> allChilds = getCacheSubBanks(bankId, sessionId);
        Map<String, List<Bank>> levelBanksMap = BankGroupUtil.groupBanks(allChilds);
        List<Bank> level2 = levelBanksMap.get("level2");
        List<Bank> level3 = levelBanksMap.get("level3");
        List<Bank> level4 = levelBanksMap.get("level4");

        group.setBranchBanks(level2);
        group.setSubBanks(level3);
        group.setTownBanks(level4);

        return group;
    }

    /**
     * 缓存取昝Banks Id的list的字符串
     */
    @Override
    public String getCacheSubBankIdstr(String bankId, String sessionId) {

        String redisKey = InfrasConstant.INFRAS_SUB_BANK_IDS_STR_ID + bankId;
        String idsStr = DataRedisUtil.getStringFromRedis(redisKey);
        if (StringUtils.isBlank(idsStr)) {
            StringBuffer sb = new StringBuffer();
            List<Bank> banks = getSubBanks(bankId, sessionId);
            if (banks != null && banks.size() > 0) {
                for (Bank bank : banks) {
                    sb.append(bank.getBankId()).append(",");
                }
                if (sb.length() > 0) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                DataRedisUtil.addStringToRedis(redisKey, sb.toString(), InfrasConstant.TIME_OUT);
            }
            return sb.toString();
        } else {
            return idsStr;
        }
    }

    /**
     * 缓存取下级银行id的list
     */
    @Override
    public List<String> getCacheSubBankIds(String bankId, String sessionId) {
        String redisKey = InfrasConstant.INFRAS_SUB_BANK_IDS_ID + bankId;
        String idsStr = DataRedisUtil.getStringFromRedis(redisKey);
        if (StringUtils.isBlank(idsStr)) {
            List<String> ids = new ArrayList<>();
            List<Bank> banks = getSubBanks(bankId, sessionId);
            if (banks != null && banks.size() > 0) {
                for (Bank bank : banks) {
                    ids.add(bank.getBankId());
                }
                DataRedisUtil.addStringToRedis(redisKey, JSON.toJSONString(ids), InfrasConstant.TIME_OUT);
            }
            return ids;
        } else {
            List<String> ids = JSONArray.parseArray(idsStr, String.class);
            return ids;
        }
    }

    /**
     * 缓存取昝Bank的list
     */
    @Override
    public List<Bank> getCacheSubBanks(String bankId, String sessionId) {
        String redisKey = InfrasConstant.INFRAS_SUB_BANKS_ID + bankId;
        String banksStr = DataRedisUtil.getStringFromRedis(redisKey);
        if (StringUtils.isBlank(banksStr)) {
            List<Bank> banks = getSubBanks(bankId, sessionId);
            if (banks != null && banks.size() > 0) {
                DataRedisUtil.addStringToRedis(redisKey, JSON.toJSONString(banks), InfrasConstant.TIME_OUT);
            }
            return banks;
        } else {
            List<Bank> banks = JSONArray.parseArray(banksStr, Bank.class);
            return banks;
        }
    }

    @Override
    public List<Bank> getSubBanks(String bankId, String sessionId) {
        List<Bank> banks = getSubBanksById(bankId, sessionId);
        List<Bank> subs = new ArrayList<>();
        for (Bank bank : banks) {
            List<Bank> sons = getSubBanks(bank.getBankId(), sessionId);
            subs.addAll(sons);
        }
        banks.addAll(subs);
        return banks;
    }

    @Override
    public String getUserBankId(String account) {
        String sessionId = UserContext.getAuthToken();
        //从流程引擎获取银行名称
        String bankName = AccountBankUtil.getAccountBankName(account, sessionId);
        //从数据库查询银行id
        List<JSONObject> bank = dataConfigMapper.findBankByName(bankName);
        String bankId = bank.get(0).getString("bankId");
        return bankId;
    }

    @Override
    public Bank getBankById(String bankId, String sessionId) {
        Assert.hasLength(bankId, "通过ID查询银行信息,bankId为空!");
        Assert.hasLength(sessionId, "param sessionId isBlank");
        String redisKey = InfrasConstant.INFRAS_BANK_INFO + bankId;
        Bank bank = DataRedisUtil.getStringFromRedis(redisKey, Bank.class);
        if (bank != null) return bank;
        //如没有缓存，查询银行并缓存
        List<JSONObject> list = dataConfigMapper.findBankByBankId(bankId);
        if (list.size() > 0) {
            JSONObject j = list.get(0);
            DataRedisUtil.addStringToRedis(redisKey, JSON.toJSONString(j), InfrasConstant.TIME_OUT);
            bank = j.toJavaObject(Bank.class);
            return bank;
        }
        return null;
    }

    @Override
    public HashSet<String> getAllBankIds(String sessionId) {
        HashSet<String> res = new HashSet<>();
        List<String> ids = new ArrayList<>();
        String redisKey = InfrasConstant.INFRAS_ALL_BANK_IDS;
        String idstr = DataRedisUtil.getStringFromRedis(redisKey);
        if (StringUtils.isNotBlank(idstr)) {
            ids = JSONArray.parseArray(idstr, String.class);
            ids.forEach(a -> res.add(a));
            return res;
        }
        //查询所有银行
        List<JSONObject> list = dataConfigMapper.findAllBank();
        for (JSONObject obj : list) {
            ids.add(obj.getString("bankId"));
            res.add(obj.getString("bankId"));
        }
        if (ids.size() > 0) {
            idstr = JSONObject.toJSONString(ids);
            DataRedisUtil.addStringToRedis(redisKey, idstr, InfrasConstant.TIME_OUT);
        }
        return res;
    }

    @Override
    public List<Bank> getSubBanksById(String bankId, String sessionId) {
        List<Bank> banks = bankMapper.getSubBanksById(bankId);
        return banks;
    }

    @Override
    public List<Bank> getBanksByLevel(int level, String sessionId) {
        List<Bank> banks;
        String redisKey = InfrasConstant.INFRAS_BANKS_LEVEL + level;
        String banksStr = DataRedisUtil.getStringFromRedis(redisKey);
        if (StringUtils.isNotBlank(banksStr)) {
            banks = JSONArray.parseArray(banksStr, Bank.class);
            return banks;
        }
        banks = bankMapper.getBanksByLevel(level);
        if (banks.size() > 0)
            DataRedisUtil.addStringToRedis(redisKey, JSON.toJSONString(banks), InfrasConstant.TIME_OUT);
        return banks;
    }

}
