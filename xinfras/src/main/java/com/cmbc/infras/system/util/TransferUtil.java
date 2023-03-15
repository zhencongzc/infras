package com.cmbc.infras.system.util;

import com.cmbc.infras.constant.EventLevelEnum;
import com.cmbc.infras.constant.RecoveryEnum;
import com.cmbc.infras.constant.StateEnum;
import com.cmbc.infras.dto.rpc.event.AlarmEvent;
import com.cmbc.infras.dto.rpc.event.Event;
import com.cmbc.infras.util.DateTimeUtils;
import com.cmbc.infras.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class TransferUtil {

    /**
     * 将KE原始告警数据Event
     * 下划线形式变量
     * 转换成驼峰形式变量
     */
    public static AlarmEvent eventToAlarm(Event event) {
        AlarmEvent alarm = new AlarmEvent();
        alarm.setGuid(event.getGuid());
        alarm.setSysType(event.getSys_type());
        alarm.setResourceId(event.getResource_id());
        alarm.setEventTime(event.getEvent_time());
        alarm.setExpireTime(event.getExpire_time());
        alarm.setContent(event.getContent());
        alarm.setEventLevel(event.getEvent_level());
        alarm.setEventType(event.getEvent_type());
        alarm.setEventSnapshot(event.getEvent_snapshot());
        int i = event.getGuid().lastIndexOf("_");
        alarm.setThreshold(i == -1 ? "" : event.getGuid().substring(i + 1));
        alarm.setRecoverGuid(event.getRecover_guid());
        alarm.setRecoverSnapshot(event.getRecover_snapshot());
        alarm.setRecoverBy(event.getRecover_by());
        alarm.setIsRecover(event.getIs_recover());
        alarm.setRecoverTime(event.getRecover_time());
        alarm.setRecoverDescription(event.getRecover_description());
        alarm.setIsConfirm(event.getIs_confirm());
        alarm.setConfirmType(event.getConfirm_type());
        alarm.setConfirmTime(event.getConfirm_time());
        alarm.setConfirmBy(event.getConfirm_by());
        alarm.setConfirmDescription(event.getConfirm_description());
        alarm.setEventLocation(event.getEvent_location());
        alarm.setEventSource(event.getEvent_source());
        alarm.setEventSuggest(event.getEvent_suggest());
        alarm.setCepProcessed(event.getCep_processed());
        alarm.setDeviceType(event.getDevice_type());
        alarm.setMasked(event.getMasked());
        alarm.setIsAccept(event.getIs_accept());
        alarm.setAcceptTime(event.getAccept_time());
        alarm.setAcceptBy(event.getAccept_by());
        alarm.setAcceptDescription(event.getAccept_description());
        alarm.setEventSnapshotMapper(event.getEvent_snapshot_mapper());
        alarm.setRecoverSnapshotMapper(event.getRecover_snapshot_mapper());
        alarm.setUnit(event.getUnit());
        /**
         * 页面展示属性设置
         */
        alarm.setEventLevelName(EventLevelEnum.getDesc(alarm.getEventLevel()));
        alarm.setAcceptName(StateEnum.getDesc(alarm.getIsAccept()));
        alarm.setRecoverName(RecoveryEnum.getDesc(alarm.getIsRecover()));
        alarm.setEventTimeShow(DateTimeUtils.transToStr(alarm.getEventTime()));
        alarm.setAcceptTimeShow(DateTimeUtils.transToStr(alarm.getAcceptTime()));
        alarm.setRecoverTimeShow(DateTimeUtils.transToStr(alarm.getRecoverTime()));
        return alarm;
    }

    /**
     * List<Event>
     * to
     * List<AlarmEvent>
     */
    public static List<AlarmEvent> eventsToAlarms(List<Event> events) {
        List<AlarmEvent> alarms = new ArrayList<>();
        for (Event event : events) {
            AlarmEvent alarm = TransferUtil.eventToAlarm(event);
            alarms.add(alarm);
        }
        return alarms;
    }

}
