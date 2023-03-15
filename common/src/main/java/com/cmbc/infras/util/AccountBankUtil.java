package com.cmbc.infras.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.dto.auth.UserDto;
import com.cmbc.infras.redis.DataRedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.net.HttpCookie;
import java.util.List;

public class AccountBankUtil {

    private static final Logger log = LoggerFactory.getLogger(AccountBankUtil.class);

    /**
     * 通过账号取得银行名
     */
    public static String getAccountBankName(String account, String sessionId) {
        if (StringUtils.isBlank(account)) {
            System.out.println("account is blank...");
            return "";
        }
        JSONObject obj = new JSONObject();
        obj.put("orgId", 1);
        HttpRequest post = HttpUtil.createPost(YmlConfig.keUrl + "/api/admin/rpc/dept/getTreeWithUser");
        post.body(obj.toJSONString());
        HttpCookie cookie = new HttpCookie(CookieKey.SESSION_ID, sessionId);
        post.cookie(cookie);
        String body = post.execute().body();
        JSONObject object = JSONObject.parseObject(body);
        if (object.getIntValue("code") != 200) {
            System.out.println("取得组织数所失败,code:" + object.getIntValue("code"));
            return "";
        }
        JSONObject data = object.getJSONArray("data").toJavaList(JSONObject.class).get(0);
        //总行账号List,先遍历看是否为总行账号
        List<JSONObject> userList = data.getJSONArray("userList").toJavaList(JSONObject.class);
        for (JSONObject user : userList) {
            String acc = user.getString("account");
            if (account.equals(acc)) {
                JSONObject bank = new JSONObject();
                bank.put("id", data.getIntValue("id"));
                bank.put("name", data.getString("name"));
                bank.put("account", account);
                return bank.getString("name");
            }
        }
        //分行及下级银行账号-遍历查询
        List<JSONObject> childrens = data.getJSONArray("children").toJavaList(JSONObject.class);
        for (JSONObject children : childrens) {
            Integer id = children.getInteger("id");
            String name = children.getString("name");
            UserDto userDto = getBankName(children, account);
            if (userDto != null) {
                JSONObject bank = new JSONObject();
                bank.put("id", id);
                bank.put("name", name);
                bank.put("account", account);
                return name;
            }
        }
        return "";
    }

    /**
     * 查询children下userList
     * account==参数账号的
     */
    public static UserDto getBankName(JSONObject children, String account) {
        if (children.getJSONArray("userList") != null) {
            List<JSONObject> subUsers = children.getJSONArray("userList").toJavaList(JSONObject.class);
            for (JSONObject user : subUsers) {
                if (account.equals(user.getString("account"))) {
                    return JSONObject.parseObject(user.toJSONString(), UserDto.class);
                }
            }
        }
        if (children.getJSONArray("children") != null) {
            List<JSONObject> subChilds = children.getJSONArray("children").toJavaList(JSONObject.class);
            for (JSONObject subChild : subChilds) {
                UserDto user = getBankName(subChild, account);
                if (user != null) {
                    return user;
                }
            }
        }
        return null;
    }

}
