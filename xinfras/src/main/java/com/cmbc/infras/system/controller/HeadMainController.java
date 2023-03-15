package com.cmbc.infras.system.controller;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseParam;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.ops.*;
import com.cmbc.infras.system.service.HeadMainService;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 总行监控
 */
@RestController
public class HeadMainController {

    @Resource
    private HeadMainService headMainService;

    /**
     * 站点统计（生产运营工作台也有）
     * 做缓存，5分钟更新一次
     */
    @RequestMapping("/head/siteStatis")
    public BaseResult<SiteStatis> getSiteStatis() {
        try {
            return headMainService.getSiteStatis();
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 雷达告警（告警汇总）
     * 查询历史告警
     */
    @RequestMapping("/head/bankRadar")
    public BaseResult<AlarmRadar> getBankRadar(BaseParam param) {
        Assert.hasLength(param.getBankId(), "参数[bankId]不能为空!");
        return headMainService.getBankAlarmRadar(param.getBankId());
    }

    /**
     * 机房温湿度
     */
    @RequestMapping("/head/humiture")
    public BaseResult<List<Humiture>> getHumiture() {
        return headMainService.getHumiture();
    }

    /**
     * 巡检动态
     * 按照创建时间查当天未关闭数据
     */
    @RequestMapping("/head/partol")
    public BaseResult<List<JSONObject>> getPartol() {
        return headMainService.getPartol();
    }

    /**
     * 作业复核
     * 按照创建时间查当年未关闭数据
     */
    @RequestMapping("/head/workCheck")
    public BaseResult<List<JSONObject>> getWorkCheck() {
        return headMainService.getWorkCheck();
    }

    /**
     * 综合评价
     */
    @RequestMapping("/head/evaluates")
    public BaseResult<List<Evaluate>> getEvaluates() {
        return headMainService.getEvaluate();
    }

}
