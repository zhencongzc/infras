package com.cmbc.infras.dto.auth;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 用户实体类
 */
@NoArgsConstructor
@Data
public class UserDto implements Serializable {

    /**
     * KE用户ID后台系统用
     * redis重置user时id丢失导致后台崩掉
     */
    private Long id;

    /**
     * 行方用户sn-唯一
     */
    private String usersn;

    /**
     * 用户账号
     * 账户改造-原account改为cmbcName
     * 因为KE登录存在account-冲突
     * 集中监控取账号用cmbcName
     */
    private String cmbcName;

    /**
     * 行方用户code
     */
    private String code;

    /**
     * 跳转的URL
     */
    private String redirectUrl;

    /**
     * token
     */
    private String token;

    /**
     * 所属银行id
     */
    private String bankId;

    //KE-account
    private String account;
    private Integer isShowMessage;
    private Integer isSuper;
    private String keWebAddress;
    private String uid;
    private Integer userLevel;
    private String theme;
    private String fileAddress;
    private Integer portalTab;
    private String email;
    private List<?> menuList;
    private OrgDTO org;
    private List<String> viewMenu;
    private List<?> tenantMenu;
    private List<String> portalMenu;
    private Integer isShowAlarm;
    private String sessionId;
    private List<RoleListDTO> roleList;
    private String extend;
    private List<DeptListDTO> deptList;
    private String phone;
    private String name;
    //对应KE用户account避免冲突加ke
    //private String keAccount;
    private String announceWebSocketAddr;

    @NoArgsConstructor
    @Data
    public static class OrgDTO {
        private String uid;
        private String name;
        private Integer id;
    }

    @NoArgsConstructor
    @Data
    public static class RoleListDTO {
        private String uid;
        private String name;
        private Integer id;
    }

    @NoArgsConstructor
    @Data
    public static class DeptListDTO {
        private String uid;
        private List<?> userList;
        private List<?> children;
        private String name;
        private Integer id;
    }
}
