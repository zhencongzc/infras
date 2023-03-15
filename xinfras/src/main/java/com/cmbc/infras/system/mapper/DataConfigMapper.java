package com.cmbc.infras.system.mapper;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.Device;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DataConfigMapper {

    @Select("select id, bank_id bankId, bank_name bankName, parent_id parentId, parent_name parentName, contact_id contactId, contact, sort, level, " +
            "level_name levelName, lng, lat, area_id areaId, area_name areaName, remark " +
            "from tl_bank where bank_name like concat('%',#{word},'%') order by id desc limit #{start},#{end}")
    List<JSONObject> quickFind(String word, int start, int end);

    @Select("select count(id) from tl_bank where bank_name like concat('%',#{word},'%')")
    Integer getTotalByWord(String word);

    @Select("select id, bank_id bankId, bank_name bankName, parent_id parentId, parent_name parentName, contact_id contactId, contact, sort, level, " +
            "level_name levelName, lng, lat, area_id areaId, area_name areaName, remark " +
            "from tl_bank where bank_name like concat('%',#{bankName},'%') and parent_name like concat('%',#{parentName},'%') " +
            "and sort like concat('%',#{sort},'%') and level like concat('%',#{level},'%') and contact_id like concat('%',#{contactId},'%') " +
            "and contact like concat('%',#{contact},'%') and area_name like concat('%',#{areaName},'%') order by id desc limit #{start},#{end}")
    List<JSONObject> advancedQuery(String bankName, String parentName, String sort, String level, String contactId, String contact, String areaName,
                                   int start, int end);

    @Select("select count(id) from tl_bank where bank_name like concat('%',#{bankName},'%') and parent_name like concat('%',#{parentName},'%') " +
            "and sort like concat('%',#{sort},'%') and level like concat('%',#{level},'%') and contact_id like concat('%',#{contactId},'%') " +
            "and contact like concat('%',#{contact},'%') and area_name like concat('%',#{areaName},'%')")
    int getTotalByQuery(String bankName, String parentName, String sort, String level, String contactId, String contact, String areaName);

    @Insert("insert into fast_query values(null,#{formName},#{name},#{rule},#{account})")
    void createFastQuery(String formName, String account, String name, String rule);

    @Select("select id, form_name formName, name, rule, creator from fast_query where creator=#{account}")
    List<JSONObject> findFastQuery(String account);

    @Delete("delete from fast_query where id=#{id}")
    void deleteFastQuery(int id);

    @Update("update tl_bank set sort=sort+1 where parent_id=#{parentId} and sort>=#{sort}")
    void sortBank(String parentId, String sort);

    @Insert("insert into tl_bank values(null, #{bankId}, #{bankName}, #{parentId}, #{parentName}, #{contactId}, #{contact}, #{sort}, #{level}, #{levelName}," +
            " #{lng}, #{lat}, #{areaId}, #{areaName}, #{linkId}, #{remark})")
    void createBank(String bankId, String bankName, String parentId, String parentName, String contactId, String contact, String sort, String level,
                    String levelName, String lng, String lat, String areaId, String areaName, String linkId, String remark);

    @Select("select sort from tl_bank where parent_id=#{parentId} order by sort")
    List<Integer> getSortByParentId(String parentId);

    @Select("select id, bank_id bankId, bank_name bankName, parent_id parentId, parent_name parentName, contact_id contactId, contact, sort, level, " +
            "level_name levelName, lng, lat, area_id areaId, area_name areaName, link_id linkId, remark from tl_bank where id=#{id}")
    List<JSONObject> findBankById(String id);

    @Select("select id, bank_id bankId, bank_name bankName, parent_id parentId, parent_name parentName, contact_id contactId, contact, sort, level, " +
            "level_name levelName, lng, lat, area_id areaId, area_name areaName, remark from tl_bank where bank_id=#{bankId}")
    List<JSONObject> findBankByBankId(String bankId);

    @Select("select id, bank_id bankId, bank_name bankName, parent_id parentId, parent_name parentName, contact_id contactId, contact, sort, level, " +
            "level_name levelName, lng, lat, area_id areaId, area_name areaName, remark from tl_bank where bank_name=#{bankName}")
    List<JSONObject> findBankByName(String bankName);

    @Select("select id, bank_id bankId, bank_name bankName, parent_id parentId, parent_name parentName, contact_id contactId, contact, sort, level, " +
            "level_name levelName, lng, lat, area_id areaId, area_name areaName, remark from tl_bank")
    List<JSONObject> findAllBank();

    @Update("update tl_bank set bank_id=#{bankId}, bank_name=#{bankName}, parent_id=#{parentId}, parent_name=#{parentName}, contact_id=#{contactId}, " +
            "contact=#{contact}, sort=#{sort}, level=#{level}, level_name=#{levelName}, lng=#{lng}, lat=#{lat}, area_id=#{areaId}, area_name=#{areaName}, " +
            "link_id=#{linkId}, remark=#{remark} where id=#{id}")
    void updateBank(String id, String bankId, String bankName, String parentId, String parentName, String contactId, String contact, String sort,
                    String level, String levelName, String lng, String lat, String areaId, String areaName, String linkId, String remark);

    @Delete("<script> delete from tl_bank where id in" +
            "<foreach item='item' collection='list' open='(' close=')' separator=','>" +
            "#{item.id}" +
            "</foreach></script>")
    void deleteBank(List<JSONObject> list);

    @Select("select id, bank_id bankId, device_id deviceId, ke_name keName, device_name deviceName, device_type deviceType, group_name groupName, true_device_id trueDeviceId " +
            "from tl_bank_device where bank_id=#{bankId}")
    List<Device> findBankDevices(String bankId);

    @Select("select id, bank_id bankId, device_id deviceId, ke_name keName, device_name deviceName, device_type deviceType, group_name groupName, true_device_id trueDeviceId " +
            "from tl_bank_device where bank_id=#{bankId}")
    List<JSONObject> findDeviceByBankId(String bankId);

    @Select("select id, bank_id bankId, device_id deviceId, ke_name keName, device_name deviceName, device_type deviceType, group_name groupName, true_device_id trueDeviceId " +
            "from tl_bank_device where bank_id=#{bankId} and device_type=#{type}")
    List<JSONObject> findDeviceByBankIdAndType(String bankId, int type);

    @Select("select id, device_id deviceId, spot_id spotId, spot_name spotName, spot_type spotType from tl_device_spot where device_id=#{deviceId} " +
            "and spot_type=#{type}")
    List<JSONObject> findSpotByDeviceIdAndType(String deviceId, int type);

    @Delete("<script> delete from tl_device_spot where device_id in" +
            "<foreach item='item' collection='list' open='(' close=')' separator=','>" +
            "#{item.deviceId}" +
            "</foreach></script>")
    void deleteSpotByDeviceId(List<JSONObject> devices);

    @Delete("delete from tl_bank_device where bank_id=#{bankId}")
    void deleteDeviceByBankId(String bankId);

    @Insert("<script> insert into tl_bank_device values" +
            "<foreach item='item' collection='devices' separator=','>" +
            "(null, #{item.bankId}, #{item.deviceId}, #{item.keName}, #{item.deviceName}, #{item.deviceType}, #{item.groupName}, #{item.trueDeviceId})" +
            "</foreach></script>")
    void addDevices(List<JSONObject> devices);

    @Insert("<script> insert into tl_device_spot values" +
            "<foreach item='item' collection='spots' separator=','>" +
            "(null, #{item.deviceId}, #{item.spotId}, #{item.spotName}, #{item.spotType})" +
            "</foreach></script>")
    void addSpots(List<JSONObject> spots);

    @Select("select id, bank_id bankId, bank_name bankName, run_time runTime from tl_bank_run_time where bank_id=#{bankId}")
    JSONObject findRunTimeByBankId(String bankId);

    @Insert("insert into tl_bank_run_time values(null, #{bankId}, #{bankName}, now())")
    void insertSafeTime(String bankId, String bankName);

    @Update("update tl_bank_run_time set bank_name=#{bankName}, run_time=now() where bank_id=#{bankId}")
    void updateSafeTime(String bankId, String bankName);

    @Select("select sort from tl_bank where id=#{id}")
    int getSortById(int id);

    @Update("update tl_bank set sort=sort+1 where parent_id=#{parentId} and #{newSort}<=sort and sort<#{oldSort}")
    void sortBankByRangePlus(String parentId, int newSort, int oldSort);

    @Update("update tl_bank set sort=sort-1 where parent_id=#{parentId} and #{oldSort}<sort and sort<=#{newSort}")
    void sortBankByRangeMinus(String parentId, int newSort, int oldSort);

    @Select("select * from tl_user where name like concat('%',#{word},'%')")
    List<JSONObject> userList(String word);

    @Insert("<script> insert into tl_user values" +
            "<foreach item='item' collection='list' separator=','>" +
            "(#{item.id}, #{item.name}, #{item.account}, #{item.departments}, #{item.roles}, null, null, 0, null, 0, null)" +
            "</foreach></script>")
    void addUserByList(List<JSONObject> list);

    @Insert("<script> replace into tl_user values" +
            "<foreach item='item' collection='list' separator=','>" +
            "(#{item.id}, #{item.name}, #{item.account}, #{item.departments}, #{item.roles}, #{item.alarmMessage}, #{item.alarmEmail}, " +
            "#{item.startFormMessage}, #{item.formMessage}, #{item.startFormEmail}, #{item.formEmail})" +
            "</foreach></script>")
    void replaceUserByList(List<JSONObject> list);

    @Delete("delete from tl_user where id=#{id}")
    void deleteUser(int id);

    @Update("update tl_user set alarmMessage=#{alarmMessage}, alarmEmail=#{alarmEmail}, startFormMessage=#{startFormMessage}, formMessage=#{formMessage}, " +
            "startFormEmail=#{startFormEmail}, formEmail=#{formEmail} where id=#{id}")
    void updateUser(int id, String alarmMessage, String alarmEmail, int startFormMessage, String formMessage, int startFormEmail, String formEmail);

    @Select("select account from tl_user where locate('\"${word}\"',${column}) > 0")
    List<String> findUserNeedInform(String word, String column);

    @Select("select account from tl_user where locate('${word1}',${column1}) > 0 and locate('${word2}',${column2}) > 0")
    List<String> findUserNeedInformWithTwoCondition(String word1, String column1, String word2, String column2);

    @Select("select account from tl_user where locate('${word1}',department) > 0 and locate('${word2}',${column2}) > 0")
    List<String> findUserNeedInformDefault(String word1, String word2, String column2);
}
