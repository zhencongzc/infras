package com.cmbc.infras.health.mapper;

import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommonMapper {

    @Select("select model_id modelId from model")
    List<JSONObject> findAllModel();

    @Select("select model_id modelId from role_commit where role_id=#{id}")
    List<JSONObject> findAvailableModelCommit(int id);

    @Select("select model_id modelId from role_audit where role_id=#{id}")
    List<JSONObject> findAvailableModelAudit(int id);

    @Select("select bs.bank_name bankName, d.id, d.name, bs.score from bank_score bs inner join dimension d on bs.dimension_id=d.id " +
            "where bs.model_id=#{modelId} and bs.level=0 and bs.year=#{year}")
    List<JSONObject> findDimensionScore(String modelId, int year);
}
