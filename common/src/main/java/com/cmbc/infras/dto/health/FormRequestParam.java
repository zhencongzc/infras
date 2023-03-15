package com.cmbc.infras.dto.health;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * “基础设施”表单数据的请求参数
 */
@Data
public class FormRequestParam {

    private String configDataId = "JCSS";
    private JSONObject query;

    public FormRequestParam(String id) {
        query = new JSONObject();
        JSONObject search = new JSONObject();
        JSONObject bank_id = new JSONObject();
        bank_id.put("value", id);
        search.put("bank_id", bank_id);
        query.put("search", search);
        query.put("withcSubdata", "true");
    }
}
