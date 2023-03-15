package com.cmbc.infras.health.mapper;

import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.StatementType;

import java.util.List;

@Mapper
public interface ModelMapper {

    @Select("select count(id) from model where model_id like concat('%',#{word},'%') or title like concat('%',#{word},'%') " +
            "or description like concat('%',#{word},'%') or create_person like concat('%',#{word},'%')")
    Integer getModelTotal(String word);

    @Select("select id, model_id modelId, title, description, create_person createPerson, create_time createTime, start_model startModel," +
            "start_score startScore from model where model_id like concat('%',#{word},'%') or title like concat('%',#{word},'%') " +
            "or description like concat('%',#{word},'%') or create_person like concat('%',#{word},'%') order by create_time desc limit #{start},#{end}")
    List<JSONObject> quickFind(String word, int start, int end);

    @Update("update model set start_model=#{startModel} where model_id=#{modelId}")
    void startModel(String modelId, int startModel);

    @Update("update model set start_score=#{startScore} where model_id=#{modelId}")
    void startScore(String modelId, int startScore);

    @Select("select name, description from type")
    List<JSONObject> findType();

    @Insert("insert into model values (null, #{modelId}, #{cycleValue}, #{cycleUnit}, #{title}, #{iconName}, #{borderColor}, #{iconColor}, #{backgroundColor}, " +
            "#{cycleStart}, #{cycleEnd}, #{description}, #{gradation}, 0, 0, #{createPerson}, #{createTime})")
    void addModel(String modelId, int cycleValue, String cycleUnit, String title, String iconName, String borderColor,
                  String iconColor, String backgroundColor, String cycleStart, String cycleEnd, String description, String gradation,
                  String createPerson, String createTime);

    @Insert("<script>" +
            "insert into dimension values(null, #{modelId}, #{rootId}, #{parentId}, #{a.name}, #{a.location}, #{a.level}, #{a.ifLeaf}, " +
            "#{a.type}, #{a.score}, <choose><when test='a.ifAudit == null'>0</when><otherwise>#{a.ifAudit}</otherwise></choose>)" +
            "</script>")
    @SelectKey(statement = "select last_insert_id()", keyProperty = "a.id", before = false, statementType = StatementType.STATEMENT,
            resultType = Integer.class)
    int addDimension(String modelId, int rootId, int parentId, @Param("a") JSONObject dimension);

    @Insert("<script> insert into type_single values" +
            "<foreach item='item' collection='optionList' separator=','>" +
            "(null, #{bankName}, #{item.dimensionId}, #{item.name}, #{item.sort}, #{item.score}, #{item.ifDocument})" +
            "</foreach></script>")
    void addOption(String bankName, List<JSONObject> optionList);

    @Insert("<script> insert into type_deduct_dimension values" +
            "<foreach item='item' collection='deductDimensionList' separator=','>" +
            "(null, #{bankName}, #{item.dimensionId}, #{item.source}, #{item.dateStart}, #{item.dateEnd}, #{item.chooseState}, #{item.chooseCondition}, " +
            "#{item.statisticsRule}, #{item.calculationRule}, 1, #{item.rule})" +
            "</foreach></script>")
    void addDeductDimension(String bankName, List<JSONObject> deductDimensionList);

    @Insert("<script> insert into type_analysis values" +
            "<foreach item='item' collection='analysisList' separator=','>" +
            "(null, #{bankName}, #{item.dimensionId}, #{item.cycle}, #{item.alarmType}, #{item.eventLevel}, #{item.eventName}, #{item.timeLimit}, " +
            "#{item.deduct}, #{item.rule})" +
            "</foreach></script>")
    void addAnalysis(String bankName, List<JSONObject> analysisList);

    @Insert("<script> insert into type_monitor values" +
            "<foreach item='item' collection='monitorList' separator=','>" +
            "(null, #{item.bankName}, #{item.bankId}, #{item.dimensionId}, #{item.spotType}, #{item.resources}, #{item.rule})" +
            "</foreach></script>")
    void addMonitor(List<JSONObject> monitorList);

    @Insert("<script> insert into bank values" +
            "<foreach item='item' collection='organization' separator=','>" +
            "(null, #{modelId}, #{item.id}, #{item.name}, #{data})" +
            "</foreach></script>")
    void addBank(String modelId, List<JSONObject> organization, String data);

    @Insert("<script> insert into role_commit values" +
            "<foreach item='item' collection='listRole' separator=','>" +
            "(null, #{modelId}, #{item.id}, #{item.name}, #{data})" +
            "</foreach></script>")
    void addCommitRole(String modelId, List<JSONObject> listRole, String data);

    @Insert("<script> insert into role_audit values" +
            "<foreach item='item' collection='listRole' separator=','>" +
            "(null, #{modelId}, #{item.id}, #{item.name}, #{data})" +
            "</foreach></script>")
    void addAuditRole(String modelId, List<JSONObject> listRole, String data);

    @Select("select model_id modelId, cycle_value cycleValue, cycle_unit cycleUnit, title, icon_name iconName, border_color borderColor, " +
            "icon_color iconColor, background_color backgroundColor, cycle_start cycleStart, cycle_end cycleEnd, description, gradation, " +
            "start_score startScore from model where model_id=#{modelId}")
    JSONObject findModel(String modelId);

    @Select("select model_id modelId, cycle_value cycleValue, cycle_unit cycleUnit, title, icon_name iconName, border_color borderColor, " +
            "icon_color iconColor, cycle_start cycleStart, cycle_end cycleEnd, description, start_score startScore from model where start_model=1")
    List<JSONObject> findActiveModel();

    @Select("select bank_id bankId, name from bank where model_id=#{modelId}")
    List<JSONObject> findBank(String modelId);

    @Select("select data from bank where model_id=#{modelId} limit 1")
    String findBankData(String modelId);

    @Select("select data from role_commit where model_id=#{modelId} limit 1")
    String findCommitRole(String modelId);

    @Select("select data from role_audit where model_id=#{modelId} limit 1")
    String findAuditRole(String modelId);

    @Select("select id, model_id modelId, parent_id parentId, name, location, level, if_leaf ifLeaf, type, score, if_audit ifAudit " +
            "from dimension where model_id=#{modelId}")
    List<JSONObject> findDimension(String modelId);

    @Select("select name, sort, score, if_document ifDocument from type_single where dimension_id=#{dimensionId} group by sort")
    List<JSONObject> findOption(int dimensionId);

    @Select("select source, date_start dateStart, date_end dateEnd, choose_state chooseState, choose_condition chooseCondition, " +
            "statistics_rule statisticsRule, calculation_rule calculationRule, standard_value standardValue, rule " +
            "from type_deduct_dimension where dimension_id=#{dimensionId} limit 1")
    JSONObject findDeductDimension(int dimensionId);

    @Select("select source, date_start dateStart, date_end dateEnd, choose_state chooseState, choose_condition chooseCondition, " +
            "statistics_rule statisticsRule, calculation_rule calculationRule, standard_value standardValue, rule " +
            "from type_deduct_dimension where dimension_id=#{dimensionId} and bank_name=#{name}")
    JSONObject findDeductDimensionByName(int dimensionId, String name);

    @Select("select bank_name bankName, bank_id bankId, spot_type spotType, resources, rule from type_monitor where dimension_id=#{dimensionId}")
    List<JSONObject> findMonitor(int dimensionId);

    @Select("select bank_name bankName, bank_id bankId, spot_type spotType, resources, rule from type_monitor where dimension_id=#{dimensionId} " +
            "and bank_name=#{name}")
    JSONObject findMonitorByIdAndBankName(int dimensionId, String name);

    @Select("select bank_name bankName, cycle, alarm_type alarmType, event_level eventLevel, event_name eventName, time_limit timeLimit, deduct, rule " +
            "from type_analysis where dimension_id=#{dimensionId} limit 1")
    JSONObject findAnalysis(int id);

    @Delete("delete from model where model_id=#{modelId}")
    void deleteModel(String modelId);

    @Delete("delete from bank where model_id=#{modelId}")
    void deleteBank(String modelId);

    @Delete("delete from role_commit where model_id=#{modelId}")
    void deleteCommitRole(String modelId);

    @Delete("delete from role_audit where model_id=#{modelId}")
    void deleteAuditRole(String modelId);

    @Delete("delete from bank_score where model_id=#{modelId}")
    void deleteScore(String modelId);

    @Select("select id, model_id modelId, root_id rootId, parent_id parentId, name, location, level, if_leaf ifLeaf, type, score, if_audit ifAudit " +
            "from dimension where model_id=#{modelId} and if_leaf=1")
    List<JSONObject> findLeaf(String modelId);

    @Select("select id from dimension where model_id=#{modelId} and if_leaf=1")
    List<Integer> findLeafId(String modelId);

    @Delete("<script> delete from type_single where dimension_id in" +
            "<foreach item='item' collection='leafId' open='(' close=')' separator=','>" +
            "#{item}" +
            "</foreach></script>")
    void deleteOption(List<Integer> leafId);

    @Delete("<script> delete from type_deduct where dimension_id in" +
            "<foreach item='item' collection='leafId' open='(' close=')' separator=','>" +
            "#{item}" +
            "</foreach></script>")
    void deleteDeduct(List<Integer> leafId);

    @Delete("<script> delete from type_deduct_dimension where dimension_id in" +
            "<foreach item='item' collection='leafId' open='(' close=')' separator=','>" +
            "#{item}" +
            "</foreach></script>")
    void deleteDeductDimension(List<Integer> leafId);

    @Delete("<script> delete from type_monitor where dimension_id in" +
            "<foreach item='item' collection='leafId' open='(' close=')' separator=','>" +
            "#{item}" +
            "</foreach></script>")
    void deleteMonitor(List<Integer> leafId);

    @Delete("<script> delete from type_analysis where dimension_id in" +
            "<foreach item='item' collection='leafId' open='(' close=')' separator=','>" +
            "#{item}" +
            "</foreach></script>")
    void deleteAnalysis(List<Integer> leafId);

    @Delete("delete from dimension where model_id=#{modelId}")
    void deleteDimension(String modelId);

    @Update("update model set cycle_value=#{cycleValue}, cycle_unit=#{cycleUnit}, title=#{title}, icon_name=#{iconName}, border_color=#{borderColor}, " +
            "icon_color=#{iconColor}, background_color=#{backgroundColor}, cycle_start=#{cycleStart}, cycle_end=#{cycleEnd}, description=#{description}, " +
            "gradation=#{gradation} " +
            "where model_id=#{modelId}")
    void updateModel(String modelId, String cycleValue, String cycleUnit, String title, String iconName, String borderColor, String iconColor,
                     String backgroundColor, String cycleStart, String cycleEnd, String description, String gradation);

    @Insert("<script> insert into bank_score values" +
            "<foreach item='item' collection='dimension' separator=','>" +
            "(null, #{modelId}, #{item.id}, #{item.level}, #{name}, null, 0, 0, <choose><when test=\"item.type == 'monitor'\">'自动'</when>" +
            "<when test=\"item.type == 'analysis'\">'自动'</when><otherwise>#{state}</otherwise></choose>, #{year}, null)" +
            "</foreach></script>")
    void addScore(String modelId, List<JSONObject> dimension, String name, String state, int year);

    @Select("<script> select dimension_id id, score result, audit_result auditResult from bank_score where bank_name=#{name} and year=#{year} " +
            "and dimension_id in " +
            "<foreach item='item' collection='leafId' open='(' close=')' separator=','>" +
            "#{item}" +
            "</foreach></script>")
    List<JSONObject> findAuditResult(String name, int year, List<Integer> leafId);

    @Insert("<script> insert into report values" +
            "<foreach item='item' collection='listReport' separator=','>" +
            "(null, #{item.modelId}, #{item.title}, #{item.description}, #{item.name}, #{item.report}, #{item.year}, #{version}, #{time})" +
            "</foreach></script>")
    void createReport(List<JSONObject> listReport, int version, String time);

    @Select("select max(version) version from report where model_id=#{modelId} for update")
    JSONObject findReportVersion(String modelId);

    @Select("select id, score from dimension where model_id=#{modelId} and if_leaf=1 and type='monitor'")
    List<JSONObject> findMonitorLeaf(String modelId);

    @Select("select id, score from dimension where model_id=#{modelId} and if_leaf=1 and type='analysis'")
    List<JSONObject> findAnalysisLeaf(String modelId);

    @Select("select score from dimension where id=#{dimensionId}")
    Double findDimensionScore(int dimensionId);

    @Select("<script> select bank_name bankName, dimension_id dimensionId, spot_type spotType, resources, rule from type_monitor where dimension_id in" +
            "<foreach item='item' collection='leafId' open='(' close=')' separator=','>" +
            "#{item.id}" +
            "</foreach></script>")
    List<JSONObject> findMonitorList(List<JSONObject> leafId);

    @Select("<script> select bank_name bankName, dimension_id dimensionId, cycle, alarm_type alarmType, event_level eventLevel, event_name eventName, " +
            "time_limit timeLimit, deduct, rule from type_analysis where dimension_id in" +
            "<foreach item='item' collection='leafId' open='(' close=')' separator=','>" +
            "#{item.id}" +
            "</foreach></script>")
    List<JSONObject> findAnalysisList(List<JSONObject> leafId);

    @Select("select model_id modelId, title from model")
    List<JSONObject> findAllModel();

    @Select("select id, model_id modelId, root_id rootId, parent_id parentId, name, location, level, if_leaf ifLeaf, type, score, if_audit ifAudit " +
            "from dimension where id=#{dimensionId}")
    JSONObject findDimensionById(int dimensionId);

    @Select("select id, model_id modelId, root_id rootId, parent_id parentId, name, location, level, if_leaf ifLeaf, type, score, if_audit ifAudit " +
            "from dimension where parent_id=#{parentId}")
    List<JSONObject> findDimensionByParentId(int parentId);

    @Delete("<script> delete from dimension where id in" +
            "<foreach item='item' collection='ids' open='(' close=')' separator=','>" +
            "#{item.id}" +
            "</foreach></script>")
    void deleteDimensionByList(List<JSONObject> ids);

    @Delete("delete from type_single where dimension_id=#{id}")
    void deleteOptionById(int id);

    @Delete("delete from type_deduct_dimension where dimension_id=#{id}")
    void deleteDeductDimensionById(int id);

    @Delete("delete from type_deduct where dimension_id=#{id}")
    void deleteDeductById(int id);

    @Delete("delete from type_monitor where dimension_id=#{id}")
    void deleteMonitorById(int id);

    @Delete("delete from type_analysis where dimension_id=#{id}")
    void deleteAnalysisById(int id);

    @Update("update dimension set name=#{a.name}, location=#{a.location}, if_leaf=#{a.ifLeaf}, type=#{a.type}, score=#{a.score} where id=#{a.id}")
    void updateDimesion(@Param("a") JSONObject dimension);

    @Delete("delete from bank_score where dimension_id=#{id}")
    void deleteScoreById(int id);

    @Select("select id, model_id modelId, root_id rootId, parent_id parentId, name, location, level, if_leaf ifLeaf, type, score, if_audit ifAudit " +
            "from dimension where root_id=#{rootId}")
    List<JSONObject> findDimensionByRootId(int rootId);

    @Delete("delete from bank_score where model_id=#{modelId} and year=#{year}")
    void deleteScoreByYear(String modelId, int year);

    @Delete("<script> delete from bank_score where model_id=#{modelId} and bank_name in" +
            "<foreach item='item' collection='banks' open='(' close=')' separator=','>" +
            "#{item.name}" +
            "</foreach></script>")
    void deleteScoreByBank(String modelId, List<JSONObject> banks);

    @Select("select id, model_id modelId, parent_id parentId, name, location, level, if_leaf ifLeaf, type, score, if_audit ifAudit " +
            "from dimension where model_id=#{modelId} and level=0")
    List<JSONObject> findRootDimension(String modelId);

    @Select("select root_id rootId, count(root_id) count " +
            "from dimension where model_id=#{modelId} and if_leaf=1 group by root_id;")
    List<JSONObject> findLeafCount(String modelId);

    @Update("<script><foreach item='item' collection='needUpdate' separator=';'>" +
            "update dimension set location=#{item.location} where id=#{item.id} " +
            "</foreach></script>")
    void updateDimesionLocation(List<JSONObject> needUpdate);



}
