package com.cmbc.infras.dto.event;

import com.cmbc.infras.constant.EventLevelEnum;
import com.cmbc.infras.constant.StateEnum;
import com.cmbc.infras.dto.Device;
import com.cmbc.infras.dto.monitor.Spot;
import com.cmbc.infras.dto.rpc.event.Event;
import com.cmbc.infras.util.DateTimeUtils;
import com.cmbc.infras.util.Utils;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;


/**
 * 告警信息-与EventInfo有重合
 * 主页面-运维页面显示的告警列表-字段信息比较全
 */
@Data
public class AlarmInfo implements Serializable {
    private static final long serialVersionUID = -7362864215314663645L;

    //银行名
    private String bankName;
    //设备名
    private String deviceName;
    //告警内容
    private String content;
    //事件发生时间
    private String eventTime;
    //受理时间
    private String acceptTime;
    //告警等级
    private int level;
    //等级名-显示用
    private String levelName;
    //告警状态
    private int state;
    //状态名-显示用
    private String stateName;
    //联系人
    private String contact;

    public AlarmInfo(){}

    public AlarmInfo(Spot spot, Event event) {
        this.bankName = spot.getBankName();
        this.deviceName = spot.getDeviceName();
        this.contact = spot.getContact();
        this.content = event.getContent();
        this.eventTime = DateTimeUtils.transToStr(event.getEvent_time());
        this.acceptTime = DateTimeUtils.transToStr(event.getAccept_time());
        this.level = event.getEvent_level();
        this.state = event.getIs_accept();
    }

    public String getLevelName() {
        return EventLevelEnum.getDesc(level);
    }

    public String getStateName() {
        return StateEnum.getDesc(state);
    }
}
