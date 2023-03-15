package com.cmbc.infras.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 返回参数模板
 */
@Data
public class BaseResult<T> implements Serializable {
    private static final long serialVersionUID = 6918030871890462027L;

    /**
     * 200:成功,其它为失败
     * 500:系统错误
     */
    private int state;
    private String message;
    private T data;
    private int total;
    private int pageSize; //单页行数
    private int pageCount; //当前页码
    private boolean success;

    public BaseResult() {
    }

    public BaseResult(boolean success, String message, T data, int total) {
        if (success) {
            this.state = 200;
        } else {
            this.state = 500;
        }
        this.success = success;
        this.message = message;
        this.data = data;
        this.total = total;
    }

    public BaseResult(boolean success, String message, T data, int total, int pageSize, int pageCount) {
        if (success) {
            this.state = 200;
        } else {
            this.state = 500;
        }
        this.success = success;
        this.message = message;
        this.data = data;
        this.total = total;
        this.pageSize = pageSize;
        this.pageCount = pageCount;
    }

    public static <T> BaseResult success(T data) {
        return new BaseResult(true, "操作成功！", data, 1);
    }

    public static <T> BaseResult success(T data, String message) {
        return new BaseResult(true, message, data, 1);
    }

    public static <T> BaseResult success(T data, int total) {
        return new BaseResult(true, "操作成功！", data, total);
    }

    public static <T> BaseResult success(T data, int total, String message) {
        return new BaseResult(true, message, data, total);
    }

    public static <T> BaseResult fail(String message) {
        return new BaseResult(false, message, null, 0);
    }

}
