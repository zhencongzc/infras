package com.cmbc.infras.dto.rpc;

import lombok.Data;

/**
 * 流程引擎用返回参数模板
 */
@Data
public class BaseResultForFlow<T> {

    /**
     * 200：操作成功
     * 500：服务器内部错误
     * 1000：鉴权不通过
     */
    private int status;
    private String message;
    private T data;
    private boolean hasNextPage;

    public BaseResultForFlow(int status, String message, T data, boolean hasNextPage) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.hasNextPage = hasNextPage;
    }

}
