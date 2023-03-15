package com.cmbc.infras.login.access.service;

import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.auth.UserDto;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;

/**
 * @desc 登录接口操作类
 * @author hdw
 */
public interface LoginService {

    BaseResult<String> logout(String code);

    BaseResult<UserDto> callBack(HttpServletRequest request, HttpServletResponse response, String code, String target) throws UnsupportedEncodingException;

    BaseResult<UserDto> mobileCallback(String code);

    String getKeToken(HttpServletRequest request, HttpServletResponse response, String code, String target);

    //UserDto loginKe(HttpServletResponse response, String account);

}
