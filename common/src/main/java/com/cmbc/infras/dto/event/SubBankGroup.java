package com.cmbc.infras.dto.event;

import com.cmbc.infras.dto.Bank;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 历史告警统计用
 * 40家分行的本行 一级分行 下级银行组List
 * [本行,二级分行,支行,村镇]List
 * 总行视角-各一级分行ID为参数
 */
public class SubBankGroup implements Serializable {
    private static final long serialVersionUID = 3890180546071407167L;

    //当前银行ID
    private String bankId;
    private String bankName;

    //下级二级分行
    private List<Bank> branchBanks;
    //下下级支行
    private List<Bank> subBanks;
    //下下下级村镇解行
    private List<Bank> townBanks;

    /**
     * 维护一份银行ID list
     */

    //下级二级分行ID list
    private List<String> branchs;
    //下下级支行ID list
    private List<String> subs;
    //下下下级村镇解行ID list
    private List<String> towns;



    public String getBankId() {
        return bankId;
    }

    public void setBankId(String bankId) {
        this.bankId = bankId;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public List<Bank> getBranchBanks() {
        return branchBanks;
    }

    public void setBranchBanks(List<Bank> branchBanks) {
        this.branchBanks = branchBanks;
        this.branchs = new ArrayList<>();
        for (Bank bank : branchBanks) {
            branchs.add(bank.getBankId());
        }
    }

    public List<Bank> getSubBanks() {
        return subBanks;
    }

    public void setSubBanks(List<Bank> subBanks) {
        this.subBanks = subBanks;
        this.subs = new ArrayList<>();
        for (Bank bank : subBanks) {
            subs.add(bank.getBankId());
        }
    }

    public List<Bank> getTownBanks() {
        return townBanks;
    }

    public void setTownBanks(List<Bank> townBanks) {
        this.townBanks = townBanks;
        this.towns = new ArrayList<>();
        for (Bank bank : townBanks) {
            towns.add(bank.getBankId());
        }
    }

    public List<String> getBranchs() {
        return branchs;
    }

    public void setBranchs(List<String> branchs) {
        this.branchs = branchs;
    }

    public List<String> getSubs() {
        return subs;
    }

    public void setSubs(List<String> subs) {
        this.subs = subs;
    }

    public List<String> getTowns() {
        return towns;
    }

    public void setTowns(List<String> towns) {
        this.towns = towns;
    }

    @Override
    public String toString() {
        return "SubBankGroup{" +
                "bankId='" + bankId + '\'' +
                ", bankName='" + bankName + '\'' +
                ", branchBanks=" + branchBanks +
                ", subBanks=" + subBanks +
                ", townBanks=" + townBanks +
                ", branchs=" + branchs +
                ", subs=" + subs +
                ", towns=" + towns +
                '}';
    }
}
