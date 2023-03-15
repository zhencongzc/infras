package com.cmbc.infras.health.controller;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.health.service.ModelService;
import com.cmbc.infras.util.Utils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Description:健康评分-模板控制
 * Author: zhencong
 * Date: 2021-10-11
 */
@RestController
public class ModelController {

    //存放所有生成报表的定时任务(key为modelId)
    public final static ConcurrentHashMap<String, ScheduledExecutorService> mapReportService = new ConcurrentHashMap<>();
    //存放所有运行监控的定时任务(key为modelId)
    public final static ConcurrentHashMap<String, ScheduledExecutorService> mapMonitorService = new ConcurrentHashMap<>();
    //存放所有统计分析的定时任务(key为modelId)
    public final static ConcurrentHashMap<String, ScheduledExecutorService> mapAnalysisService = new ConcurrentHashMap<>();

    @Resource
    private ModelService modelService;

    /**
     * 模板列表-快速查询
     * 条件：模板ID/标题/用途/创建人
     */
    @PostMapping("/quickFind")
    public BaseResult<List<JSONObject>> quickFind(@RequestBody JSONObject param) {
        int pageSize = param.getInteger("pageSize");
        int pageCount = param.getInteger("pageCount");
        int start = pageSize * (pageCount - 1);
        List<JSONObject> list = modelService.quickFind(param.getString("word"), start, pageSize);
        int total = modelService.getModelTotal(param.getString("word"));
        BaseResult<List<JSONObject>> result = new BaseResult<>(true, null, list, total, pageSize, pageCount);
        return result;
    }

    /**
     * 启动模板
     * 启动时连带开启“启动计分”，下个评分周期开始生成报表，关闭时测评中心不展示该模板
     * 给参与的组织创建当年和次年成绩
     */
    @PostMapping("/startModel")
    public BaseResult<String> startModel(@RequestBody JSONObject param) {
        try {
            modelService.startModel(param.getString("modelId"), param.getIntValue("startModel"));
            return BaseResult.success("");
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 启动计分
     * 启动时下个评分周期开始生成报表，关闭时不按照周期生成报告
     */
    @PostMapping("/startScore")
    public BaseResult<List<JSONObject>> startScore(@RequestBody JSONObject param) {
        modelService.startScore(param.getString("modelId"), param.getIntValue("startScore"));
        return BaseResult.success("");
    }

    /**
     * 查询评分方式
     */
    @PostMapping("/findType")
    public BaseResult<List<JSONObject>> findType() {
        List<JSONObject> list = modelService.findType();
        return BaseResult.success(list);
    }

//    /**
//     * 图片上传
//     * 返回图片url
//     */
//    @PostMapping("/uploadPicture")
//    public BaseResult<String> uploadPicture(@RequestParam("file") MultipartFile file) {
//        BaseResult<String> result = modelService.uploadPicture(file);
//        return result;
//    }

    /**
     * 模板新增-组织角色查询
     * 从门户获取所有组织及角色
     */
    @PostMapping("/findOrganizationAndRole")
    public BaseResult<JSONObject> findOrganizationAndRole(@RequestBody JSONObject param) {
        return modelService.findOrganizationAndRole();
    }

    /**
     * 数据来源
     * 从流程引擎获取所有“已发布”的表单
     */
    @PostMapping("/findResource")
    public BaseResult<List<JSONObject>> findResource(@RequestBody JSONObject param) {
        return modelService.findResource();
    }

    /**
     * 表单状态
     * 从流程引擎获取当前表单所有状态
     */
    @PostMapping("/findFormState")
    public BaseResult<List<String>> findFormState(@RequestBody JSONObject param) {
        return modelService.findFormState(param.getString("resourceId"));
    }

    /**
     * 领域列表-运行监控
     * 根据组织id从流程引擎获取不同组织存在的设备类型：温度、湿度、实时PUE、UPS负载率
     */
    @PostMapping("/findMonitorList")
    public BaseResult<List<JSONObject>> findMonitorList(@RequestBody JSONObject param) {
        return modelService.findMonitorList(param);
    }

    /**
     * 领域列表-统计分析
     * 目前只支持“告警处理”统计
     */
    @PostMapping("/findAnalysisList")
    public BaseResult<List<JSONObject>> findAnalysisList(@RequestBody JSONObject param) {
        return modelService.findAnalysisList(param);
    }

    /**
     * 新增模板
     */
    @PostMapping("/addModel")
    public BaseResult<String> addModel(@RequestBody JSONObject param) {
        String modelId = Utils.getCurrentTime("yyyyMMddHHmmss") + (new Random().nextInt(9000) + 1000);//模板ID
        try {
            modelService.addModel(param, modelId, null);
            return BaseResult.success(modelId);
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 查看模板
     */
    @PostMapping("/findModel")
    public BaseResult<JSONObject> findModel(@RequestBody JSONObject param) {
        JSONObject res = modelService.findModel(param.getString("modelId"));
        return BaseResult.success(res);
    }

    /**
     * 删除模板
     */
    @PostMapping("/deleteModel")
    public BaseResult<String> deleteModel(@RequestBody JSONObject param) {
        modelService.deleteModel(param.getString("modelId"));
        return BaseResult.success("");
    }

    /**
     * 编辑模板
     * 修改组织、角色或维度时，删除历史记录重建model，更新其他数据直接更新model
     */
    @PostMapping("/updateModel")
    public BaseResult<String> updateModel(@RequestBody JSONObject param) {
        try {
            modelService.updateModel(param);
            return BaseResult.success("");
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 编辑模板-维度新增
     */
    @PostMapping("/dimension/add")
    public BaseResult<String> addDimension(@RequestBody JSONObject param) {
        try {
            JSONObject res = modelService.addDimension(param);
            return BaseResult.success(res);
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 编辑模板-维度保存（含子叶维度）
     */
    @PostMapping("/dimension/save")
    public BaseResult<String> saveDimension(@RequestBody JSONObject param) {
        try {
            modelService.saveDimension(param);
            return BaseResult.success("");
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 编辑模板-维度删除（含子叶维度）
     */
    @PostMapping("/dimension/delete")
    public BaseResult<String> deleteDimension(@RequestBody JSONObject param) {
        try {
            modelService.deleteDimension(param);
            return BaseResult.success("");
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }


}
