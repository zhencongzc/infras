package com.cmbc.infras.login.access.util;

import cn.com.sense.oauth.client.OAuthClient;
import cn.com.sense.oauth.client.config.OAuthClientConfig;
import com.cmbc.infras.util.YmlConfig;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4JServletContextListener;

@Order(5)
@DependsOn("ymlConfig")
@Configuration
public class CmbcOAuthConfig implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("infras.init ---start");
        OAuthClientConfig.getInstance().setConfig(
                YmlConfig.clientId.trim(),
                YmlConfig.secret.trim(),
                YmlConfig.authorizeUrl.trim(),
                YmlConfig.tokenUrl.trim(),
                YmlConfig.userInfoUrl.trim()
        );
        StringBuffer sb = new StringBuffer();
        sb.append("clientId:").append(YmlConfig.clientId)
                .append(",secret:").append(YmlConfig.secret)
                .append(",authorizeUrl:").append(YmlConfig.authorizeUrl)
                .append(",tokenUrl:").append(YmlConfig.tokenUrl)
                .append(",userInfoUrl:").append(YmlConfig.userInfoUrl);
        System.out.println("config:---------" + sb.toString());
        System.out.println("infras.init ---end");
    }

    @Bean
    public OAuthClient oAuthClient() {
        return new OAuthClient();
    }

    @Bean
    public SysOutOverSLF4JServletContextListener sysOutOverSLF4JServletContextListener() {
        return new SysOutOverSLF4JServletContextListener();
    }

}
