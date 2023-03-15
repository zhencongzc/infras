package com.cmbc.infras.system.util;

import com.cmbc.infras.dto.Bank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 银行分组工具类
 */
public class BankGroupUtil {

    /**
     * 生产网银行层级配置
     * 一级分行下挂所有的二级分行s,支行s,村镇银行s
     * 统计需要区分 二级,支行,村镇 时,通过level分组
     */
    public static Map<String, List<Bank>> groupBanks(List<Bank> childs) {
        Map<String, List<Bank>> map = new HashMap<>();
        List<Bank> branchs = new ArrayList<>();
        List<Bank> subs = new ArrayList<>();
        List<Bank> towns = new ArrayList<>();
        map.put("level2", branchs);
        map.put("level3", subs);
        map.put("level4", towns);

        for (Bank bank : childs) {
            switch (bank.getLevel()) {
                case 2:
                    branchs.add(bank);
                    break;
                case 3:
                    subs.add(bank);
                    break;
                case 4:
                    towns.add(bank);
                    break;
                case 1:
                    System.out.println("BankGroupUtil.groupBanks level 1 bank id:" + bank.getBankId() + ",bankName:" + bank.getBankName());
                    break;
                default:
                    System.out.println("BankGroupUtil.groupBanks level:" + bank.getLevel() + ",bank id:" + bank.getBankId() + ",bankName:" + bank.getBankName());
            }
        }
        return map;
    }

}
