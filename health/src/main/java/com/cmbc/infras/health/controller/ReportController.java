package com.cmbc.infras.health.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.health.service.ReportService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.List;

/**
 * Description:运行报表
 * Author: zhencong
 * Date: 2021-11-23
 */
@RestController
@RequestMapping("/report")
public class ReportController {

    @Resource
    private ReportService reportService;

    /**
     * 分行查询
     * 从门户获取所有组织及角色
     */
    @PostMapping("/findBank")
    public BaseResult<JSONObject> findBank(@RequestBody JSONObject param) {
        try {
            JSONObject json = reportService.findBank(param.getIntValue("isAdmin"), param.getString("name"));
            return BaseResult.success(json);
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 能耗统计-查询
     */
    @PostMapping("/energyStatistics")
    public BaseResult<List<JSONObject>> energyStatistics(@RequestBody JSONObject param) {
        try {
            int pageSize = param.getInteger("pageSize");
            int pageCount = param.getInteger("pageCount");
            int start = pageSize * (pageCount - 1);
            List<JSONObject> list = reportService.energyStatistics(param);
            int total = list.size();
            int count = pageSize;
            List<JSONObject> res = new LinkedList<>();
            for (int i = start; count > 0 && i < total; i++, count--) {
                res.add(list.get(i));
            }
            BaseResult<List<JSONObject>> result = new BaseResult<>(true, null, res, total, pageSize, pageCount);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 能耗统计-导出
     */
    @PostMapping("/energyStatistics/export")
    public void exportEnergy(HttpServletResponse response, @RequestBody JSONObject param) {
        try {
            reportService.exportEnergy(response, param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 温湿度-查询
     */
    @PostMapping("/temperatureHumidity")
    public BaseResult<List<JSONObject>> temperatureHumidity(@RequestBody JSONObject param) {
        try {
            int pageSize = param.getInteger("pageSize");
            int pageCount = param.getInteger("pageCount");
            int start = pageSize * (pageCount - 1);
            List<JSONObject> list = reportService.temperatureHumidity(param);
            int total = list.size();
            int count = pageSize;
            List<JSONObject> res = new LinkedList<>();
            for (int i = start; count > 0 && i < total; i++, count--) {
                res.add(list.get(i));
            }
            BaseResult<List<JSONObject>> result = new BaseResult<>(true, null, res, total, pageSize, pageCount);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 温湿度-导出
     */
    @PostMapping("/temperatureHumidity/export")
    public void exportTemperatureHumidity(HttpServletResponse response, @RequestBody JSONObject param) {
        try {
            reportService.exportTemperatureHumidity(response, param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 健康评分-模板查询
     */
    @PostMapping("/healthModel")
    public BaseResult<List<JSONObject>> healthModel(@RequestBody JSONObject param) {
        List<JSONObject> list = reportService.healthModel(param);
        return BaseResult.success(list);
    }

    /**
     * 健康评分-查询
     */
    @PostMapping("/healthScore")
    public BaseResult<List<JSONObject>> healthScore(@RequestBody JSONObject param) {
        try {
            int pageSize = param.getInteger("pageSize");
            int pageCount = param.getInteger("pageCount");
            int start = pageSize * (pageCount - 1);
            List<JSONObject> list = reportService.healthScore(param);
            int total = list.size();
            int count = pageSize;
            List<JSONObject> res = new LinkedList<>();
            for (int i = start; count > 0 && i < total; i++, count--) {
                res.add(list.get(i));
            }
            BaseResult<List<JSONObject>> result = new BaseResult<>(true, null, res, total, pageSize, pageCount);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 健康评分-导出
     */
    @PostMapping("/healthScore/export")
    public void exportHealthScore(HttpServletResponse response, @RequestBody JSONObject param) {
        try {
            reportService.exportHealthScore(response, param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 告警统计-查询
     */
    @PostMapping("/alarmStatistics")
    public BaseResult<List<JSONObject>> alarmStatistics(@RequestBody JSONObject param) {
        try {
            List<JSONObject> list = reportService.alarmStatistics(param);
            return BaseResult.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 告警统计-导出
     */
    @PostMapping("/alarmStatistics/export")
    public void exportAlarmStatistics(HttpServletResponse response, @RequestBody JSONObject param) {
        try {
            reportService.exportAlarmStatistics(response, param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 维护及演练报表-表单查询
     */
    @PostMapping("/findForm")
    public BaseResult<List<JSONObject>> findForm(@RequestBody JSONObject param) {
        try {
            List<JSONObject> list = reportService.findForm(param);
            return BaseResult.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 维护及演练报表-查询
     */
    @PostMapping("/maintainDrill")
    public BaseResult<JSONObject> maintainDrill(@RequestBody JSONObject param) {
        try {
            int pageSize = param.getInteger("pageSize");
            int pageCount = param.getInteger("pageCount");
            int start = pageSize * (pageCount - 1);
            JSONObject res = reportService.maintainDrill(param);
            JSONArray array = res.getJSONArray("list");
            if (array != null) {
                List<JSONObject> data = array.toJavaList(JSONObject.class);
                int total = data.size();
                int count = pageSize;
                List<JSONObject> list = new LinkedList<>();
                for (int i = start; count > 0 && i < total; i++, count--) {
                    list.add(data.get(i));
                }
                res.put("list", list);
                return new BaseResult<>(true, null, res, total, pageSize, pageCount);
            }
            return BaseResult.success(res);
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 维护及演练报表-导出
     */
    @PostMapping("/maintainDrill/export")
    public void exportMaintainDrill(HttpServletResponse response, @RequestBody JSONObject param) {
        try {
            reportService.exportMaintainDrill(response, param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
