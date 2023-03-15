package com.cmbc.infras.system.controller;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.system.service.AssetInfoService;
import com.cmbc.infras.util.YmlConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;

/**
 * Description:资产信息管理系统
 * Author: zhencong
 * Date: 2022-11-28
 */
@Slf4j
@RestController
@RequestMapping("/asset")
public class AssetInfoController {

    @Resource
    private AssetInfoService assetInfoService;

    @Value("asset.getData.url")
    private String getDataUrl;

    private static final Logger log4j2 = LogManager.getLogger(AssetInfoController.class.getName());

    /**
     * 数据筛选列表/数据筛选查询/同步清单列表/同步清单查询
     * 查询条件：银行名称
     */
    @PostMapping("/quickFind")
    public BaseResult<List<JSONObject>> quickFind(@RequestBody JSONObject param) {
        int pageSize = param.getInteger("pageSize");
        int pageCount = param.getInteger("pageCount");
        int start = pageSize * (pageCount - 1);
        List<JSONObject> list = assetInfoService.quickFind(param.getString("word"), start, pageSize, param.getIntValue("synchronize"));
        int total = assetInfoService.getModelTotal(param.getString("word"), param.getIntValue("synchronize"));
        return new BaseResult<>(true, "", list, total, pageSize, pageCount);
    }

    /**
     * 同步资产数据
     */
    @PostMapping("/asset/synchronize")
    public BaseResult<String> synchronizeAsset() {
        List<JSONObject> data = new LinkedList<>();
        //从行方全量接口获取数据
        if (!"true".equals(YmlConfig.loginTest)) data = assetInfoService.getAllData(getDataUrl);
        //同步资产管理数据
        assetInfoService.synchronizeAsset(data);
        return BaseResult.success("");
    }

    /**
     * 更新资产管理数据-dcim的接口
     */
    @RequestMapping("/asset/update")
    public String updateAsset(@RequestBody JSONObject params) {
        try {
            assetInfoService.updateAsset(params.getJSONArray("list").toJavaList(JSONObject.class));
            return "success";
        } catch (Exception e) {
            log.error("出现异常:" + e.getMessage());
            return "出现异常:" + e.getMessage();
        }
    }

    /**
     * 添加自动同步
     */
    @PostMapping("/auto/synchronize/add")
    public BaseResult<String> addAutoSynchronize(@RequestBody JSONObject param) {
        assetInfoService.addAutoSynchronize(param.getJSONArray("list").toJavaList(String.class));
        return BaseResult.success("");
    }

    /**
     * 取消自动同步
     */
    @PostMapping("/auto/synchronize/cancel")
    public BaseResult<String> cancelAutoSynchronize(@RequestBody JSONObject param) {
        assetInfoService.cancelAutoSynchronize(param.getJSONArray("list").toJavaList(String.class));
        return BaseResult.success("");
    }

    /**
     * 设备编号确定
     */
    @PostMapping("/resourceId/save")
    public BaseResult<String> saveResourceId(@RequestBody JSONObject param) {
        assetInfoService.saveResourceId(param.getString("id"), param.getString("resourceId"));
        return BaseResult.success("");
    }

    /**
     * 发送资产数据
     */
    @PostMapping("/asset/send")
    public BaseResult<String> sendAsset(@RequestBody JSONObject param, HttpServletRequest request) {
        String res = assetInfoService.sendAsset(param.getJSONArray("list").toJavaList(String.class), request);
        if ("success".equals(res)) return BaseResult.success(null, res);
        return BaseResult.fail(res);
    }

    /**
     * 接口设置列表/接口设置查询
     * 查询条件：接口名称
     */
    @PostMapping("/interface/list")
    public BaseResult<List<JSONObject>> interfaceList(@RequestBody JSONObject param) {
        List<JSONObject> list = assetInfoService.interfaceList(param.getString("word"));
        return new BaseResult<>(true, "", list, list.size());
    }

    /**
     * 接口新增
     */
    @PostMapping("/interface/add")
    public BaseResult<String> addInterface(@RequestBody JSONObject param) {
        assetInfoService.addInterface(param.getString("name"), param.getString("url"), param.getString("description"));
        return BaseResult.success("");
    }

    /**
     * 接口保存
     */
    @PostMapping("/interface/save")
    public BaseResult<String> saveInterface(@RequestBody JSONObject param) {
        assetInfoService.saveInterface(param.getIntValue("id"), param.getString("name"), param.getString("url"),
                param.getString("description"));
        return BaseResult.success("");
    }

    /**
     * 接口删除
     */
    @PostMapping("/interface/delete")
    public BaseResult<String> deleteInterface(@RequestBody JSONObject param) {
        assetInfoService.deleteInterface(param.getIntValue("id"));
        return BaseResult.success("");
    }

    /**
     * 接口探活
     */
    @PostMapping("/interface/probe")
    public BaseResult<String> probeInterface(@RequestBody JSONObject param) {
        try {
            String data = assetInfoService.probeInterface(param.getString("url"));
            return BaseResult.success(data, "探活成功，接口正常！");
        } catch (Exception e) {
            return BaseResult.fail("接口异常：" + e.getMessage());
        }
    }

    /**
     * 数据列表/数据列表查询
     * 查询条件：字段KEY/注解
     */
    @PostMapping("/data/list")
    public BaseResult<List<JSONObject>> dataList(@RequestBody JSONObject param) {
        List<JSONObject> list = assetInfoService.dataList(param.getString("word"), param.getInteger("sendOrNot"));
        return new BaseResult<>(true, "", list, list.size());
    }

    /**
     * 映射设置保存
     */
    @PostMapping("/mapping/save")
    public BaseResult<String> saveMapping(@RequestBody JSONObject param) {
        assetInfoService.saveMapping(param.getIntValue("id"), param.getString("name"), param.getString("newKey"),
                param.getIntValue("sendOrNot"));
        return BaseResult.success("");
    }

    /**
     * 常量映射设置列表
     */
    @PostMapping("/mapping/list")
    public BaseResult<List<JSONObject>> mappingList(@RequestBody JSONObject param) {
        List<JSONObject> list = assetInfoService.mappingList(param.getString("oldKey"));
        return new BaseResult<>(true, "", list, list.size());
    }


    /**
     * 映射新增
     */
    @PostMapping("/mapping/add")
    public BaseResult<String> addMapping(@RequestBody JSONObject param) {
        try {
            assetInfoService.addMapping(param.getString("name"), param.getString("columnName"), param.getString("syncValue"),
                    param.getString("mapValue"), param.getString("description"));
            return BaseResult.success("");
        } catch (Exception e) {
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 映射编辑
     */
    @PostMapping("/mapping/update")
    public BaseResult<String> updateMapping(@RequestBody JSONObject param) {
        try {
            assetInfoService.updateMapping(param.getIntValue("id"), param.getString("mapValue"), param.getString("description"));
            return BaseResult.success("");
        } catch (Exception e) {
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 映射删除
     */
    @PostMapping("/mapping/delete")
    public BaseResult<String> deleteMapping(@RequestBody JSONObject param) {
        try {
            assetInfoService.deleteMapping(param.getIntValue("id"));
            return BaseResult.success("");
        } catch (Exception e) {
            return BaseResult.fail(e.getMessage());
        }
    }
}
