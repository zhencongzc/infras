package com.cmbc.infras.dto.rpc.event;

import lombok.Data;

import java.io.Serializable;

/**
 * com.cmbc.infras.dto.rpc.event.Event-驼峰形式
 */
@Data
public class AlarmEvent implements Serializable {
    private static final long serialVersionUID = 4418292458582973389L;

    private String guid;    //a5e2b463-0714-11ec-9cda-00163e0301f0__70
    private String sysType;//acquisition
    private String resourceId;//0_123_1_2_0
    private int eventTime;//1630054580
    private int expireTime;
    private String content;//"湿度: 过高报警",
    /**
     * 1:紧急,2:严重,3:重要,4:次要,5:预警
     */
    private int eventLevel;//1
    private int eventType;//2
    private String eventTypeName;//告警类型
    private String eventSnapshot;//当前值
    private String threshold;//阈值
    private String recoverGuid;
    private String recoverSnapshot;
    private String recoverBy;
    private int isRecover;
    private int recoverTime;
    private String recoverDescription;
    private int isConfirm;
    private int confirmType;
    private int confirmTime;
    private String confirmBy;
    private String confirmDescription;
    private String eventLocation;  //"project_root/0_113/0_114/0_121/0_123"
    private String eventSource;    //"中国/北京/分行/机房/温湿度_01"
    private String eventSuggest;
    private int cepProcessed;
    private String deviceType;     //"2.6.1.1.1.1.1"
    private int masked;
    private int isAccept;  //0:未处理,1:处理中,2:已处理
    private int acceptTime;
    private String acceptBy;
    private String acceptDescription;
    private String eventSnapshotMapper;
    private String recoverSnapshotMapper;
    private String unit;

    /**
     * 页面展示用
     * 状态名称,时间格式化
     */
    //eventLevel
    private String eventLevelName;
    //isAccept
    private String acceptName;
    //isRecover
    private String recoverName;
    //eventTime
    private String eventTimeShow;
    //acceptTime
    private String acceptTimeShow;
    //recoverTime
    private String recoverTimeShow;

    //告警显示-联系人-add-1119
    private String contact;

    //前端移动端三页面需要bankId,bankName
    private String bankId;
    private String bankName;

}
