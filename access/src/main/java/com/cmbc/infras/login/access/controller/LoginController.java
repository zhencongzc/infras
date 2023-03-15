package com.cmbc.infras.login.access.controller;

import com.alibaba.fastjson.JSON;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.auth.UserDto;
import com.cmbc.infras.login.access.rpc.UserBankRpc;
import com.cmbc.infras.login.access.service.LoginService;
import com.cmbc.infras.login.access.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;

@Controller
@Slf4j
public class LoginController {

    @Resource
    private UserService userService;
    @Resource
    private LoginService loginService;

    @ResponseBody
    @GetMapping("/getKeToken")
    public String getKeToken(HttpServletRequest request, HttpServletResponse response, String code, String target) {
        return loginService.getKeToken(request, response, code, target);
    }

    @ResponseBody
    @RequestMapping("/logout")
    public ModelAndView logout(String code) {
        BaseResult<String> result = loginService.logout(code);
        if (result.isSuccess()) {
            return new ModelAndView(new RedirectView(result.getData()));
        }
        return new ModelAndView();
    }

    //跳转门户首页
    @RequestMapping("/toLoginAdmin")
    public String toLoginAdmin(String code, Model model) {
        log.info("LoginController.toLoginAdmin......");
        model.addAttribute("code", code);
        model.addAttribute("target", "home");
        return "loginadmin";
    }

    //跳转门户首页
    @RequestMapping("/toLoginAccess")
    public String toLoginAccess(String code, Model model) {
        log.info("LoginController.toLoginAccess......");
        model.addAttribute("code", code);
        model.addAttribute("target", "home");
        return "success";
    }

    //第三方告警页-跳转
    @RequestMapping("/toAlarmPage")
    public String toAlarmPage(String code, Model model) {
        log.info("LoginController.toAlarmPage......");
        model.addAttribute("code", code);
        model.addAttribute("target", "alarmPage");
        return "success";
    }

    //第三方告警数量查询
    @RequestMapping("/toLastCount")
    public String toLastCount(String code, Model model) {
        log.info("LoginController.toLastCount......");
        model.addAttribute("code", code);
        model.addAttribute("target", "lastCount");
        return "success";
    }

    @GetMapping("/info/authorizeCallback")
    @ResponseBody
    public BaseResult<UserDto> authorizeCallback(HttpServletRequest request, HttpServletResponse response, String code, String target) {
        log.info("LoginController.authorizeCallback...code:" + code + ",target:" + target);
        BaseResult<UserDto> result;
        try {
            result = loginService.callBack(request, response, code, target);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
        UserDto user = result.getData();
        user.setExtend("");
        return result;
    }

    /**
     * 移动OA入口-回调-access端处理
     */
    @RequestMapping("/app/callback")
    @ResponseBody
    public BaseResult<String> appCallback(HttpServletRequest request, HttpServletResponse response, String code, Model model) {
        log.info("移动OA进入 access appCallback code:" + code);
        BaseResult<UserDto> result = userService.loginProcess(request, response, code);
        log.info("移动OA结束 access appCallback result:" + result);
        if (result.isSuccess()) {
            return BaseResult.success("");
        } else {
            return BaseResult.fail("");
        }
    }

}
