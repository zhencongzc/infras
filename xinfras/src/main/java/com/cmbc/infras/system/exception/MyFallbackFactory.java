package com.cmbc.infras.system.exception;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.rpc.SpotValDto;
import com.cmbc.infras.dto.rpc.event.AlarmAcceptParam;
import com.cmbc.infras.dto.rpc.event.AlarmConfirmParam;
import com.cmbc.infras.dto.rpc.event.EventGroupParam;
import com.cmbc.infras.dto.rpc.event.EventParam;
import com.cmbc.infras.dto.rpc.spot.SpotDto;
import com.cmbc.infras.system.rpc.EventRpc;
import com.cmbc.infras.system.rpc.MobileOARpc;
import feign.hystrix.FallbackFactory;

/**
 * 处理Feign请求异常结果
 */

public class MyFallbackFactory implements FallbackFactory<EventRpc> {
    @Override
    public EventRpc create(Throwable throwable) {
        return new EventRpc() {
            @Override
            public String getEventLastCount(String cookie, EventGroupParam param) {
                return throwable.getMessage();
            }

            @Override
            public String getEventLastCount(String cookie, JSONObject param) {
                return throwable.getMessage();
            }

            @Override
            public String getEventLast(String cookie, EventParam param) {
                return throwable.getMessage();
            }

            @Override
            public String getEventCount(String cookie, EventParam param) {
                return throwable.getMessage();
            }

            @Override
            public String getEventCount(String cookie, JSONObject param) {
                return throwable.getMessage();
            }

            @Override
            public String getEvent(String cookie, EventParam param) {
                return throwable.getMessage();
            }

            @Override
            public String getEvent(String cookie, JSONObject param) {
                return throwable.getMessage();
            }

            @Override
            public String alarmAccept(String cookie, AlarmAcceptParam param) {
                return throwable.getMessage();
            }

            @Override
            public String alarmConfirm(String cookie, AlarmConfirmParam param) {
                return throwable.getMessage();
            }

            @Override
            public String spaceView(String cookie, String resource_id, String attribute_name, String attribute_value, String relation_code, String output_format) {
                return throwable.getMessage();
            }

            @Override
            public String spaceDeviceSpotList(String cookie, SpotDto spotDto) {
                return throwable.getMessage();
            }

            @Override
            public String getSpotLast(String cookie, SpotValDto param) {
                return throwable.getMessage();
            }

            @Override
            public String getRoleList(String cookie) {
                return throwable.getMessage();
            }

            @Override
            public String getEmployeeList(String cookie) {
                return throwable.getMessage();
            }

            @Override
            public String getRolesDetail(String cookie, Integer id) {
                return throwable.getMessage();
            }

            @Override
            public String sendAsset(String cookie, JSONObject param) {
                return throwable.getMessage();
            }

            @Override
            public String getOrganizationAndRole(JSONObject param) {
                return throwable.getMessage();
            }
        };
    }
}
