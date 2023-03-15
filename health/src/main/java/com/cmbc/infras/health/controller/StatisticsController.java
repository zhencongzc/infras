package com.cmbc.infras.health.controller;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.health.service.StatisticsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * Description:健康评分二期-综合概览
 * Author: zhencong
 * Date: 2022-9-6
 */
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    /**
     * 评分评级
     */
    @PostMapping("/rating")
    public BaseResult<List<JSONObject>> rating(@RequestBody JSONObject param) {
        try {
            List<JSONObject> res = statisticsService.rating(param.getString("modelId"));
            return BaseResult.success(res, res.size());
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 综合趋势
     * 从当前日期起往前推，每隔7天查一次模板，查询12周的，展示日期和总分的中位数
     */
    @PostMapping("/trend")
    public BaseResult<List<JSONObject>> trend(@RequestBody JSONObject param) {
        try {
            List<JSONObject> res = statisticsService.trend(param.getString("modelId"));
            return BaseResult.success(res, res.size());
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail("请求失败！");
        }
    }

    /**
     * 评分概览
     */
    @PostMapping("/overview")
    public BaseResult<JSONObject> overview(@RequestBody JSONObject param) {
        try {
            JSONObject res = statisticsService.overview(param.getString("modelId"));
            return BaseResult.success(res);
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail("请求失败！");
        }
    }

    /**
     * 综合排名
     */
    @PostMapping("/rank")
    public BaseResult<List<JSONObject>> rank(@RequestBody JSONObject param) {
        try {
            List<JSONObject> res = statisticsService.rank(param.getString("modelId"));
            return BaseResult.success(res);
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail("请求失败！");
        }
    }

    /**
     * 各维度得分率
     */
    @PostMapping("/scoreRate")
    public BaseResult<List<JSONObject>> scoreRate(@RequestBody JSONObject param) {
        try {
            List<JSONObject> res = statisticsService.scoreRate(param.getString("modelId"));
            return BaseResult.success(res, res.size());
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail("请求失败！");
        }
    }

    /**
     * 各维度趋势
     * 从当前日期起往前推，每隔7天查一次模板，查询12周的，展示日期和各维度总分的中位数
     */
    @PostMapping("/dimensionTrend")
    public BaseResult<List<JSONObject>> dimensionTrend(@RequestBody JSONObject param) {
        try {
            List<JSONObject> res = statisticsService.dimensionTrend(param.getString("modelId"));
            return BaseResult.success(res, res.size());
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail("请求失败！");
        }
    }

    /**
     * 各维度得分率（分行）
     */
    @PostMapping("/branch/scoreRate")
    public BaseResult<List<JSONObject>> branchScoreRate(@RequestBody JSONObject param) {
        try {
            List<JSONObject> res = statisticsService.branchScoreRate(param.getString("modelId"), param.getString("name"));
            return BaseResult.success(res, res.size());
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail("请求失败！");
        }
    }

    /**
     * 分值趋势（分行）
     * 从当前日期起往前推，每隔7天查一次模板，查询12周的，展示日期和各维度的分数
     */
    @PostMapping("/branch/scoreTrend")
    public BaseResult<List<JSONObject>> branchScoreTrend(@RequestBody JSONObject param) {
        try {
            List<JSONObject> res = statisticsService.branchScoreTrend(param.getString("modelId"), param.getString("name"));
            return BaseResult.success(res, res.size());
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail("请求失败！");
        }
    }

    /**
     * 各维度排名（分行）
     */
    @PostMapping("/branch/dimensionRank")
    public BaseResult<List<JSONObject>> branchDimensionRank(@RequestBody JSONObject param) {
        try {
            List<JSONObject> res = statisticsService.branchDimensionRank(param.getString("modelId"), param.getString("name"));
            return BaseResult.success(res, res.size());
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail("请求失败！");
        }
    }

    /**
     * 分行概览（分行）
     */
    @PostMapping("/branch/overview")
    public BaseResult<JSONObject> branchOverview(@RequestBody JSONObject param) {
        try {
            JSONObject res = statisticsService.branchOverview(param.getString("modelId"), param.getString("name"));
            return BaseResult.success(res, res.size());
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail("请求失败！");
        }
    }

    /**
     * 评分细则（分行）
     */
    @PostMapping("/branch/scoreDetail")
    public BaseResult<List<JSONObject>> branchScoreDetail(@RequestBody JSONObject param) {
        try {
            List<JSONObject> res = statisticsService.branchScoreDetail(param.getString("modelId"), param.getString("name"));
            return BaseResult.success(res, res.size());
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail("请求失败！");
        }
    }

}
