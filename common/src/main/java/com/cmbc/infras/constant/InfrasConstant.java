package com.cmbc.infras.constant;

public class InfrasConstant {

    public static final String KE_RPC_COOKIE = "DCIM_ACCOUNT=admin";//调用KE平台接口header Cookie参数
    public static final String HEAD_BANK_ID = "0";
    public static final int TIME_OUT = 60 * 60 * 24;
    public static final String AUTH_LOGIN = "simplemore_auth_login_";
    public static final String USER_BANK_KEY = "account_bank_name_";
    public static final String INFRAS_ALL_BANK_IDS = "infras_all_bank_ids";
    public static final String INFRAS_BANK_INFO = "infras_bank_info_"; //edis银行信息Key头部
    public static final String INFRAS_SUB_BANK_IDS_STR_ID = "infras_sub_bank_ids_str_id_";
    public static final String INFRAS_SUB_BANK_IDS_ID = "infras_sub_bank_ids_id_";
    public static final String INFRAS_SUB_BANKS_ID = "infras_sub_banks_id_";
    public static final String INFRAS_BANKS_LEVEL = "infras_banks_level_";
    public static final String APP_REQUEST_HEADER = "REQUEST_SOURCE";
    public static final String APP_SOURCE = "APP";
    public static final int ALARM_PAGE_SIZE = 400;//实时告警有最大数值设置,最大值300？设成300一次分页查全部,避免多次接口查询

}
