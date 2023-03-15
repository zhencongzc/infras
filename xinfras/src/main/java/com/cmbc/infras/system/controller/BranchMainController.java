package com.cmbc.infras.system.controller;

import com.cmbc.infras.dto.BaseParam;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.ops.*;
import com.cmbc.infras.system.service.BranchMainService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 分行主界面
 */
@RestController
public class BranchMainController {

    @Resource
    private BranchMainService branchMainService;

    /**
     * 雷达告警
     */
    @RequestMapping("/bramain/branchRadar")
    public BaseResult<List<BranchRadar>> getBranchRadar(BaseParam param) {
        return branchMainService.getBranchRadar(param);
    }

    /**
     * 能耗管理
     */
    @RequestMapping("/bramain/energy")
    public BaseResult<Energy> getEnergyData(BaseParam param) {
        return branchMainService.getEnergyData(param);
    }

    /**
     * 机房温湿度
     */
    @RequestMapping("/bramain/humiture")
    public BaseResult<List<Humiture>> getHumiture(BaseParam param) {
        return branchMainService.getHumiture(param);
    }

    /**
     * 告警汇总-告警处理率
     * 未处理：未处理的/总数
     * 处理中：已受理的/总数
     * 已处理：已确认的/总数
     */
    @RequestMapping("/bramain/disposeRate")
    public BaseResult<DisposeRate> getDisposeRate(BaseParam param) {
        return branchMainService.getDisposeRate(param);
    }

    /**
     * 告警汇总-告警级别分类
     */
    @RequestMapping("/bramain/gradeRate")
    public BaseResult<GradeRate> getGradeRate(BaseParam param) {
        return branchMainService.getGradeRate(param);
    }

    /**
     * 容量管理
     */
    @RequestMapping("/bramain/capacity")
    public BaseResult<Capacity> getCapacity(BaseParam param) {
        return branchMainService.getCapacity(param);
    }

    /**
     * 维护进度
     * 作业复合进度：按照创建时间查当年已关闭数据/当年总数
     * 巡检动态进度：按照创建时间查当天已关闭数据/当天总数
     */
    @RequestMapping("/bramain/maintainRate")
    public BaseResult<Maintain> getMaintainRate(BaseParam param) {
        return branchMainService.getMaintainRate(param);
    }

}
