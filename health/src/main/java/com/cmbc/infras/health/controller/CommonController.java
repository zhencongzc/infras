package com.cmbc.infras.health.controller;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.health.service.CommonService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * Description:健康评分-通用控制
 * Author: zhencong
 * Date: 2021-10-18
 */
@RestController
@RequestMapping("/common")
public class CommonController {

    @Resource
    private CommonService commonService;

    /**
     * 查询账户组织和权限
     */
    @PostMapping("/findAuthority")
    public BaseResult<JSONObject> findAuthority(@RequestBody JSONObject param) {
        return commonService.findAuthority(param.getIntValue("id"));
    }

    /**
     * 综合评价
     * 集中监控访问的接口，返回所有组织不同维度的成绩
     */
    @PostMapping("/evaluation")
    public BaseResult<List<JSONObject>> evaluation(@RequestBody JSONObject param) {
        return commonService.evaluation(param.getString("modelId"));
    }

    /**
     * 维护进度
     * 集中监控访问的接口，返回当前模板，当前组织的实时成绩和年平均分
     */
    @PostMapping("/maintainRate")
    public BaseResult<JSONObject> maintainRate(@RequestBody JSONObject param) {
        return commonService.maintainRate(param.getString("modelId"), param.getString("name"));
    }
}
