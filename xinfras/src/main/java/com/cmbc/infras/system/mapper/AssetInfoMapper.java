package com.cmbc.infras.system.mapper;

import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.annotations.*;

import java.util.HashSet;
import java.util.List;

@Mapper
public interface AssetInfoMapper {

    @Select("select id, bbankOrgId, bbankOrgName, orgId, orgName, equipKindId, kindCode, equipKindName, equipTypeId, typeCode, " +
            "equipTypeName, equipBrandId, brandCode, equipBrandName, equipModelId, modelCode, equipModelName, equipName, status, serialNum, " +
            "barCode, rfid, responsorAId, responsorAName, responsorBId, responsorBName, machineroomId, machineroomName, department, price, depreciate, " +
            "localize, receivedDate, supplierId, supplierName, maintenanceStartDate, maintenanceEndDate, maintenanceCompId, maintenanceCompName, remark, " +
            "createTime, updateTime, createUserid, createUsername, typeIdAllPath, equipSeq, backOrInternet, floorNum, cpuModel, cpuCore, memory, disk, " +
            "unit, totalPrice, liableUserLoginName, userLoginName, receiveLoginName, contractNo, invoiceNo, invoiceDate, faCompanyNo, faDeptNo, apCode, " +
            "selfNo, signNo, oldAssetNo, oldAssetBarcode, accountNo, accountDate, assetSource, originalSerialNum, assetKind, sourceType, assetSpec, " +
            "scrapType, attachMark, mainScene, isImportant, conf1, conf2, conf3, conf4 ,resourceId, synchronizeOrNot, needSend, sendState, sendTime " +
            "from asset_info where bbankOrgName like concat('%',#{word},'%') and synchronizeOrNot=#{synchronize} limit #{start},#{end}")
    List<JSONObject> quickFind(String word, int start, int end, int synchronize);

    @Select("<script> select id, bbankOrgId, bbankOrgName, orgId, orgName, equipKindId, kindCode, equipKindName, equipTypeId, typeCode, " +
            "equipTypeName, equipBrandId, brandCode, equipBrandName, equipModelId, modelCode, equipModelName, equipName, status, serialNum, " +
            "barCode, rfid, responsorAId, responsorAName, responsorBId, responsorBName, machineroomId, machineroomName, department, price, depreciate, " +
            "localize, receivedDate, supplierId, supplierName, maintenanceStartDate, maintenanceEndDate, maintenanceCompId, maintenanceCompName, remark, " +
            "createTime, updateTime, createUserid, createUsername, typeIdAllPath, equipSeq, backOrInternet, floorNum, cpuModel, cpuCore, memory, disk, " +
            "unit, totalPrice, liableUserLoginName, userLoginName, receiveLoginName, contractNo, invoiceNo, invoiceDate, faCompanyNo, faDeptNo, apCode, " +
            "selfNo, signNo, oldAssetNo, oldAssetBarcode, accountNo, accountDate, assetSource, originalSerialNum, assetKind, sourceType, assetSpec, " +
            "scrapType, attachMark, mainScene, isImportant, conf1, conf2, conf3, conf4 ,resourceId " +
            "from asset_info " +
            "where resourceId is not null and resourceId !=\"\" and needSend=1 and id in" +
            "<foreach item='item' collection='list' open='(' close=')' separator=','>" +
            "#{item}" +
            "</foreach></script>")
    List<JSONObject> findAssetNeedSend(List<String> list);

    @Select("select count(id) from asset_info where bbankOrgName like concat('%',#{word},'%') and synchronizeOrNot=#{synchronize}")
    Integer getModelTotal(String word, int synchronize);

    @Select("select id, resourceId from asset_info where resourceId is not null and resourceId !=\"\"")
    List<JSONObject> findAssetResourceId();

    @Select("select id from asset_info where synchronizeOrNot=1")
    HashSet<String> findAssetIdNeedSynchronize();

    @Delete("delete from asset_info")
    void deleteAllAsset();

    @Insert("<script> insert into asset_info values" +
            "<foreach item='item' collection='data' separator=','>" +
            "(#{item.id}, #{item.bbankOrgId}, #{item.bbankOrgName}, #{item.orgId}, #{item.orgName}, #{item.equipKindId}, #{item.kindCode}, #{item.equipKindName}, " +
            "#{item.equipTypeId}, #{item.typeCode}, #{item.equipTypeName}, #{item.equipBrandId}, #{item.brandCode}, #{item.equipBrandName}, #{item.equipModelId}, " +
            "#{item.modelCode}, #{item.equipModelName}, #{item.equipName}, #{item.status}, #{item.serialNum}, #{item.barCode}, #{item.rfid}, " +
            "#{item.responsorAId}, #{item.responsorAName}, #{item.responsorBId}, #{item.responsorBName}, #{item.machineroomId}, #{item.machineroomName}, " +
            "#{item.department}, #{item.price}, #{item.depreciate}, #{item.localize}, #{item.receivedDate}, #{item.supplierId}, #{item.supplierName}, " +
            "#{item.maintenanceStartDate}, #{item.maintenanceEndDate}, #{item.maintenanceCompId}, #{item.maintenanceCompName}, #{item.remark}, " +
            "#{item.createTime}, #{item.updateTime}, #{item.createUserid}, #{item.createUsername}, #{item.typeIdAllPath}, #{item.equipSeq}, " +
            "#{item.backOrInternet}, #{item.floorNum}, #{item.cpuModel}, #{item.cpuCore}, #{item.memory}, #{item.disk}, #{item.unit}, #{item.totalPrice}, " +
            "#{item.liableUserLoginName}, #{item.userLoginName}, #{item.receiveLoginName}, #{item.contractNo}, #{item.invoiceNo}, #{item.invoiceDate}, " +
            "#{item.faCompanyNo}, #{item.faDeptNo}, #{item.apCode}, #{item.selfNo}, #{item.signNo}, #{item.oldAssetNo}, #{item.oldAssetBarcode}, " +
            "#{item.accountNo}, #{item.accountDate}, #{item.assetSource}, #{item.originalSerialNum}, #{item.assetKind}, #{item.sourceType}, #{item.assetSpec}, " +
            "#{item.scrapType}, #{item.attachMark}, #{item.mainScene}, #{item.isImportant}, #{item.conf1}, #{item.conf2}, #{item.conf3}, #{item.conf4}, " +
            "#{item.resourceId}, #{item.synchronizeOrNot}, 1, 0, null)" +
            "</foreach></script>")
    void synchronizeAsset(List<JSONObject> data);

    @Select("select oldKey, newKey from asset_key_map where sendOrNot=1")
    List<JSONObject> findAssetKeyMap();

    @Select("select columnName, syncValue, mapValue from asset_translate")
    List<JSONObject> findAssetTranslate();

    @Update("<script> update asset_info set synchronizeOrNot=#{i} where id in" +
            "<foreach item='item' collection='list' open='(' close=')' separator=','>" +
            "#{item}" +
            "</foreach></script>")
    void handleAutoSynchronize(List<String> list, int i);

    @Update("update asset_info set resourceId=#{resourceId} where id=#{id}")
    void saveResourceId(String id, String resourceId);

    @Select("select id, name, url, description from url where name like concat('%',#{word},'%')")
    List<JSONObject> interfaceList(String word);

    @Insert("insert into url values(null, #{name}, #{url}, #{description})")
    void addInterface(String name, String url, String description);

    @Update("update url set name=#{name}, url=#{url}, description=#{description} where id=#{id}")
    void saveInterface(int id, String name, String url, String description);

    @Delete("delete from url where id=#{id}")
    void deleteInterface(int id);

    @Select("<script>" +
            "select id, name, oldKey, newKey, sendOrNot from asset_key_map where <when test='sendOrNot != null'>sendOrNot=#{sendOrNot} and </when>" +
            "(name like concat('%',#{word},'%') or oldKey like concat('%',#{word},'%'))" +
            "</script>")
    List<JSONObject> dataList(String word, Integer sendOrNot);

    @Update("update asset_key_map set name=#{name}, newKey=#{newKey}, sendOrNot=#{sendOrNot} where id=#{id}")
    void saveMapping(int id, String name, String newKey, int sendOrNot);

    @Select("select id, name, columnName, syncValue, mapValue, description from asset_translate where columnName=#{oldKey}")
    List<JSONObject> mappingList(String oldKey);

    @Select("select id from asset_translate where columnName=#{columnName} and syncValue=#{syncValue}")
    List<JSONObject> checkMappingExist(String columnName, String syncValue);

    @Insert("insert into asset_translate values(null, #{name}, #{columnName}, #{syncValue}, #{mapValue}, #{description})")
    void addMapping(String name, String columnName, String syncValue, String mapValue, String description);

    @Update("update asset_translate set mapValue=#{mapValue}, description=#{description} where id=#{id}")
    void updateMapping(int id, String mapValue, String description);

    @Delete("delete from asset_translate where id=#{id}")
    void deleteMapping(int id);

    @Select("<script> select * from ass_asset_info where resource_id in" +
            "<foreach item='item' collection='list' open='(' close=')' separator=','>" +
            "#{item.resource_id}" +
            "</foreach></script>")
    List<JSONObject> findAssetNeedUpdate(List<JSONObject> list);

    @Delete("<script> delete from ass_asset_info where resource_id in" +
            "<foreach item='item' collection='list' open='(' close=')' separator=','>" +
            "#{item.resource_id}" +
            "</foreach></script>")
    void deleteAssetByList(List<JSONObject> list);

    @Insert("<script> insert into ass_asset_info values" +
            "<foreach item='item' collection='list' separator=','>" +
            "(#{item.resource_id}, #{item.device_num}, #{item.serial_num}, #{item.name}, #{item.ci_type}, " +
            "#{item.parent_id}, #{item.location}, #{item.location_cn}, #{item.path}, #{item.path_cn}, #{item.device_type}, #{item.device_type_cn}, " +
            "#{item.board_options}, #{item.board_id}, #{item.board_template}, #{item.open_space}, #{item.status}, #{item.active_status}, " +
            "#{item.device_info}, #{item.vendor_info}, #{item.vendor_info_cn}, #{item.description}, #{item.department}, #{item.owner}, " +
            "#{item.project}, #{item.create_date}, #{item.creater_name}, #{item.position_types}, #{item.umonit}, #{item.eic_num}, #{item.frontImg}, " +
            "#{item.backImg}, #{item.properties}, #{item.spots}, #{item.gate_status}, #{item.current_device}, #{item.current_location}, #{item.manage_type})" +
            "</foreach></script>")
    void insertAssetByList(List<JSONObject> list);

    @Insert("<script><foreach item='item' collection='list' separator=';'>" +
            "update asset_info set needSend=#{needSend}, sendState=#{sendState}<when test='sendTime != null'>, sendTime=#{sendTime}</when> " +
            "where id=#{item}" +
            "</foreach></script>")
    void updateStateByList(List<String> list, int needSend, int sendState, String sendTime);

}
