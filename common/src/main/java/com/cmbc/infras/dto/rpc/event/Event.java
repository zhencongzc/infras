package com.cmbc.infras.dto.rpc.event;

import lombok.Data;

@Data
public class Event {

    private String guid;    //a5e2b463-0714-11ec-9cda-00163e0301f0__70
    private String sys_type;//acquisition
    private String resource_id;//0_123_1_2_0
    private int event_time;//1630054580
    private int expire_time;
    private String content;//"湿度: 过高报警",
    /**
     * 1:紧急,2:严重,3:重要,4:次要,5:预警
     */
    private int event_level;//1
    private int event_type;//2
    private String event_snapshot;
    private String recover_guid;
    private String recover_snapshot;
    private String recover_by;
    private int is_recover;
    private int recover_time;
    private String recover_description;
    private int is_confirm;
    private int confirm_type;
    private int confirm_time;
    private String confirm_by;
    private String confirm_description;
    private String event_location;  //"project_root/0_113/0_114/0_121/0_123"
    private String event_source;    //"中国/北京/分行/机房/温湿度_01"
    private String event_suggest;
    private int cep_processed;
    private String device_type;     //"2.6.1.1.1.1.1"
    private int masked;
    private int is_accept;  //0:未处理,1:处理中,2:已处理
    private int accept_time;
    private String accept_by;
    private String accept_description;
    private String event_snapshot_mapper;
    private String recover_snapshot_mapper;
    private String unit;


}
