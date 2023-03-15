package com.cmbc.infras.system.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.Label;
import com.cmbc.infras.dto.LabelParam;
import com.cmbc.infras.dto.rpc.event.*;
import com.cmbc.infras.system.mapper.AlarmMapper;
import com.cmbc.infras.system.mapper.BankMapper;
import com.cmbc.infras.system.mapper.LabelMapper;
import com.cmbc.infras.system.rpc.EventRpc;
import com.cmbc.infras.system.service.AlarmService;
import com.cmbc.infras.system.service.EventService;
import com.cmbc.infras.system.util.TransferUtil;
import com.cmbc.infras.util.AlarmParamUtils;
import com.cmbc.infras.util.DateTimeUtils;
import com.cmbc.infras.util.EventParamUtils;
import com.cmbc.infras.util.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
public class AlarmServiceImpl implements AlarmService {

    @Resource
    private EventService eventService;

    @Resource
    private BankMapper bankMapper;
    @Resource
    private AlarmMapper alarmMapper;
    @Resource
    private LabelMapper labelMapper;

    @Resource
    private EventRpc eventRpc;

    @Override
    public BaseResult<List<AlarmEvent>> getAllAlarms(JSONObject json, boolean noAccount) {
        Label label = new Label();
        label.setId(json.getIntValue("id"));
        label.setEventLevel(json.getString("eventLevel"));
        label.setRecoverState(json.getString("recoverState"));//恢复状态
        label.setProcessState(json.getString("processState"));//处理状态
        //组装入参
        String account = noAccount ? "admin" : UserContext.getUserAccount();
        EventGroupParam param = EventParamUtils.creatLastAlarmParam(label);
        //KE实时告警最大数300(可设置),这里设成400
        param.getPage().setSize(InfrasConstant.ALARM_PAGE_SIZE);
        //如果label里有bankId则筛选银行
        String bankId = json.getString("bankId");
        if (bankId != null && !"0".equals(bankId)) {
            JSONObject j = new JSONObject();
            List<QueryCondition> or = new LinkedList<>();
            or.add(new QueryCondition("event_location", "like_any", new String[]{"%/" + bankId + "/%"}));
            or.add(new QueryCondition("event_location", "like_any", new String[]{"%/" + bankId}));
            j.put("or", or);
            param.getWhere().getAnd().get(0).setOr(or);
        }
        String resultStr = eventRpc.getEventLast(AlarmParamUtils.createCookie(account), param);
        List<Event> events = EventParamUtils.parseEventResult(resultStr);
        BaseResult<List<Event>> result = BaseResult.success(events, events.size());
        if (!result.isSuccess()) return BaseResult.fail(result.getMessage());
        List<AlarmEvent> alarms = TransferUtil.eventsToAlarms(events);
        return BaseResult.success(alarms, alarms.size());
    }

    @Override
    public BaseResult<List<AlarmEvent>> getAllAlarm(EventParam param) {
        BaseResult<List<Event>> result = eventService.getEventLast(param);
        if (!result.isSuccess()) {
            return BaseResult.fail(result.getMessage());
        }
        List<Event> events = result.getData();
        List<AlarmEvent> alarms = TransferUtil.eventsToAlarms(events);
        return BaseResult.success(alarms, alarms.size());
    }

    @Override
    public BaseResult<List<AlarmEvent>> getHistoryAlarm(JSONObject param) {
        String content = param.getString("content");//搜索文本
        Long beginTime = param.getLong("beginTime");
        Long endTime = param.getLong("endTime");
        JSONObject condition = param.getJSONObject("condition");//筛选条件
        //组装入参
        JSONObject param1 = new JSONObject();//入参
        List<JSONObject> outAnd = new LinkedList<>();
        List<QueryCondition> and = new ArrayList<>();//and条件
        //组装and
        and.add(new QueryCondition("masked", "eq", 0));
        and.add(new QueryCondition("cep_processed", "eq", 0));
        and.add(new QueryCondition("event_level", "in", condition.getJSONArray("event_level")));
        and.add(new QueryCondition("is_accept", "in", condition.getJSONArray("is_accept")));
        and.add(new QueryCondition("is_recover", "in", condition.getJSONArray("is_recover")));
        and.add(new QueryCondition("event_type", "in", condition.getJSONArray("event_type")));
        if (beginTime == null) {
            and.add(new QueryCondition("event_time", "gte", DateTimeUtils.getCurrentMonthDot()));
        } else {
            and.add(new QueryCondition("event_time", "gte", beginTime));
        }
        if (endTime == null) {
            and.add(new QueryCondition("event_time", "lte", DateTimeUtils.getCurrentTime()));
        } else {
            and.add(new QueryCondition("event_time", "lte", endTime));
        }
        JSONObject j = new JSONObject();
        j.put("and", and);
        outAnd.add(j);
        //组装or
        //位置
        List<QueryCondition> or1 = new ArrayList<>();
        JSONArray event_location = condition.getJSONArray("event_location");
        int size = event_location.size();
        if (size != 0) {
            String[] str1 = new String[size];
            String[] str2 = new String[size];
            for (int i = 0; i < size; i++) {
                String bankId = (String) event_location.get(i);
                str1[i] = "%/" + bankId + "/%";
                str2[i] = "%/" + bankId;
            }
            or1.add(new QueryCondition("event_location", "like_any", str1));
            or1.add(new QueryCondition("event_location", "like_any", str2));
        }
        JSONObject j1 = new JSONObject();
        j1.put("or", or1);
        outAnd.add(j1);
        //搜索文本项
        List<QueryCondition> or2 = new ArrayList<>();
        if (StringUtils.isNotBlank(content)) {
            StringBuffer sb = new StringBuffer("%").append(content).append("%");
            or2.add(new QueryCondition("content", "like", sb.toString()));
            or2.add(new QueryCondition("event_source", "like", sb.toString()));
        }
        JSONObject j2 = new JSONObject();
        j2.put("or", or2);
        outAnd.add(j2);
        //sorts
        List<SortCondition> sorts = new ArrayList() {{
            add(new SortCondition("event_time", "DESC"));
        }};
        //page
        PageCondition page;
        if (param.getIntValue("number") == 0 || param.getIntValue("size") == 0) {
            page = new PageCondition("1", 20);
        } else {
            page = new PageCondition(param.getIntValue("number") + "", param.getIntValue("size"));
        }
        //组装
        JSONObject where = new JSONObject();
        where.put("and", outAnd);
        param1.put("extra", true);
        param1.put("where", where);
        param1.put("sorts", sorts);
        param1.put("page", page);
        String account = UserContext.getUserAccount();
        BaseResult<List<Event>> result = eventService.getEvents(account, param1);
        if (!result.isSuccess()) return BaseResult.fail(result.getMessage());
        List<Event> events = result.getData();
        List<AlarmEvent> alarms = TransferUtil.eventsToAlarms(events);
        return new BaseResult<>(true, "", alarms, result.getTotal(), result.getPageSize(), result.getPageCount());
    }

    @Override
    public BaseResult<Boolean> alarmAccept(AlarmAcceptParam param) {
        Assert.notEmpty(param.getEvent_list(), "受理数据不能为空！");
        //Assert.hasLength(param.getAccept_by(), "受理人不能为空！");
        Assert.hasLength(param.getAccept_description(), "描述不能为空");
        String account = UserContext.getUserAccount();
        param.setAccept_by(account);//使用登录赋号替换-"系统管理员"
        BaseResult<Boolean> result = eventService.alarmAccept(param);
        return result;
    }

    @Override
    public BaseResult<Boolean> alarmConfirm(AlarmConfirmParam param, boolean noAccount) {
        Assert.notEmpty(param.getEvent_list(), "受理数据不能为空！");
        Assert.hasLength(param.getConfirm_description(), "描述不能为空");
        String account = noAccount ? "admin" : UserContext.getUserAccount();
        param.setConfirm_by(account);//使用登录赋号替换-"系统管理员"
        param.setConfirm_type(1);   //1:真实,2:测试,3:误告警
        BaseResult<Boolean> result = eventService.alarmConfirm(param);
        return result;
    }

    /**
     * 标签功能模块方法
     **/

    @Override
    public BaseResult<List<Label>> getLabels() {
        String account = UserContext.getUserAccount();
        List<Label> list = labelMapper.getLabels(account);
        return BaseResult.success(list, list.size());
    }

    @Override
    public BaseResult<Label> getLabel(Integer id) {
        Label label = labelMapper.getLabel(id);
        return BaseResult.success(label);
    }

    @Override
    public BaseResult<Boolean> labelCheck(Integer id) {
        String account = UserContext.getUserAccount();
        int i = labelMapper.unCheck(account);
        int j = labelMapper.setCheck(id);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Label> addLabel(LabelParam param) {

        String account = UserContext.getUserAccount();

        Assert.hasLength(param.getName(), "标签名称不能为空！");
        Assert.notEmpty(param.getEventLevel(), "请选择告警等级！");
        Assert.notEmpty(param.getStatus(), "请选择处理状态！");
        Assert.notEmpty(param.getRecoverStatus(), "请选择恢复状态！");

        Label label = transToLabel(param);
        label.setAccount(account);

        int i = labelMapper.addLabel(label);
        if (i == 1) {
            return BaseResult.success(label);
        }
        return BaseResult.fail("添加标签失败！");
    }

    @Override
    public BaseResult<Boolean> editLabel(LabelParam param) {

        Assert.hasLength(param.getName(), "标签名称不能为空！");
        Assert.notEmpty(param.getEventLevel(), "请选择告警等级！");
        Assert.notEmpty(param.getStatus(), "请选择处理状态！");
        Assert.notEmpty(param.getRecoverStatus(), "请选择恢复状态！");

        Label label = transToLabel(param);

        int i = labelMapper.editLabel(label);
        if (i == 1) {
            return BaseResult.success(true);
        }
        return BaseResult.fail("修改标签失败！");
    }

    @Override
    public BaseResult<Boolean> delLabel(Integer id) {
        int i = labelMapper.delLabel(id);
        if (i == 1) {
            return BaseResult.success(true);
        }
        return BaseResult.fail("删除标签失败！");
    }

    @Override
    public BaseResult<List<Bank>> getLocations() {
        List<Bank> banks = bankMapper.getBanksByLevel(1);
        //String sessionId = UserContext.getAuthToken();
        //List<Bank> banks = bankService.getBanksByLevel(1, sessionId);
        return BaseResult.success(banks, banks.size());
    }

    /**
     * LabelParam -> Label
     */
    private Label transToLabel(LabelParam param) {
        Label label = new Label();
        label.setId(param.getId());
        label.setLabelName(param.getName());
        label.setLocation(param.getEventLocation());

        label.setEventLevel(this.listToString(param.getEventLevel()));
        label.setProcessState(this.listToString(param.getStatus()));
        label.setRecoverState(this.listToString(param.getRecoverStatus()));

        return label;
    }

    private String listToString(List<String> list) {
        StringBuffer sb = new StringBuffer();
        for (String s : list) {
            sb.append(s).append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    @Override
    public List<JSONObject> findFastInput(int type) {
        String account = UserContext.getUserAccount();
        return alarmMapper.findFastInput(account, type);
    }

    @Override
    public int addFastInput(int type, String content) {
        String account = UserContext.getUserAccount();
        JSONObject j = new JSONObject();
        alarmMapper.addFastInput(account, type, content, j);
        return j.getIntValue("id");
    }

    @Override
    public void updateFastInput(int id, String content) {
        alarmMapper.updateFastInput(id, content);
    }

    @Override
    public void deleteFastInput(int id) {
        alarmMapper.deleteFastInput(id);
    }
}

