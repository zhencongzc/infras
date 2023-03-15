package com.cmbc.infras.system.service.impl;

import com.alibaba.fastjson.JSON;
import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.Device;
import com.cmbc.infras.dto.event.SubBankGroup;
import com.cmbc.infras.dto.monitor.Spot;
import com.cmbc.infras.system.mapper.BankMapper;
import com.cmbc.infras.system.mapper.DeviceSpotMapper;
import com.cmbc.infras.system.service.BankService;
import com.cmbc.infras.system.util.BankGroupUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service
public class BankServiceImpl implements BankService {

    @Resource
    private BankService bankService;

    @Resource
    private DeviceSpotMapper deviceSpotMapper;
    @Resource
    private BankMapper bankMapper;

    /**
     * 银行List->银行ID list
     */
    @Override
    public List<String> getBankIds(List<Bank> list) {
        List<String> ids = new ArrayList<>();
        for (Bank bank : list) {
            ids.add(bank.getBankId());
        }
        return ids;
    }

    /**
     * 查询当前银行下的机房设备
     */
    @Override
    public List<Device> getBankDevices(List<String> bankIds) {
        Map<String, Object> param = new HashMap<>();
        param.put("bankIds", bankIds);
        List<Device> devices = deviceSpotMapper.getBanksDevices(param);
        return devices;
    }

    /**
     * 查询银行list下所有测点resourceId集合
     * BankIds->Spots
     * 根据银行ID列表->查询银行s下所有设备
     * 根据所有设备->查询所有测点Ids
     */
    @Override
    public List<String> getBanksSpots(List<Bank> banks) {
        if (banks == null || banks.isEmpty()) {
            System.out.println("BankServiceImpl.getBanksSpots param banks is empty");
            return new ArrayList<>();
        }
        List<String> spotIds = new ArrayList<>();
        List<String> bankIds = getBankIds(banks);

        List<Device> devices = getBankDevices(bankIds);
        if (devices.isEmpty()) {
            log.error("银行没有配置设备 bankIds:{}", JSON.toJSONString(bankIds));
            return spotIds;
        }
        List<String> deviceIds = new ArrayList<>();
        for (Device dev : devices) {
            deviceIds.add(dev.getDeviceId());
        }
        List<Spot> spots = deviceSpotMapper.getDevicesSpots(deviceIds);
        if (spots.isEmpty()) {
            log.error("设备没有配置测点 devices:{}", JSON.toJSONString(deviceIds));
            return spotIds;
        }
        for (Spot spot : spots) {
            spotIds.add(spot.getSpotId());
        }
        return spotIds;
    }

    @Override
    public List<Bank> getSubBanks(String bankId) {
        List<Bank> banks = bankMapper.getSubBanksById(bankId);
        List<Bank> subs = new ArrayList<>();
        for (Bank bank : banks) {
            List<Bank> sons = getSubBanks(bank.getBankId());
            subs.addAll(sons);
        }
        banks.addAll(subs);
        return banks;
    }

    /**
     * 分层批量递归查询下级银行s
     */
    @Override
    public List<Bank> getSubBanksBatch(List<String> bankIds) {
        List<Bank> banks = bankMapper.getSubBanks(bankIds);
        if (banks.isEmpty()) {
            return banks;
        }
        List<String> ids = this.getIds(banks);
        List<Bank> bks = getSubBanksBatch(ids);
        banks.addAll(bks);
        return banks;
    }

    @Override
    public List<String> getSubBankIds(List<String> bankIds) {
        List<String> ids = bankMapper.getSubBankIds(bankIds);
        if (ids.isEmpty()) {
            return ids;
        }
        List<String> sids = getSubBankIds(ids);
        ids.addAll(sids);
        return ids;
    }

    @Override
    public List<Bank> getSubBanks(List<String> bankIds) {
        List<Bank> banks = bankMapper.getSubBanks(bankIds);
        List<String> ids = new ArrayList<>();
        for (Bank bank : banks) {
            ids.add(bank.getBankId());
        }
        if (ids.size() > 0) {
            List<Bank> subBanks = getSubBanks(ids);
            banks.addAll(subBanks);
        }
        return banks;
    }

    private List<String> getIds(List<Bank> banks) {
        List<String> ids = new ArrayList<>();
        for (Bank bank : banks) {
            ids.add(bank.getBankId());
        }
        return ids;
    }

    /**
     * 告警统计改造-部分********************************************
     * 统计数据逻辑变更-不在查解行/设备/测点
     * 查告警只根据银行ID,查银行ID下所有告警
     */

    /**
     * 40家分行统计用
     * [本行机房,二级分行,支行,村镇]银行IDs
     * 12.1改版
     * 生产配置-二级分行支行村镇银行都挂在一级分行下
     * 先查一级分行的子集,再通过level分出--二级s,支行s,村镇s
     */
    @Override
    public SubBankGroup getSubBanksGroup(String bankId) {
        SubBankGroup group = new SubBankGroup();
        Bank bank = bankService.getBankById(bankId);
        group.setBankId(bankId);
        group.setBankName(bank.getBankName());
        List<Bank> allChilds = bankMapper.selectSubsById(bankId);
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
     * 改版-通过流程表单查询银行相关信息
     * 有问题切回数据库，下边同名方法-newEA
     */
    @Override
    public Bank getBankById(String bankId) {
        return bankMapper.getBankById(bankId);
    }

}
