package com.cmbc.infras.login.access.mapper;

import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UsualMapper {

    @Select("select id, bank_id bankId, bank_name bankName, parent_id parentId, parent_name parentName, contact_id contactId, contact, sort, level, " +
            "level_name levelName, lng, lat, area_id areaId, area_name areaName, remark from tl_bank where bank_name=#{bankName}")
    List<JSONObject> findBankByName(String bankName);

}
