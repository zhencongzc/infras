package com.cmbc.infras.system.rpc;

import com.alibaba.fastjson.JSONObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 健康评分接口
 */
@FeignClient(name = "health", url = "${ke-rpc.server}")
public interface HealthRpc {

    /**
     * 分行主界面-维护进度
     * 实时得分,年增均分
     */
    @PostMapping("/health/common/maintainRate")
    String getScoreData(JSONObject param);

    /**
     * 总行监控-综合评价
     */
    @PostMapping("/health/common/evaluation")
    String evaluation(JSONObject param);

}
