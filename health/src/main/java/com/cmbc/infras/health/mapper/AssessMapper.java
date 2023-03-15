package com.cmbc.infras.health.mapper;

import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AssessMapper {

    @Select("select model_id, icon_name, icon_color, border_color, background_color, title, description from model where start_model=1 and" +
            "(title like concat('%',#{word},'%') or description like concat('%',#{word},'%'))")
    List<JSONObject> adminQuickFind(String word);

    @Select("select m.model_id, m.icon_name, m.icon_color, m.border_color, m.background_color, m.title, m.description " +
            "from bank b inner join model m on b.model_id=m.model_id " +
            "where b.name=#{name} and m.start_model=1 and (m.title like concat('%',#{word},'%') or m.description like concat('%',#{word},'%'))")
    List<JSONObject> quickFind(String name, String word);

    @Select("select bank_name bankName, sum(score) score from bank_score where model_id=#{modelId} and level=0 and year=#{year} " +
            "group by bank_name order by score desc;")
    List<JSONObject> scoreRank(String modelId, int year);

    @Select("select sum(score) from dimension where model_id=#{modelId} and level=0")
    Double findModelTotalScore(String modelId);

    @Select("select cycle_start cycleStart, cycle_end cycleEnd from model where model_id=#{modelId}")
    JSONObject findCycleDate(String modelId);

    @Select("select d.id, d.name, d.type, d.score, bs.score result, bs.state from bank_score bs inner join dimension d on bs.dimension_id=d.id " +
            "where bs.model_id=#{modelId} and bs.bank_name=#{name} and bs.year=#{year} and bs.level=0")
    List<JSONObject> findScore(String modelId, String name, int year);

    @Select("select id, name, location, if_leaf ifLeaf, score, if_audit ifAudit from dimension where model_id=#{modelId} and root_id=#{id} " +
            "and if_leaf=1")
    List<JSONObject> findLeaf(String modelId, String id);

    @Select("select id, name, sort, score, if_document ifDocument from type_single " +
            "where bank_name=#{name} and dimension_id=#{id}")
    List<JSONObject> findOption(String name, int id);

    @Select("select id, bill_id billId, name, location, create_time createTime, state from type_deduct " +
            "where bank_name=#{name} and dimension_id=#{id} and year=#{year}")
    List<JSONObject> findDeduct(String name, int id, int year);

    @Select("select id, bill_id billId, name, location, create_time createTime, state from type_deduct " +
            "where bank_name=#{name} and dimension_id=#{id} and year=#{year}")
    List<JSONObject> findDeductByDate(String name, int id, int year);

    @Insert("insert into type_single_document values (null, #{id}, #{fileName}, #{url})")
    void insertUrl(int id, String url, String fileName);

    @Select("select sort, score, audit_result auditResult, state, advice from bank_score where dimension_id=#{id} and bank_name=#{name} and year=#{year}")
    JSONObject findResult(int id, String name, int year);

    @Update("<script><foreach item='item' collection='list' separator=';'>" +
            "update bank_score set sort=#{item.sort}, score=#{item.result}, audit_result=#{item.auditResult}, state=#{state} where dimension_id=#{item.id} " +
            "and bank_name=#{name} and year=#{year}" +
            "</foreach></script>")
    void commitSingle(int year, String name, List<JSONObject> list, String state);

    @Delete("delete from type_single_document where id=#{id}")
    void deleteDocument(int id);

    @Select("<script>select id, root_id rootId from dimension where id in" +
            "<foreach item='item' collection='list' open='(' close=')' separator=','>" +
            "#{item.id}" +
            "</foreach></script>")
    List<JSONObject> findDimensionByList(List<JSONObject> list);

    @Update("<script><foreach item='item' collection='listResult' separator=';'>" +
            "update bank_score set score=#{item.score}, state=#{state} where dimension_id=#{item.id} and bank_name=#{item.name} and year=#{item.year}" +
            "</foreach></script>")
    void updateResultByList(List<JSONObject> listResult, String state);

    @Update("update bank_score set score=#{score}, state=#{state} where dimension_id=#{id} and bank_name=#{name} and year=#{year}")
    void updateResult(String id, String name, int year, double score, String state);

    @Select("select id, name, if_document ifDocument from type_single where bank_name=#{name} and dimension_id=#{id} and sort in " +
            "(select sort from bank_score where bank_name=#{name} and year=#{year} and dimension_id=#{id})")
    JSONObject findDescription(int id, String name, int year);

    @Update("<script><foreach item='item' collection='list' separator=';'>" +
            "update bank_score set <choose><when test='item.auditResult == 0'>score=0,</when><otherwise>score=#{item.result},</otherwise></choose> " +
            "audit_result=#{item.auditResult}, state='已审核', advice=#{item.advice} " +
            "where dimension_id=#{item.id} and bank_name=#{name} and year=#{year}" +
            "</foreach></script>")
    void commitAudit(String name, int year, List<JSONObject> list);

    @Select("<script>select id, model_id modelId, title, description, bank_name name, version, create_time createTime " +
            "from report where bank_name=#{name} and (title like concat('%',#{word},'%') or " +
            "description like concat('%',#{word},'%')) order by create_time desc limit #{start},#{pageSize}" +
            "</script>")
    List<JSONObject> historyQuickFind(String name, String word, int start, int pageSize);

    @Select("select model_id modelId from model")
    List<String> findAllModelId();

    @Select("<script>select t.* from(" +
            "<foreach item='item' collection='allModelId' separator='union all'>" +
            "select id, model_id modelId, title, description, version, create_time createTime from report where model_id=#{item} " +
            "and (title like concat('%',#{word},'%') or description like concat('%',#{word},'%')) group by version " +
            "</foreach>" +
            ")t order by createTime desc limit #{start},#{pageSize}" +
            "</script>")
    List<JSONObject> adminHistoryQuickFind(List<String> allModelId, String word, int start, int pageSize);

    @Select("<script>select count(*) total from report where bank_name=#{name} and (title like concat('%',#{word},'%') or " +
            "description like concat('%',#{word},'%'))" +
            "</script>")
    List<JSONObject> getAssessTotal(String name, String word, int start, int pageSize);

    @Select("<script>select count(*) total from(" +
            "<foreach item='item' collection='allModelId' separator='union all'>" +
            "select id, model_id modelId, title, description, version, create_time createTime from report where model_id=#{item} " +
            "and (title like concat('%',#{word},'%') or description like concat('%',#{word},'%')) group by version " +
            "</foreach>" +
            ")t" +
            "</script>")
    List<JSONObject> adminGetAssessTotal(List<String> allModelId, String word, int start, int pageSize);

    @Select("select id, title, description, bank_name name, report, year, create_time createTime from report where id=#{id}")
    JSONObject assessResult(int id);

    @Select("select id from report where model_id=#{modelId} and version=#{version}")
    List<Integer> findReportByVersion(String modelId, Integer version);

    @Select("<script>select id, title, description, bank_name name, report, year, create_time createTime from report where id in" +
            "<foreach item='item' collection='listId' open='(' close=')' separator=','>" +
            "#{item}" +
            "</foreach></script>")
    List<JSONObject> allAssessResult(List<Integer> listId);

    @Select("select id from report where model_id=#{modelId} and bank_name=#{name} and year=#{year} and version=(select max(version) " +
            "from report where model_id=#{modelId})")
    List<JSONObject> findReportId(String modelId, String name, int year);

    @Select("select state from bank_score where model_id=#{modelId} and dimension_id=#{id} and bank_name=#{name} and year=#{year}")
    String findState(String modelId, String id, String name, int year);

    @Update("<script><foreach item='item' collection='needUpdate' separator=';'>" +
            "update type_deduct set name=#{item.name}, create_time=#{item.createTime}, state=#{item.state} where id=#{item.id} and year=#{item.year}" +
            "</foreach></script>")
    void updateSourceData(List<JSONObject> needUpdate);

    @Insert("<script> insert into type_deduct values" +
            "<foreach item='item' collection='needInsert' separator=','>" +
            "(null, #{item.bankName}, #{item.dimensionId}, #{item.billId}, #{item.name}, #{item.location}, #{item.createTime}, #{item.state}, #{item.year})" +
            "</foreach></script>")
    void addSourceData(List<JSONObject> needInsert);

    @Delete("delete from type_deduct where id=#{id}")
    void deleteSourceData(int id);

    @Select("select count(id) from type_deduct where bank_name=#{name} and dimension_id=#{id}")
    int findDeductCount(String name, int id);

    @Select("<script>select count(id) count from type_deduct where dimension_id=#{dimensionId} and bank_name=#{name} and state in" +
            "<foreach item='item' collection='condition' open='(' close=')' separator=','>" +
            "#{item.text}" +
            "</foreach></script>")
    JSONObject findConditionDeductCount(String name, int dimensionId, List<JSONObject> condition);

    @Update("<script><foreach item='item' collection='listResult' separator=';'>" +
            "update bank_score set score=#{item.score}, audit_result=#{item.auditResult}, state=#{state} " +
            "where dimension_id=#{item.id} and bank_name=#{item.name} and year=#{item.year}" +
            "</foreach></script>")
    void updateScore(List<JSONObject> listResult, String state);

    @Update("<script><foreach item='item' collection='listResult' separator=';'>" +
            "update bank_score set score=#{item.score}, audit_result=#{item.auditResult}, state=#{state} " +
            "where dimension_id=#{item.id} and bank_name=#{item.name} and year=#{item.year}" +
            "</foreach></script>")
    void updateAutomaticScore(List<JSONObject> listResult, String state);

    @Select("<script> select dimension_id dimensionId, score from bank_score where bank_name=#{name} and year=#{year} and dimension_id in" +
            "<foreach item='item' collection='list' open='(' close=')' separator=','>" +
            "#{item.id}" +
            "</foreach></script>")
    List<JSONObject> findIdAndScore(String name, int year, List<JSONObject> list);

    @Select("select id, file_name fileName, document from type_single_document where option_id=#{optionId}")
    List<JSONObject> findDocumentById(int optionId);

    @Select("<script>select bank_name bankName, report from report where model_id=#{modelId} and bank_name in " +
            "<foreach item='item' collection='organization' open='(' close=')' separator=','>" +
            "#{item.name}" +
            "</foreach>" +
            "and create_time between #{start} and #{end} group by bank_name" +
            "</script>")
    List<JSONObject> findReportByDate(String modelId, String start, String end, List<JSONObject> organization);

    @Select("select report from report where model_id=#{modelId} and bank_name=#{name} and year=#{year}")
    List<JSONObject> findReportByYear(String modelId, String name, int year);

    @Select("<script>select id, dimension_id dimensionId, audit_result auditResult, state from bank_score where model_id=#{modelId} and bank_name=#{name} " +
            "and year=#{year} and dimension_id in " +
            "<foreach item='item' collection='leafs' open='(' close=')' separator=','>" +
            "#{item.id}" +
            "</foreach></script>")
    List<JSONObject> findLeafScore(String modelId, String name, List<JSONObject> leafs, int year);

    @Select("select id, type, handler, detail, create_time createTime from record where model_id=#{modelId} and dimension_id=#{id} and " +
            "bank_name=#{name} and year=#{year} order by create_time desc")
    List<JSONObject> findRecord(String modelId, String name, int id, int year);

    @Insert("insert into record values(null, #{modelId}, #{dimensionId}, #{bankName}, #{type}, #{handler}, #{detail}, #{year}, now())")
    void makeRecord(String modelId, int dimensionId, String bankName, String type, String handler, String detail, int year);

    @Select("select name from dimension where id=#{id}")
    String findDimensionName(int id);

    @Select("select name from type_single where dimension_id=#{id} and sort=#{sort} limit 1")
    String findSingleSortName(int id, int sort);

    @Select("select name from type_deduct where bank_name=#{bankName} and dimension_id=#{id}")
    List<JSONObject> findDeductList(String bankName, int id);

    @Delete("delete from type_deduct where bank_name=#{name} and dimension_id=#{id} and year=#{year}")
    void deleteFormDataByNameAndDimension(String name, int id, int year);

    @Update("<script><foreach item='item' collection='list' separator=';'>" +
            "update type_deduct_dimension set standard_value=#{item.standardValue} " +
            "where dimension_id=#{item.id} and bank_name=#{name}" +
            "</foreach></script>")
    void updateStandardValue(String name, List<JSONObject> list);

    @Select("<script>select sum(score) from bank_score where bank_name=#{name} and year=#{year} and dimension_id in " +
            "<foreach item='item' collection='list' open='(' close=')' separator=','>" +
            "#{item.id}" +
            "</foreach></script>")
    Double selectSumByList(String name, int year, List<JSONObject> list);
}
