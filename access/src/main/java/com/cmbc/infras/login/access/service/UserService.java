package com.cmbc.infras.login.access.service;

import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.auth.UserDto;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface UserService {

    BaseResult<UserDto> loginProcess(HttpServletRequest request, HttpServletResponse response, String code);

}
