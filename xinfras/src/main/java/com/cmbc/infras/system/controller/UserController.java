package com.cmbc.infras.system.controller;

import com.alibaba.fastjson.JSON;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.auth.UserDto;
import com.cmbc.infras.system.service.FlowFormService;
import com.cmbc.infras.system.service.UserService;
import com.cmbc.infras.system.util.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Slf4j
@Controller
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private FlowFormService flowFormService;

    @GetMapping("/userBankId")
    public String getUserBankId(@RequestParam("account") String account) {
        return flowFormService.getUserBankId(account);
    }

//    /**
//     * 移动OA入口-回调-system端处理
//     */
//    @RequestMapping("/app/callback")
//    public String appCallback(HttpServletRequest request, HttpServletResponse response, String code, Model model) {
//        log.info("移动OA进入 xinfras callback code:" + code);
//        BaseResult<UserDto> result = userService.loginProcess(request, response, code);
//        if (result.isSuccess()) {
//            model.addAttribute("success", "false");
//            model.addAttribute("message", result.getMessage());
//        } else {
//            UserDto user = result.getData();
//            model.addAttribute("success", "true");
//            model.addAttribute("user", JSON.toJSONString(user));
//        }
//        return "successApp";
//    }

    /**
     * 机房监控告警页面登录接口
     * 通过account免密登录门户、KE
     * 机房监控嵌入的iframe界面使用
     */
    @RequestMapping("/loginPortalKe")
    @ResponseBody
    public BaseResult<UserDto> loginPortalKe(HttpServletResponse response, String QWEASDZXC) {
//        String encode = URLEncoder.encode(QWEASDZXC, "utf-8"); //本地测试用
        //将用户名account解密
        String account = EncryptionUtil.decryption(QWEASDZXC, "ITPTL");
        log.info("机房监控告警页面登录接口 QWEASDZXC:{},account:{}", QWEASDZXC, account);
        //根据account免密登录门户和KE并设置cookie
        BaseResult<UserDto> res = userService.login(response, account);
        return res;
    }

}
