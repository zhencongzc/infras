package com.cmbc.infras.health.mapper;

import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface StatisticsMapper {

    @Select("select bank_name bankName, report from report " +
            "where model_id=#{modelId} and create_time between #{dateStart} and #{dateEnd} group by bank_name")
    List<JSONObject> findReportByDate(String modelId, String dateStart, String dateEnd);

    @Select("select count(*) from bank where model_id=#{modelId}")
    int findBankCount(String modelId);

    @Select("select bank_name bankName, report from report " +
            "where model_id=#{modelId} and bank_name=#{name} and create_time between #{dateStart} and #{dateEnd} limit 1")
    JSONObject findReportByBank(String modelId, String name, String dateStart, String dateEnd);

    @Select("select bank_name bankName, score from bank_score " +
            "where model_id=#{modelId} and dimension_id=#{id} and year=#{year} order by score desc")
    List<JSONObject> findScoreByDimensionId(String modelId, int id, String year);

    @Select("select bs.bank_name bankName, d.id, d.name, bs.score from bank_score bs inner join dimension d on bs.dimension_id=d.id " +
            "where bs.model_id=#{modelId} and bs.level=0 and bs.year=#{year} and bs.bank_name=#{name}")
    List<JSONObject> findDimensionScoreByBank(String modelId, int year, String name);

}
