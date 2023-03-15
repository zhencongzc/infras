package com.cmbc.infras.system.exception;


import com.alibaba.fastjson.JSONException;
import com.cmbc.infras.dto.BaseResult;
import com.fasterxml.jackson.core.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 统一异常处理
 */
@Slf4j
@RestControllerAdvice
public class WebExceptionHandler {

    @ExceptionHandler(value = NullPointerException.class)
    public BaseResult nullPointerHandler(HttpServletRequest request, NullPointerException e) {
        log.error("空指针异常:{}", e.getMessage(), e);
        return BaseResult.fail(String.format("空指针异常:%s", e.getMessage()));
    }

    public BaseResult argumentExceptionHandler(HttpServletRequest request, IllegalArgumentException e) {
        log.error("参数异常:{}", e.getMessage(), e);
        return BaseResult.fail(String.format("参数异常:%s", e.getMessage()));
    }

    @ExceptionHandler(value = Exception.class)
    public BaseResult exceptionHandler(HttpServletRequest request, Exception e) {
        if (e instanceof JsonParseException) {
            log.error("JSON转换异常:{}", e.getMessage());
            return BaseResult.fail(String.format("JSON转换异常:%s", e.getMessage()));
        } else if (e instanceof JSONException) {
            log.error("FastJSON转换异常:{}", e.getMessage());
            return BaseResult.fail(String.format("FastJSON转换异常:%s", e.getMessage()));
        } else {
            log.error("系统异常:{}", e);
            return BaseResult.fail(String.format("系统异常:%s", e.getMessage()));
        }
    }

}
