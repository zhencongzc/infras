package com.cmbc.infras.health.controller;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.health.service.AssessService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Description:健康评分-测评控制
 * Author: zhencong
 * Date: 2021-10-14
 */
@RestController
@RequestMapping("/assess")
public class AssessController {

    @Resource
    private AssessService assessService;

    /**
     * 测评列表-快速查询
     * 标题/用途
     */
    @PostMapping("/quickFind")
    public BaseResult<List<JSONObject>> quickFind(@RequestBody JSONObject param) {
        List<JSONObject> list = assessService.quickFind(param.getIntValue("isAdmin"), param.getIntValue("isAuditRole"), param.getString("name"),
                param.getString("word"));
        return BaseResult.success(list);
    }

    /**
     * 当前测评-评分排名
     * 按得分降序
     */
    @PostMapping("/scoreRank")
    public BaseResult<JSONObject> scoreRank(@RequestBody JSONObject param) {
        JSONObject res = assessService.scoreRank(param.getString("modelId"));
        return BaseResult.success(res);
    }

    /**
     * 历史测评-评分排名
     * 查询某个历史version的排名数据，按得分降序
     */
    @PostMapping("/historyScoreRank")
    public BaseResult<JSONObject> historyScoreRank(@RequestBody JSONObject param) {
        JSONObject res = assessService.historyScoreRank(param.getString("modelId"), param.getIntValue("version"));
        return BaseResult.success(res);
    }

    /**
     * 测评维度
     * 显示当年数据、到达填写周期（起）时，显示次年数据，起止周期内可以填写次年数据
     */
    @PostMapping("/assessDimension")
    public BaseResult<JSONObject> assessDimension(@RequestBody JSONObject param) {
        JSONObject result = assessService.assessDimension(param.getString("modelId"), param.getString("name"), param.getIntValue("isAdmin"),
                param.getIntValue("isAuditRole"));
        return BaseResult.success(result);
    }

    /**
     * 处理记录
     */
    @PostMapping("/record")
    public BaseResult<List<JSONObject>> record(@RequestBody JSONObject param) {
        List<JSONObject> result = assessService.record(param.getString("modelId"), param.getString("name"), param.getIntValue("id"),
                param.getIntValue("year"));
        return BaseResult.success(result);
    }

    /**
     * 单选查询
     */
    @PostMapping("/findSingle")
    public BaseResult<List<JSONObject>> findSingle(@RequestBody JSONObject param) {
        List<JSONObject> list = assessService.findSingle(param.getString("modelId"), param.getString("id"), param.getString("name"),
                param.getIntValue("year"));
        return BaseResult.success(list);
    }

    /**
     * 单选提交
     */
    @PostMapping("/commitSingle")
    public BaseResult<String> commitSingle(@RequestBody JSONObject param) {
        assessService.commitSingle(param);
        return BaseResult.success("");
    }

    /**
     * 证明材料上传
     * 上传到服务器
     */
    @PostMapping("/uploadDocument")
    public BaseResult<String> uploadDocument(MultipartFile file, int id) {
        if (null != file) return assessService.uploadDocument(file, id);
        return BaseResult.fail("file不能为空");
    }

    /**
     * 证明材料下载
     */
    @RequestMapping("/downloadDocument")
    public void downloadDocument(HttpServletResponse response, @RequestParam String document, @RequestParam String fileName) {
        assessService.downloadDocument(response, document, fileName);
    }

    /**
     * 证明材料删除
     */
    @PostMapping("/deleteDocument")
    public BaseResult<String> deleteDocument(@RequestBody JSONObject param) {
        BaseResult<String> result = assessService.deleteDocument(param.getIntValue("id"), param.getString("document"));
        return result;
    }

    /**
     * 单选审核查询
     */
    @PostMapping("/findSingleAudit")
    public BaseResult<List<JSONObject>> findSingleAudit(@RequestBody JSONObject param) {
        List<JSONObject> list = assessService.findSingleAudit(param.getString("modelId"), param.getString("id"), param.getString("name"),
                param.getIntValue("year"));
        return BaseResult.success(list);
    }

    /**
     * 单选审核提交
     */
    @PostMapping("/commitSingleAudit")
    public BaseResult<String> commitSingleAudit(@RequestBody JSONObject param) {
        assessService.commitSingleAudit(param);
        return BaseResult.success("");
    }

    /**
     * 累计扣分查询
     */
    @PostMapping("/findDeduct")
    public BaseResult<List<JSONObject>> findDeduct(@RequestBody JSONObject param) {
        List<JSONObject> list = assessService.findDeduct(param.getString("modelId"), param.getString("id"), param.getString("name"),
                param.getIntValue("year"));
        return BaseResult.success(list);
    }

    /**
     * 数据列表
     * 从流程引擎获取当前组织的单据数据
     */
    @PostMapping("/getSourceData")
    public BaseResult<List<JSONObject>> getSourceData(@RequestBody JSONObject param) {
        try {
            List<JSONObject> list = assessService.getSourceData(param);
            return BaseResult.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 数据列表-移除
     */
    @PostMapping("/deleteSourceData")
    public BaseResult<String> deleteSourceData(@RequestBody JSONObject param) {
        assessService.deleteSourceData(param);
        return BaseResult.success("");
    }

    /**
     * 累计扣分提交
     */
    @PostMapping("/commitDeduct")
    public BaseResult<String> commitDeduct(@RequestBody JSONObject param) {
        assessService.commitDeduct(param);
        return BaseResult.success("");
    }

    /**
     * 累计扣分审核查询
     */
    @PostMapping("/findDeductAudit")
    public BaseResult<List<JSONObject>> findDeductAudit(@RequestBody JSONObject param) {
        List<JSONObject> list = assessService.findDeductAudit(param.getString("modelId"), param.getString("id"), param.getString("name"),
                param.getIntValue("year"));
        return BaseResult.success(list);
    }

    /**
     * 更新数据
     */
    @PostMapping("/updateFormData")
    public BaseResult<String> updateFormData(@RequestBody JSONObject param) {
        try {
            assessService.updateFormData(param);
            return BaseResult.success("");
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 标准值保存
     */
    @PostMapping("/standardValue/save")
    public BaseResult<String> saveStandardValue(@RequestBody JSONObject param) {
        try {
            assessService.saveStandardValue(param);
            return BaseResult.success("");
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 累计扣分审核提交
     */
    @PostMapping("/commitDeductAudit")
    public BaseResult<String> commitDeductAudit(@RequestBody JSONObject param) {
        assessService.commitDeductAudit(param);
        return BaseResult.success("");
    }

    /**
     * 运行监控查询
     */
    @PostMapping("/findMonitor")
    public BaseResult<List<JSONObject>> findMonitor(@RequestBody JSONObject param) {
        List<JSONObject> list = assessService.findMonitor(param.getString("modelId"), param.getString("id"), param.getString("name"),
                param.getIntValue("year"));
        return BaseResult.success(list);
    }

    /**
     * 统计分析查询
     */
    @PostMapping("/findAnalysis")
    public BaseResult<List<JSONObject>> findAnalysis(@RequestBody JSONObject param) {
        List<JSONObject> list = assessService.findAnalysis(param.getString("modelId"), param.getString("id"), param.getString("name"),
                param.getIntValue("year"));
        return BaseResult.success(list);
    }

    /**
     * 历史测评列表-快速查询
     * 标题/用途
     */
    @PostMapping("/historyQuickFind")
    public BaseResult<List<JSONObject>> historyQuickFind(@RequestBody JSONObject param) {
        int pageSize = param.getInteger("pageSize");
        int pageCount = param.getInteger("pageCount");
        int start = pageSize * (pageCount - 1);
        String name = param.getString("name");
        List<JSONObject> list;
        List<JSONObject> listTotal;
        int total;
        //是管理员或审核人查询全部银行数据
        if (param.getIntValue("isAdmin") == 1 || param.getIntValue("isAuditRole") == 1) {
            list = assessService.adminHistoryQuickFind(param.getString("word"), start, pageSize);
            listTotal = assessService.adminGetAssessTotal(param.getString("word"), start, pageSize);
            total = listTotal.size() == 0 ? 0 : listTotal.get(0).getInteger("total");
        } else {
            list = assessService.historyQuickFind(name, param.getString("word"), start, pageSize);
            listTotal = assessService.getAssessTotal(name, param.getString("word"), start, pageSize);
            total = listTotal.size() == 0 ? 0 : listTotal.get(0).getInteger("total");
        }
        return new BaseResult<>(true, null, list, total, pageSize, pageCount);
    }

    /**
     * 测评结果
     */
    @PostMapping("/assessResult")
    public BaseResult<JSONObject> assessResult(@RequestBody JSONObject param) {
        try {
            JSONObject jo = assessService.assessResult(param.getIntValue("id"));
            return BaseResult.success(jo);
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 当前测评-查看报表
     */
    @RequestMapping("/checkReport")
    public void checkReport(HttpServletResponse response, @RequestParam String modelId, @RequestParam String name) {
        assessService.checkReport(response, modelId, name);
    }

    /**
     * 历史测评-修正导出
     */
    @PostMapping("/correctExport")
    public void correctExport(HttpServletResponse response, @RequestBody JSONObject param) {
        try {
            assessService.correctExport(response, param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 历史测评-导出
     */
    @RequestMapping("/export")
    public void export(HttpServletResponse response, @RequestParam int id) {
        assessService.export(response, id);
    }

    /**
     * 当前测评-导出报表
     */
    @RequestMapping("/exportZip/now")
    public void exportZipNow(HttpServletResponse response, @RequestParam String modelId) {
        assessService.exportZipNow(response, modelId);
    }

    /**
     * 历史测评-导出报表
     */
    @RequestMapping("/exportZip")
    public void exportZip(HttpServletResponse response, @RequestParam String modelId, @RequestParam Integer version) {
        assessService.exportZip(response, modelId, version);
    }
}
