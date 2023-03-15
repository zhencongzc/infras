package com.cmbc.infras.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Component
public class YmlConfig {

    public static String clientId;//应用编码G26
    public static String secret;
    public static String redirectUrl;
    public static String redirectAlarmPage;//重定向告警页路径-过不过nginx-(infras_web or infras)
    public static String authorizeUrl;//跳转到认证模块的地址
    public static String tokenUrl;//认证模块授权token的地址
    public static String keUrl;//ke 接口地址
    public static String keEncrtyKey;//ke 加密的key
    public static String simpleAdminUrl;//门户地址
    public static String simpleAdminIndexUrl;//门户首页地址
    public static String userInfoUrl;//认证平台用户信息的地址
    public static String logoutUrl;//退出地址
    public static String messageUrl;//message
    public static String oaHome;//OA跳转首页地址
    public static String redisAddress;//redis.address
    public static String redisPassword;//password
    public static String redisCluster;//is cluster
    public static String loginInception;//是否开启登录拦击
    public static String whiteString;//Filter白名单
    public static List<String> whiteList;//白名单List
    public static String loginTest;//是否使用测试账号
    public static String switchRefresh;//第三方告警页首次进入是否刷新一次-开关
    public static String evaluateFormId;//流程表单_综合评价_表单IDz
    public static String partolFormId;//巡检动态_流程表单
    public static String maintainFormId;//维护任务_流程表单
    public static String deduceFormId;//演练任务_流程表单
    public static String scoreFormId;//实时得分-年平均分-流程表单
    public static String msClientId;//生产运营部生产调度中心-三方接口调用
    public static String msCode;//生产运营部生产调度中心-三方接口调用
    public static String annotationLog;//自定义注解-是否打印日志(查询结果多，日志太长)
    public static String homePortal;//infras重定向到门户首页地址
    public static String configs;//key:value格式配置
    //移动端服务认证用
    public static String mobile_authorize_url;
    public static String client_id;
    public static String client_secret;
    public static String username;
    public static String password;
    public static String grant_type;

    @Value("${mobile.authorize.url}")
    public void setMobileAuthorizeUrl(String mobile_authorize_url) {
        YmlConfig.mobile_authorize_url = mobile_authorize_url;
    }

    @Value("${mobile.authorize.client_id}")
    public void setClient_id(String client_id) {
        YmlConfig.client_id = client_id;
    }

    @Value("${mobile.authorize.client_secret}")
    public void setClient_secret(String client_secret) {
        YmlConfig.client_secret = client_secret;
    }

    @Value("${mobile.authorize.username}")
    public void setUsername(String username) {
        YmlConfig.username = username;
    }

    @Value("${mobile.authorize.password}")
    public void setPassword(String password) {
        YmlConfig.password = password;
    }

    @Value("${mobile.authorize.grant_type}")
    public void setGrant_type(String grant_type) {
        YmlConfig.grant_type = grant_type;
    }

    @Value("${authorize.clientId:}")
    public void setClientId(String id) {
        clientId = id;
    }

    @Value("${authorize.secret:}")
    public void setSecret(String secretVal) {
        secret = secretVal;
    }

    @Value("${authorize.redirectUrl:}")
    public void setRedirectUrl(String redirectUrlVal) {
        redirectUrl = redirectUrlVal;
    }

    @Value("${authorize.redirectAlarmPage:/infras/alarm/event.html}")
    public void setRedirectAlarmPage(String redirectAlarmPageVal) {
        redirectAlarmPage = redirectAlarmPageVal;
    }

    @Value("${authorize.authorizeUrl:}")
    public void setAuthorizeUrl(String authorize) {
        authorizeUrl = authorize;
    }

    @Value("${authorize.tokenUrl:}")
    public void setTokenUrl(String token) {
        tokenUrl = token;
    }

    @Value("${authorize.userInfoUrl:}")
    public void setUserInfoUrl(String userInfo) {
        userInfoUrl = userInfo;
    }

    @Value("${authorize.logoutUrl:}")
    public void setLogoutUrl(String logout) {
        logoutUrl = logout;
    }

    @Value("${authorize.messageUrl:}")
    public void setMessageUrl(String messageUrlVal) {
        messageUrl = messageUrlVal;
    }

    @Value("${infras.oa_home:}")
    public void setOaHome(String value) {
        oaHome = value;
    }

    @Value("${redis.addrs}")
    public void setRedisAddres(String address) {
        redisAddress = address;
    }

    @Value("${redis.passwd}")
    public void setRedisPassword(String password) {
        redisPassword = password;
    }

    @Value("${redis.cluster}")
    public void setRedisCluster(String cluster) {
        redisCluster = cluster;
    }

    @Value("${login.inception:}")
    public void setLoginInception(String loginInceptionValue) {
        loginInception = loginInceptionValue;
    }

    @Value("${ke-rpc.server:http://127.0.0.1}")
    public void setKeUrl(String keUrlValue) {
        keUrl = keUrlValue;
    }

    @Value("${ke.encrtyKey:}")
    public void setKeEncrtyKey(String keEncrtyKeyValue) {
        keEncrtyKey = keEncrtyKeyValue;
    }

    @Value("${simple.admin.url:http://127.0.0.1/auth/sso/login}")
    public void setSimpleAdminUrl(String simpleAdminUrlValue) {
        simpleAdminUrl = simpleAdminUrlValue;
    }

    @Value("${simple.admin.index.url:http://127.0.0.1}")
    public void setSimpleAdminIndexUrl(String simpleAdminIndexUrlValue) {
        simpleAdminIndexUrl = simpleAdminIndexUrlValue;
    }

    @Value("${login.test:}")
    public void setLoginTest(String loginTestValue) {
        loginTest = loginTestValue;
    }

    @Value("${authorize.whiteString:}")
    public void setWhiteString(String whiteStringVal) {
        whiteString = StringUtil.removeSpecialChar(whiteStringVal);
        whiteList = new ArrayList<>();
        String[] arr = whiteString.split(",");
        for (int i = 0; i < arr.length; i++) {
            String item = arr[i].trim();
            whiteList.add(item);
        }
    }

    @Value("${flowForm.evaluate.formId:202112091604466996}")
    public void setEvaluateFormId(String evaluateFormIdStr) {
        evaluateFormId = evaluateFormIdStr;
    }

    @Value("${flowForm.partol.formId:913450999554461696}")
    public void setPartolFormId(String value) {
        partolFormId = value;
    }

    //维护任务表单
    @Value("${flowForm.maintain.formId:915630045579137024}")
    public void setMaintainFormId(String value) {
        maintainFormId = value;
    }

    //演练任务表单
    @Value("${flowForm.deduce.formId:915630138457804800}")
    public void setDeduceFormId(String value) {
        deduceFormId = value;
    }

    //实时得分-年平均分表单
    @Value("${flowForm.score.formId:202112091604466996}")
    public void setScoreFormId(String value) {
        scoreFormId = value;
    }

    @Value("${interface.lastCount.clientId}")
    public void setMsClientId(String clientId) {
        msClientId = clientId;
    }

    @Value("${interface.lastCount.code}")
    public void setMsCode(String code) {
        msCode = code;
    }

    @Value("${interface.switchRefresh:close}")
    public void setSwitchRefresh(String value) {
        switchRefresh = value;
    }

    @Value("${self.annotation.log.open:false}")
    public void setAnnotationLog(String value) {
        annotationLog = value;
    }

    @Value("${infras.home.portal:http://39.106.43.98:8088/portal}")
    public void setHomePortal(String value) {
        homePortal = value;
    }

    @Value("${infras.common.configs:}")
    public void setConfigs(String value) {
        configs = value;
    }

    /**
     * 通用配置 key:value
     */
    public static boolean getBoolValue(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        if (StringUtils.isBlank(configs)) {
            return false;
        }
        try {
            JSONObject obj = JSONObject.parseObject(configs);
            String value = obj.getString(key);
            if (StringUtils.isBlank(value)) {
                return false;
            }
            if ("true".equals(value) || "1".equals(value)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getStringValue(String key) {
        Assert.hasLength(key, "YmlConfig.getStringValue key is empty");
        if (!effectConfig()) {
            return null;
        }
        JSONObject obj = JSONObject.parseObject(configs);
        return obj.getString(key);
    }

    /**
     * 通用配置是否有效
     */
    private static boolean effectConfig() {
        if (StringUtils.isBlank(configs)) {
            return false;
        }
        JSONObject obj = null;
        try {
            obj = JSONObject.parseObject(configs);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
