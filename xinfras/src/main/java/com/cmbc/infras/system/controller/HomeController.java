package com.cmbc.infras.system.controller;

import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.system.service.FlowFormService;
import com.cmbc.infras.util.YmlConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 给行方的门户地址跳转用
 * 使用infras拦截器校验登录
 * 认证完成-跳转到门户首页
 * (门户没有登录认证功能)
 * 给行方入口:
 * http://40.2.160.91:18080/infras/home/portal
 */
@Slf4j
@Controller
public class HomeController {

    Logger LOG = LoggerFactory.getLogger("ExecuteAspect");

    @Resource
    private FlowFormService flowFormService;

    @RequestMapping("/home/portal")
    public void redirectHomePortal(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        LOG.info("HomeController.redirectHomePortal in ");
        //BaseResult<String> rst = flowFormService.loginProcess();
        boolean b = setCookie(req, res);
        LOG.info("HomeController.redirectHomePortal set cookie...");
        res.sendRedirect(YmlConfig.homePortal);
        LOG.info("HomeController.redirectHomePortal redirect...");
    }

    private boolean setCookie(HttpServletRequest req, HttpServletResponse res) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null || cookies.length == 0) {
            throw new RuntimeException("获取cookie信息为空！");
        }
        for (Cookie cookie : cookies) {
            LOG.info("cookie key:{},value:{}", cookie.getName(), cookie.getValue());
            res.addCookie(cookie);
        }
        return true;
    }

}
