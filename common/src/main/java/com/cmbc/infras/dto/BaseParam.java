package com.cmbc.infras.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class BaseParam implements Serializable {
    private static final long serialVersionUID = 6771439586099682436L;

    /**
     * 用户
     */
    private String account;
    /**
     * 用户认证redis_key
     */
    private String code;
    /**
     * 向下递归查询层级
     * 总行,分行,二级分行,支行,村镇银行
     */
    private int level = 5;
    /**
     * 所查银行ID
     */
    private String bankId;

    /**
     * 区分请求页面
     * "main":分行主界面
     * "ops":分行运维界面
     */
    private String page;

}
