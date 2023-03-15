package com.cmbc.infras.system.util;

import com.cmbc.infras.dto.BaseResult;
import org.apache.commons.lang3.StringUtils;

/**
 * WebSocket推送code解析工具
 * 账号:银行ID:页面标识
 * eg:"beijing1:0_723:branch-main"
 * 总行跳转:"admin:0_723:branch-ops"
 */
public class WSDecodeUtil {

    /**
     * 账号:银行ID:页面标识
     * eg:"beijing1:0_723:branch-main"
     * 总行跳转:"admin:0_723:branch-ops"
     */
    public static BaseResult<String> getBankResourceId(String code) {
        if (StringUtils.isBlank(code)) {
            return BaseResult.fail("参数code为空");
        }
        String[] arr = code.split(":");
        if (arr.length < 3) {
            return BaseResult.fail("参数code格式不正确");
        }
        String account = arr[0];
        String bankId = arr[1];
        String pageTag = arr[2];
        if (StringUtils.isBlank(bankId)) {
            return BaseResult.fail("BankId为空！");
        }
        return BaseResult.success(bankId);
    }

}
