package com.cmbc.infras.health.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.health.contant.AlarmLevelEnum;
import com.cmbc.infras.health.contant.SpotTypeEnum;
import com.cmbc.infras.health.dto.FormRequestParam;
import com.cmbc.infras.health.mapper.AssessMapper;
import com.cmbc.infras.health.mapper.ModelMapper;
import com.cmbc.infras.health.rpc.ProcessEngineRpc;
import com.cmbc.infras.health.rpc.RpcUtil;
import com.cmbc.infras.health.service.ModelService;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.health.thread.AnalysisThread;
import com.cmbc.infras.health.thread.CreateReport;
import com.cmbc.infras.health.thread.MonitorThread;
import com.cmbc.infras.health.util.CommonUtils;
import com.cmbc.infras.health.redis.DataRedisUtil;
import com.cmbc.infras.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.cmbc.infras.health.controller.ModelController.*;

@Service
@Slf4j
public class ModelServiceImpl implements ModelService {

    @Value("${health.picture}")
    private String pictureUri;

    @Resource
    private ModelService modelService;

    @Resource
    private ModelMapper modelMapper;
    @Resource
    private AssessMapper assessMapper;
    @Resource
    private ProcessEngineRpc processEngineRpc;

    @Override
    public List<JSONObject> quickFind(String word, int start, int end) {
        List<JSONObject> list = modelMapper.quickFind(word, start, end);
        for (JSONObject l : list) {
            //封装模板启动时间
            String time = DataRedisUtil.getStringFromRedis(l.getString("modelId") + "_startTime");
            String startTime;
            if (time == null) {
                startTime = "";
            } else {
                Long t = Long.valueOf(time + "000");
                Date date = new Date(t);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                startTime = sdf.format(date);
            }
            l.put("startTime", startTime);
        }
        return list;
    }

    @Override
    public int getModelTotal(String word) {
        return modelMapper.getModelTotal(word);
    }

    @Override
    @Transactional(isolation = Isolation.DEFAULT)
    public void startModel(String modelId, int startModel) throws Exception {
        if (startModel == 1) {
            //启动模板时，删除次年成绩，新建次年成绩
            int year = Integer.parseInt(Utils.getCurrentTime("yyyy")) + 1;
            modelMapper.deleteScoreByYear(modelId, year);
            List<JSONObject> dimen = modelMapper.findDimension(modelId);
            if (dimen.size() != 0) {
                List<JSONObject> bank = modelMapper.findBank(modelId);
                for (JSONObject b : bank) {
                    String state = "待完善";
                    modelMapper.addScore(modelId, dimen, b.getString("name"), state, year);
                }
            }
            //运行监控：开启定时任务
            JSONObject model = modelMapper.findModel(modelId);
            int cycleValue = model.getIntValue("cycleValue");
            String cycleUnit = model.getString("cycleUnit");
            long cycle = cycleValue * CommonUtils.change(cycleUnit);
            MonitorThread monitorThread = new MonitorThread(modelId, modelMapper, assessMapper, processEngineRpc);
            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            service.scheduleAtFixedRate(monitorThread, 0, cycle, TimeUnit.SECONDS);
            mapMonitorService.put(modelId, service);
            log.info("mapMonitorService运行的线程为:" + mapMonitorService.keySet().toString());
            //存储模板启动时间到redis
            long startTime = System.currentTimeMillis() / 1000;
            DataRedisUtil.addStringToRedis(modelId + "_startTime", startTime + "", null);
            //统计分析：开启定时任务
            AnalysisThread analysisThread = new AnalysisThread(modelId, modelMapper, assessMapper, processEngineRpc);
            ScheduledExecutorService service1 = Executors.newSingleThreadScheduledExecutor();
            service1.scheduleAtFixedRate(analysisThread, 0, cycle, TimeUnit.SECONDS);
            mapAnalysisService.put(modelId, service1);
            log.info("mapAnalysisService运行的线程为:" + mapAnalysisService.keySet().toString());
        } else {
            //关闭运行监控任务
            ScheduledExecutorService service = mapMonitorService.get(modelId);
            if (service != null) service.shutdown();
            mapMonitorService.remove(modelId);
            log.info("mapMonitorService运行的线程为:" + mapMonitorService.keySet().toString());
            //删除模板启动时间
            DataRedisUtil.delete(modelId + "_startTime");
            //关闭统计分析任务
            ScheduledExecutorService service1 = mapAnalysisService.get(modelId);
            if (service1 != null) service1.shutdown();
            mapAnalysisService.remove(modelId);
            log.info("mapAnalysisService运行的线程为:" + mapAnalysisService.keySet().toString());
        }
        modelMapper.startModel(modelId, startModel);
    }

    @Override
    public void startScore(String modelId, int startScore) {
        if (startScore == 1) {
            //查询报告生成周期，开启定时任务
            JSONObject model = modelMapper.findModel(modelId);
            int cycleValue = model.getIntValue("cycleValue");
            String cycleUnit = model.getString("cycleUnit");
            long cycle = cycleValue * CommonUtils.change(cycleUnit);
            CreateReport runnable = new CreateReport(modelId, modelMapper, assessMapper, false);
            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
//            service.scheduleAtFixedRate(runnable, 0, 30, TimeUnit.SECONDS);//测试用
            service.scheduleAtFixedRate(runnable, cycle, cycle, TimeUnit.SECONDS);
            mapReportService.put(modelId, service);
            log.info("mapReportService运行的线程为:" + mapReportService.keySet().toString());
        } else {
            ScheduledExecutorService service = mapReportService.get(modelId);
            if (service != null) service.shutdown();
            mapReportService.remove(modelId);
            log.info("mapReportService运行的线程为:" + mapReportService.keySet().toString());
        }
        modelMapper.startScore(modelId, startScore);
    }

    @Override
    public List<JSONObject> findType() {
        return modelMapper.findType();
    }

//    @Override
//    public BaseResult<String> uploadPicture(MultipartFile file) {
//        if (!FileUtil.isPicture(file)) return BaseResult.fail("上传的文件非图片");
//        String date = Utils.getCurrentTime("yyyyMMdd");
//        String filePath = pictureUri + "/" + date;
//        boolean b = FileUtil.uploadDocument(file, filePath);
//        if (b) return BaseResult.success(filePath + "/" + file.getOriginalFilename());
//        return BaseResult.fail("上传失败");
//    }

    @Override
    public BaseResult<JSONObject> findOrganizationAndRole() {
        JSONObject res = new JSONObject();
        JSONObject param = new JSONObject();
        param.put("orgId", 1);
        String result = processEngineRpc.getOrganizationAndRole(param);
        JSONObject json = JSONObject.parseObject(result);
        if (!"200".equals(json.getString("code")))
            return BaseResult.fail("查询流程引擎失败，接口：/api/admin/rpc/dept/getTreeWithUser，返回信息：" + json.toJSONString());
        List<JSONObject> data = json.getJSONArray("data").toJavaList(JSONObject.class);
        //参与的组织
        JSONObject organization = data.get(0);
        List<JSONObject> children = organization.getJSONArray("children").toJavaList(JSONObject.class);
        for (JSONObject child : children) {
            child.remove("children");
        }
        for (JSONObject child : children) {
            //organization去掉机房管理中心
            if (child.getIntValue("id") == 17) {
                children.remove(child);
                break;
            }
        }
        organization.put("children", children);
        res.put("organization", organization);
        //角色信息
        JSONObject role = JSONObject.parseObject(result).getJSONArray("data").toJavaList(JSONObject.class).get(0);
        res.put("commitRole", role);
        res.put("auditRole", role);
        return BaseResult.success(res);
    }

    @Override
    public BaseResult<List<JSONObject>> findResource() {
        String cookie = RpcUtil.getCookie();
        String result = processEngineRpc.getFormList(cookie);
        JSONObject json = JSONObject.parseObject(result);
        if (!"200".equals(json.getString("status")))
            return BaseResult.fail("查询流程引擎失败，接口：/api/flow/api/v1/bfm/config/data/module/list，返回信息：" + json.toJSONString());
        List<JSONObject> validForm = new LinkedList<>();
        List<JSONObject> data = JSONArray.parseArray(json.getString("data")).toJavaList(JSONObject.class);
        for (JSONObject d : data) {
            if ("Published".equals(d.getString("status")) && "enable".equals(d.getString("enableStatus"))
                    && "processForm".equals(d.getString("moduleType"))) {
                JSONObject j = new JSONObject();
                j.put("name", d.getString("name"));
                String moduleKey = d.getString("moduleKey");
                if (moduleKey.contains("_")) moduleKey = moduleKey.substring(0, moduleKey.lastIndexOf("_"));
                j.put("resourceId", moduleKey);
                validForm.add(j);
            }
        }
        return BaseResult.success(validForm);
    }

    @Override
    public BaseResult<List<String>> findFormState(String resourceId) {
        String cookie = RpcUtil.getCookie();
        String result = processEngineRpc.getFormState(cookie, resourceId, "status");
        JSONObject json = JSONObject.parseObject(result);
        if (!"200".equals(json.getString("status")))
            return BaseResult.fail("查询流程引擎失败，接口：/api/flow/api/v1/bfm/dictionary/getDictionary，返回信息：" + json.toJSONString());
        List<JSONObject> state = new LinkedList<>();
        List<JSONObject> data = JSONArray.parseArray(json.getString("data")).toJavaList(JSONObject.class);
        for (JSONObject d : data) {
            JSONObject j = new JSONObject();
            j.put("text", d.getString("label"));
            j.put("value", d.getString("value"));
            state.add(j);
        }
        return BaseResult.success(state);
    }

    @Override
    public BaseResult<List<JSONObject>> findMonitorList(JSONObject jsonObject) {
        List<JSONObject> organization = jsonObject.getJSONArray("organization").toJavaList(JSONObject.class);
        for (JSONObject object : organization) {
            //组装参数
            String id = object.getString("id");
            FormRequestParam param = new FormRequestParam(id);
            String cookie = RpcUtil.getCookie();
            String result = processEngineRpc.getInfrastructureData(cookie, param);
            JSONObject json = JSONObject.parseObject(result);
            if (!"200".equals(json.getString("status")))
                return BaseResult.fail("查询流程引擎失败，接口：/api/flow/api/v1/bfm/instances/model/list，返回信息：" + json.toJSONString());
            List<JSONObject> data = json.getJSONObject("data").getJSONArray("instancesData").toJavaList(JSONObject.class);
            //封装包含的测点类型和测点id
            List<JSONObject> spotType = new LinkedList<>();
            List<JSONObject> temperatureSpot = new LinkedList<>();
            List<JSONObject> humiditySpot = new LinkedList<>();
            List<JSONObject> pueSpot = new LinkedList<>();
            List<JSONObject> upsSpot = new LinkedList<>();
            for (JSONObject j : data) {
                JSONArray field_xxx_zgxlfpa = j.getJSONArray("Field_xxx_zgxlfpa");
                if (null != field_xxx_zgxlfpa) {
                    //测点列表
                    List<JSONObject> spots = field_xxx_zgxlfpa.toJavaList(JSONObject.class);
                    spots.forEach((a) -> {
                        String type = a.getString("spot_name");
                        if (SpotTypeEnum.belongSpotType(type)) {
                            JSONObject j1 = new JSONObject();
                            j1.put("resource_id", a.getString("spot_id"));
                            if (type.equals(SpotTypeEnum.TEMPERATURE.getDesc())) temperatureSpot.add(j1);
                            if (type.equals(SpotTypeEnum.HUMIDITY.getDesc())) humiditySpot.add(j1);
                            if (type.equals(SpotTypeEnum.PUE.getDesc())) pueSpot.add(j1);
                            if (type.equals(SpotTypeEnum.UPS.getDesc())) upsSpot.add(j1);
                        }
                    });
                }
            }
            if (temperatureSpot.size() != 0) {
                JSONObject temperature = new JSONObject();
                temperature.put("name", SpotTypeEnum.TEMPERATURE.getDesc());
                temperature.put("spotTypeId", SpotTypeEnum.TEMPERATURE.getId());
                temperature.put("resources", temperatureSpot);
                spotType.add(temperature);
            }
            if (humiditySpot.size() != 0) {
                JSONObject humidity = new JSONObject();
                humidity.put("name", SpotTypeEnum.HUMIDITY.getDesc());
                humidity.put("spotTypeId", SpotTypeEnum.HUMIDITY.getId());
                humidity.put("resources", humiditySpot);
                spotType.add(humidity);
            }
            if (pueSpot.size() != 0) {
                JSONObject pue = new JSONObject();
                pue.put("name", SpotTypeEnum.PUE.getDesc());
                pue.put("spotTypeId", SpotTypeEnum.PUE.getId());
                pue.put("resources", pueSpot);
                spotType.add(pue);
            }
            if (upsSpot.size() != 0) {
                JSONObject ups = new JSONObject();
                ups.put("name", SpotTypeEnum.UPS.getDesc());
                ups.put("spotTypeId", SpotTypeEnum.UPS.getId());
                ups.put("resources", upsSpot);
                spotType.add(ups);
            }
            object.put("spotType", spotType);
        }
        JSONObject res = new JSONObject();
        res.put("organization", organization);
        return BaseResult.success(res);
    }

    @Override
    public BaseResult<List<JSONObject>> findAnalysisList(JSONObject param) {
        //告警处理列表
        List<JSONObject> alarmList = new LinkedList<>();
        AlarmLevelEnum[] values = AlarmLevelEnum.values();
        for (AlarmLevelEnum value : values) {
            JSONObject j = new JSONObject();
            j.put("eventLevel", value.getCode());
            j.put("eventName", value.getDesc());
            alarmList.add(j);
        }
        List<JSONObject> result = new LinkedList<>();
        JSONObject alarm = new JSONObject();
        alarm.put("name", "告警处理");
        alarm.put("list", alarmList);
        result.add(alarm);
        return BaseResult.success(result);
    }

    @Override
    @Transactional(isolation = Isolation.DEFAULT)
    public void addModel(JSONObject param, String modelId, String createTime) throws Exception {
        int cycleValue = param.getIntValue("cycleValue");//评分周期
        String cycleUnit = param.getString("cycleUnit");//评分周期单位
        String title = param.getString("title");//标题
        String iconName = param.getString("iconName");//图标
        String borderColor = param.getString("borderColor");//边框颜色
        String iconColor = param.getString("iconColor");//图标颜色
        String backgroundColor = param.getString("backgroundColor");//背景颜色
        String cycleStart = param.getString("cycleStart");//填写周期（起）
        String cycleEnd = param.getString("cycleEnd");//填写周期（止）
        String description = param.getString("description");//描述
        String createPerson = param.getString("userName");//创建人
        String gradation = param.getJSONArray("gradation").toString();//等级划分
        if (createTime == null) createTime = Utils.getCurrentTime("yyyy-MM-dd HH:mm:ss");
        List<JSONObject> organization = param.getJSONArray("organization").toJavaList(JSONObject.class);
        List<JSONObject> commitRole = param.getJSONArray("commitRole").toJavaList(JSONObject.class);
        List<JSONObject> auditRole = param.getJSONArray("auditRole").toJavaList(JSONObject.class);
        //新增银行数据
        String organizationData = JSONObject.toJSONString(organization);
        modelMapper.addBank(modelId, organization, organizationData);
        //新增提交人
        List<JSONObject> listCommit = new LinkedList<>();
        String commitData = JSONObject.toJSONString(commitRole);
        for (JSONObject role : commitRole) {
            if (role.getIntValue("ifLeaf") != 1) {
                handleRole(role, listCommit, commitData);
            } else {
                listCommit.add(role);
            }
        }
        modelMapper.addCommitRole(modelId, listCommit, commitData);
        //新增审核人
        List<JSONObject> listAudit = new LinkedList<>();
        String auditData = JSONObject.toJSONString(auditRole);
        for (JSONObject role : auditRole) {
            if (role.getIntValue("ifLeaf") != 1) {
                handleRole(role, listAudit, commitData);
            } else {
                listAudit.add(role);
            }
        }
        modelMapper.addAuditRole(modelId, listAudit, auditData);
        //新增模板
        modelMapper.addModel(modelId, cycleValue, cycleUnit, title, iconName, borderColor, iconColor, backgroundColor, cycleStart, cycleEnd, description,
                gradation, createPerson, createTime);
        /**
         * 2022/11/2改动，新增模板不再添加维度和成绩信息，统一在编辑模板时创建
         */
//        //新增维度和单选得分项
//        List<JSONObject> dimension = param.getJSONArray("dimension").toJavaList(JSONObject.class);//维度
//        List<JSONObject> optionList = new LinkedList<>();//得分项列表
//        List<JSONObject> deductList = new LinkedList<>();//扣分项列表
//        List<JSONObject> deductDimensionList = new LinkedList<>();//扣分项结构列表
//        List<JSONObject> monitorList = new LinkedList<>();//运行监控列表
//        List<JSONObject> analysisList = new LinkedList<>();//统计分析列表
//        //新增非子叶维度结构，并分别处理不同评分方式
//        for (JSONObject jo : dimension) {
//            modelMapper.addDimension(modelId, 0, 0, jo);
//            int dimensionId = jo.getIntValue("id");
//            if (jo.getIntValue("ifLeaf") == 0)
//                handleDimension(jo, modelId, dimensionId, dimensionId, optionList, deductList, deductDimensionList, monitorList, analysisList);
//            if (jo.getIntValue("ifLeaf") == 1)
//                handleLeaf(jo, dimensionId, optionList, deductList, deductDimensionList, monitorList, analysisList);
//        }
//        //新增单选、累计扣分维度结构、统计分析结构
//        for (JSONObject bank : organization) {
//            String bankName = bank.getString("name");
//            if (0 != optionList.size()) modelMapper.addOption(bankName, optionList);
//            if (0 != deductDimensionList.size()) modelMapper.addDeductDimension(bankName, deductDimensionList);
//            if (0 != analysisList.size()) modelMapper.addAnalysis(bankName, analysisList);
//        }
//        //新增累计扣分扣分项
//        if (0 != deductList.size()) modelMapper.addDeduct(deductList);
//        //新增运行监控结构
//        if (0 != monitorList.size()) modelMapper.addMonitor(monitorList);
//        //创建成绩
//        int year = Integer.parseInt(Utils.getCurrentTime("yyyy"));
//        int nextYear = year + 1;
//        //查询模板维度、参与的银行，创建当年和次年成绩
//        List<JSONObject> dimen = modelMapper.findDimension(modelId);
//        if (dimen.size() != 0) {
//            List<JSONObject> bank = modelMapper.findBank(modelId);
//            for (JSONObject b : bank) {
//                String state = "待完善";
//                modelMapper.addScore(modelId, dimen, b.getString("name"), state, year);
//                modelMapper.addScore(modelId, dimen, b.getString("name"), state, nextYear);
//            }
//        }
    }

    private void handleRole(JSONObject json, List<JSONObject> listRole, String commitData) {
        List<JSONObject> commitRole = json.getJSONArray("sub").toJavaList(JSONObject.class);
        for (JSONObject role : commitRole) {
            if (role.getIntValue("ifLeaf") != 1) {
                handleRole(role, listRole, commitData);
            } else {
                listRole.add(role);
            }
        }
    }

//    /**
//     * 递归处理非子叶维度
//     */
//    private void handleDimension(JSONObject jsonObject, String modelId, int rootId, int parentId, List<JSONObject> optionList,
//                                 List<JSONObject> deductList, List<JSONObject> deductDimensionList, List<JSONObject> monitorList,
//                                 List<JSONObject> analysisList) throws Exception {
//        List<JSONObject> subDimension = jsonObject.getJSONArray("subDimension").toJavaList(JSONObject.class);//下级维度
//        for (JSONObject jo : subDimension) {
//            modelMapper.addDimension(modelId, rootId, parentId, jo);
//            int dimensionId = jo.getIntValue("id");
//            if (jo.getIntValue("ifLeaf") == 0)
//                handleDimension(jo, modelId, rootId, dimensionId, optionList, deductList, deductDimensionList, monitorList, analysisList);
//            if (jo.getIntValue("ifLeaf") == 1)
//                handleLeaf(jo, dimensionId, optionList, deductList, deductDimensionList, monitorList, analysisList);
//        }
//    }

//    /**
//     * 处理子叶维度
//     */
//    private void handleLeaf(JSONObject jsonObject, int dimensionId, List<JSONObject> optionList, List<JSONObject> deductList,
//                            List<JSONObject> deductDimensionList, List<JSONObject> monitorList, List<JSONObject> analysisList) {
//        //单项选择，获取得分项
//        if ("single".equals(jsonObject.getString("type"))) {
//            List<JSONObject> option = jsonObject.getJSONArray("option").toJavaList(JSONObject.class);
//            for (JSONObject object : option) {
//                object.put("dimensionId", dimensionId);
//            }
//            optionList.addAll(option);
//        }
//        //累计扣分，获取扣分项
//        if ("deduct".equals(jsonObject.getString("type"))) {
//            //根据数据源、起止时间、状态获取扣分项列表
//            JSONObject param = new JSONObject();//请求参数
//            JSONObject source = jsonObject.getJSONObject("source");
//            List<JSONObject> chooseState = jsonObject.getJSONArray("chooseState").toJavaList(JSONObject.class);
//            String dateStart = jsonObject.getString("dateStart");
//            String dateEnd = jsonObject.getString("dateEnd");
//            String year = Utils.getCurrentTime("yyyy");
//            //保存扣分维度结构
//            JSONObject deductDimension = new JSONObject();
//            deductDimension.put("dimensionId", jsonObject.getIntValue("id"));
//            deductDimension.put("source", source.toJSONString());
//            deductDimension.put("dateStart", dateStart);
//            deductDimension.put("dateEnd", dateEnd);
//            deductDimension.put("chooseState", jsonObject.getJSONArray("chooseState").toJSONString());
//            deductDimension.put("chooseCondition", jsonObject.getJSONArray("chooseCondition").toJSONString());
//            deductDimension.put("statisticsRule", jsonObject.getString("statisticsRule"));
//            String calculationRule = jsonObject.getString("calculationRule");
//            if (calculationRule.equals("百分比")) calculationRule += "#" + jsonObject.getJSONArray("condition");
//            deductDimension.put("calculationRule", calculationRule);
//            deductDimension.put("rule", jsonObject.getJSONArray("rule").toJSONString());
//            deductDimensionList.add(deductDimension);
//            //组装参数
//            param.put("configDataId", source.getString("resourceId"));
//            JSONObject query = new JSONObject();
//            JSONObject search = new JSONObject();
//            JSONObject status = new JSONObject();//筛选的状态
//            StringBuilder value = new StringBuilder();
//            for (JSONObject object : chooseState) {
//                value.append(object.getString("value")).append(",");
//            }
//            String substring = value.substring(0, value.lastIndexOf(","));
//            status.put("value", substring);
//            search.put("status", status);
//            JSONObject Field_xxx_create_time = new JSONObject();//筛选的起止时间
//            Field_xxx_create_time.put("startTime", year + "-" + dateStart);
//            Field_xxx_create_time.put("endTime", year + "-" + dateEnd);
//            search.put("Field_xxx_create_time", Field_xxx_create_time);
//            query.put("search", search);
//            param.put("query", query);
//            String cookie = RpcUtil.getCookie();
//            String result = processEngineRpc.getFormData(cookie, param);
//            JSONObject json = JSONObject.parseObject(result);
//            if ("200".equals(json.getString("status"))) {
//                List<JSONObject> data = json.getJSONObject("data").getJSONArray("instancesData").toJavaList(JSONObject.class);
//                //有筛选条件进行筛选
//                JSONArray chooseCondition = jsonObject.getJSONArray("chooseCondition");
//                if (!chooseCondition.isEmpty()) {
//                    List<JSONObject> condition = chooseCondition.toJavaList(JSONObject.class);
//                    HashMap<String, String> map = new HashMap<>();
//                    condition.forEach(j -> map.put(j.getString("text"), j.getString("value")));
//                    Set<String> keySet = map.keySet();
//                    Iterator<JSONObject> iterator = data.iterator();
//                    while (iterator.hasNext()) {
//                        JSONObject next = iterator.next();
//                        //有一项不符合筛选项则删除
//                        for (String a : keySet) {
//                            String string = next.getString(a);
//                            if (string == null || !string.equals(map.get(a))) {
//                                iterator.remove();
//                                break;
//                            }
//                        }
//                    }
//                }
//                //组装数据
//                List<JSONObject> option = new LinkedList<>();
//                for (JSONObject d : data) {
//                    JSONObject jo = new JSONObject();
//                    jo.put("bankName", d.getString("Field_xxx_create_dept") == null ? "" : d.getString("Field_xxx_create_dept"));
//                    jo.put("bill", d.getString("Field_xxx_title"));
//                    jo.put("billId", d.getString("id"));
//                    jo.put("createTime", d.getString("Field_xxx_create_time").substring(0, 10));
//                    jo.put("state", d.getString("status"));
//                    option.add(jo);
//                }
//                for (JSONObject object : option) {
//                    jsonObject.put("source", jsonObject.getJSONObject("source").toJSONString());
//                    jsonObject.put("chooseState", jsonObject.getJSONArray("chooseState").toJSONString());
//                    jsonObject.put("rule", jsonObject.getJSONArray("rule").toJSONString());
//                    object.putAll(jsonObject);
//                    object.put("dimensionId", dimensionId);
//                }
//                deductList.addAll(option);
//            } else {
//                log.warn("查询流程引擎失败，接口：/api/flow/api/v1/bfm/instances/model/list，返回信息：" + json.toJSONString());
//            }
//        }
//        //运行监控，获取结构
//        if ("monitor".equals(jsonObject.getString("type"))) {
//            JSONArray array = jsonObject.getJSONArray("bankSpot");
//            if (array != null) {
//                List<JSONObject> bankSpot = array.toJavaList(JSONObject.class);
//                for (JSONObject bank : bankSpot) {
//                    JSONObject temp = new JSONObject();
//                    temp.putAll(jsonObject);
//                    temp.put("bankName", bank.getString("name"));
//                    temp.put("bankId", bank.getIntValue("id"));
//                    temp.put("dimensionId", dimensionId);
//                    temp.put("resources", bank.getJSONArray("resources").toJSONString());
//                    temp.put("rule", jsonObject.getJSONArray("rule").toJSONString());
//                    monitorList.add(temp);
//                }
//            }
//        }
//        //统计分析
//        if ("analysis".equals(jsonObject.getString("type"))) {
//            if (jsonObject.getIntValue("timeLimit") == 1) {
//                jsonObject.put("rule", jsonObject.getJSONArray("rule").toJSONString());
//            } else {
//                jsonObject.put("rule", "[]");
//            }
//            jsonObject.put("deduct", jsonObject.getDoubleValue("deduct"));
//            jsonObject.put("dimensionId", dimensionId);
//            analysisList.add(jsonObject);
//        }
//    }


    @Override
    public JSONObject findModel(String modelId) {
        JSONObject model = modelMapper.findModel(modelId);
        //查询参与的组织、提交的角色、审核的角色
        String organization = modelMapper.findBankData(modelId);
        String commitRole = modelMapper.findCommitRole(modelId);
        String auditRole = modelMapper.findAuditRole(modelId);
        model.put("organization", JSONArray.parseArray(organization));
        model.put("commitRole", JSONArray.parseArray(commitRole));
        model.put("auditRole", JSONArray.parseArray(auditRole));
        //查询维度、组装维度、单项选择得分项
        List<JSONObject> listDimension = modelMapper.findDimension(modelId);
        List<JSONObject> roots = new LinkedList<>();//存放根维度
        for (JSONObject jo : listDimension) {
            if (jo.getIntValue("level") == 0) roots.add(jo);
        }
        for (JSONObject root : roots) {
            if (root.getIntValue("ifLeaf") == 0) packageDimension(root);
            if (root.getIntValue("ifLeaf") == 1) packageLeaf(root);
        }
        model.put("dimension", roots);
        //等级划分
        model.put("gradation", model.getJSONArray("gradation"));
        return model;
    }

    /**
     * 递归封装非子叶维度
     */
    private void packageDimension(JSONObject root) {
        //查询子维度，递归封装
        int rootId = root.getIntValue("id");
        List<JSONObject> subDimension = modelMapper.findDimensionByParentId(rootId);
        for (JSONObject s : subDimension) {
            if (s.getIntValue("ifLeaf") == 0) packageDimension(s);
            if (s.getIntValue("ifLeaf") == 1) packageLeaf(s);
        }
        root.put("subDimension", subDimension);
    }

    /**
     * 封装子叶维度
     */
    private void packageLeaf(JSONObject jo) {
        //单项选择，封装得分项
        if ("single".equals(jo.getString("type"))) {
            List<JSONObject> option = modelMapper.findOption(jo.getIntValue("id"));
            jo.put("option", option);
        }
        //累计扣分，封装扣分项维度
        if ("deduct".equals(jo.getString("type"))) {
            JSONObject leaf = modelMapper.findDeductDimension(jo.getIntValue("id"));
            if (leaf != null) {
                leaf.put("rule", leaf.getJSONArray("rule"));
                leaf.put("source", leaf.getJSONObject("source"));
                leaf.put("chooseState", leaf.getJSONArray("chooseState"));
                leaf.put("chooseCondition", leaf.getJSONArray("chooseCondition"));
                String[] calculationRule = leaf.getString("calculationRule").split("#");
                if (calculationRule[0].equals("百分比")) {
                    leaf.put("calculationRule", calculationRule[0]);
                    leaf.put("condition", JSONArray.parseArray(calculationRule[1]));
                }
                jo.putAll(leaf);
            }
        }
        //运行监控，封装结构
        if ("monitor".equals(jo.getString("type"))) {
            List<JSONObject> list = modelMapper.findMonitor(jo.getIntValue("id"));
            if (list.size() != 0) {
                List<JSONObject> bankSpot = new LinkedList<>();
                for (JSONObject j : list) {
                    JSONObject j1 = new JSONObject();
                    j1.put("name", j.getString("bankName"));
                    j1.put("id", j.getIntValue("bankId"));
                    j1.put("resources", j.getJSONArray("resources"));
                    bankSpot.add(j1);
                }
                JSONObject leaf = list.get(0);
                leaf.remove("bankName");
                leaf.remove("bankId");
                leaf.remove("resources");
                leaf.put("bankSpot", bankSpot);
                String spotType = leaf.getString("spotType");
                leaf.put("spotType", spotType);
                leaf.put("spotTypeId", SpotTypeEnum.getId(spotType));
                leaf.put("rule", leaf.getJSONArray("rule"));
                jo.putAll(leaf);
            }
        }
        //运行监控，封装结构
        if ("analysis".equals(jo.getString("type"))) {
            JSONObject leaf = modelMapper.findAnalysis(jo.getIntValue("id"));
            if (leaf != null) {
                leaf.put("rule", leaf.getJSONArray("rule"));
                jo.putAll(leaf);
            }
        }
    }

    @Override
    @Transactional(isolation = Isolation.DEFAULT)
    public void deleteModel(String modelId) {
        //删除模板、参与的组织、提交的角色、审核的角色、成绩
        modelMapper.deleteModel(modelId);
        modelMapper.deleteBank(modelId);
        modelMapper.deleteCommitRole(modelId);
        modelMapper.deleteAuditRole(modelId);
        modelMapper.deleteScore(modelId);
        //查询子叶维度id，删除对应得分项、扣分项、扣分项维度、运行监控、统计分析，删除维度
        List<Integer> leafId = modelMapper.findLeafId(modelId);
        if (leafId.size() != 0) {
            modelMapper.deleteOption(leafId);
            modelMapper.deleteDeduct(leafId);
            modelMapper.deleteDeductDimension(leafId);
            modelMapper.deleteMonitor(leafId);
            modelMapper.deleteAnalysis(leafId);
        }
        modelMapper.deleteDimension(modelId);
        //删除定时任务
        ScheduledExecutorService service = mapReportService.get(modelId);
        if (service != null) service.shutdown();
        mapReportService.remove(modelId);
        //删除模板启动时间
        DataRedisUtil.delete(modelId + "_startTime");
    }

    @Override
    @Transactional
    public void updateModel(JSONObject param) {
        String modelId = param.getString("modelId");
        List<JSONObject> organization = param.getJSONArray("organization").toJavaList(JSONObject.class);
        List<JSONObject> commitRole = param.getJSONArray("commitRole").toJavaList(JSONObject.class);
        List<JSONObject> auditRole = param.getJSONArray("auditRole").toJavaList(JSONObject.class);
        //比对旧参与组织信息
        List<JSONObject> oldOrganization = modelMapper.findBank(modelId);//旧参与组织
        HashSet<String> organ = new HashSet<>();
        HashSet<String> oldOrgan = new HashSet<>();
        organization.forEach(a -> organ.add(a.getString("name")));
        oldOrganization.forEach(a -> oldOrgan.add(a.getString("name")));
        List<JSONObject> needDelete = new LinkedList<>();//需要删除成绩的组织
        List<JSONObject> needInsert = new LinkedList<>();//需要新增成绩的组织
        for (JSONObject j : oldOrganization) {
            if (!organ.contains(j.getString("name"))) {
                needDelete.add(j);
            }
        }
        for (JSONObject j : organization) {
            if (!oldOrgan.contains(j.getString("name"))) {
                needInsert.add(j);
            }
        }
        if (!needDelete.isEmpty()) modelMapper.deleteScoreByBank(modelId, needDelete);//取消的组织删除其成绩
        //新增的组织添加成绩
        if (!needInsert.isEmpty()) {
            int year = Integer.parseInt(Utils.getCurrentTime("yyyy"));
            int nextYear = year + 1;
            List<JSONObject> dimen = modelMapper.findDimension(modelId);
            if (dimen.size() != 0) {
                for (JSONObject b : needInsert) {
                    String state = "待完善";
                    modelMapper.addScore(modelId, dimen, b.getString("name"), state, year);
                    modelMapper.addScore(modelId, dimen, b.getString("name"), state, nextYear);
                }
            }
        }
        //重建参与组织
        modelMapper.deleteBank(modelId);
        String organizationData = JSONObject.toJSONString(organization);
        modelMapper.addBank(modelId, organization, organizationData);
        //重建提交角色
        modelMapper.deleteCommitRole(modelId);
        List<JSONObject> listCommit = new LinkedList<>();
        String commitData = JSONObject.toJSONString(commitRole);
        for (JSONObject role : commitRole) {
            if (role.getIntValue("ifLeaf") != 1) {
                handleRole(role, listCommit, commitData);
            } else {
                listCommit.add(role);
            }
        }
        modelMapper.addCommitRole(modelId, listCommit, commitData);
        //重建审核角色
        modelMapper.deleteAuditRole(modelId);
        List<JSONObject> listAudit = new LinkedList<>();
        String auditData = JSONObject.toJSONString(auditRole);
        for (JSONObject role : auditRole) {
            if (role.getIntValue("ifLeaf") != 1) {
                handleRole(role, listAudit, commitData);
            } else {
                listAudit.add(role);
            }
        }
        modelMapper.addAuditRole(modelId, listAudit, auditData);
        //更新模板
        modelMapper.updateModel(param.getString("modelId"), param.getString("cycleValue"), param.getString("cycleUnit"),
                param.getString("title"), param.getString("iconName"), param.getString("borderColor"), param.getString("iconColor"),
                param.getString("backgroundColor"), param.getString("cycleStart"), param.getString("cycleEnd"),
                param.getString("description"), param.getJSONArray("gradation").toString());
    }

    @Override
    @Transactional
    public JSONObject addDimension(JSONObject param) {
        //新增维度
        String modelId = param.getString("modelId");
        JSONObject dimension = param.getJSONObject("dimension");//维度
        int rootId = dimension.getIntValue("rootId");
        int parentId = dimension.getIntValue("parentId");
        modelMapper.addDimension(modelId, rootId, parentId, dimension);
        //创建当年和次年成绩
        int year = Integer.parseInt(Utils.getCurrentTime("yyyy"));
        int nextYear = year + 1;
        List<JSONObject> dimen = new LinkedList<>();
        dimen.add(dimension);
        List<JSONObject> bank = modelMapper.findBank(modelId);
        for (JSONObject b : bank) {
            String state = "待完善";
            modelMapper.addScore(modelId, dimen, b.getString("name"), state, year);
            modelMapper.addScore(modelId, dimen, b.getString("name"), state, nextYear);
        }
        //如果是子叶维度
        if (dimension.getIntValue("ifLeaf") == 1) addLeafByType(modelId, dimension, dimension.getString("type"));
        return dimension;
    }

    @Override
    @Transactional
    public void saveDimension(JSONObject param) {
        String modelId = param.getString("modelId");
        JSONObject dimension = param.getJSONObject("dimension");//维度
        int id = dimension.getIntValue("id");
        String type = dimension.getString("type");
        int ifLeaf = dimension.getIntValue("ifLeaf");
        //判断原来维度是否是子叶，分4种情况讨论
        JSONObject oldDimen = modelMapper.findDimensionById(id);
        if (oldDimen.getIntValue("ifLeaf") == 0) {
            if (ifLeaf == 1) {
                //维度-子叶：查询所有下级维度、删除所有下级维度，如果有子叶删除对应子叶数据，同时删除成绩
                List<JSONObject> subs = modelMapper.findDimensionByParentId(id);
                if (subs.size() != 0) modelMapper.deleteDimensionByList(subs);
                for (JSONObject sub : subs) {
                    if (sub.getIntValue("ifLeaf") == 1) deleteLeafByType(sub, type);
                }
                //更新维度，新增子叶数据
                modelMapper.updateDimesion(dimension);
                addLeafByType(modelId, dimension, type);
            } else {
                //维度-维度：如果评分方式改变，说明根维度改变，查询所有rootId为根维度id的下级、删除所有下级、如果有子叶删除对应子叶数据，同时删除成绩
                if (!oldDimen.getString("type").equals(type)) {
                    List<JSONObject> subs = modelMapper.findDimensionByRootId(id);
                    if (subs.size() != 0) modelMapper.deleteDimensionByList(subs);
                    for (JSONObject sub : subs) {
                        if (sub.getIntValue("ifLeaf") == 1) deleteLeafByType(sub, oldDimen.getString("type"));
                    }
                }
                //更新维度
                modelMapper.updateDimesion(dimension);
                //判断名称是否变化，如变化更新所有下级维度的location字段
                if (!oldDimen.getString("name").equals(dimension.getString("name"))) {
                    List<JSONObject> needUpdate = new LinkedList<>();//需要更新的维度
                    handler(id, needUpdate);
                    String name = dimension.getString("name");
                    int level = dimension.getIntValue("level");
                    needUpdate.forEach(a -> {
                        String oldLocation = a.getString("location");
                        String[] arr = oldLocation.split(">");
                        arr[level] = name;
                        StringBuilder newLocation = new StringBuilder();
                        for (int i = 0; i < arr.length; i++) {
                            newLocation.append(arr[i]).append(">");
                        }
                        String substring = newLocation.substring(0, newLocation.length() - 1);
                        a.put("location", substring);
                    });
                    if (!needUpdate.isEmpty()) modelMapper.updateDimesionLocation(needUpdate);
                }
            }
        } else {
            if (ifLeaf == 1) {
                //子叶-子叶：删除对应子叶数据，同时删除成绩
                deleteLeafByType(dimension, type);
                //更新维度，新增子叶数据
                modelMapper.updateDimesion(dimension);
                addLeafByType(modelId, dimension, type);
                //创建当年和次年成绩
                List<JSONObject> bank = modelMapper.findBank(modelId);//参与的银行
                int year = Integer.parseInt(Utils.getCurrentTime("yyyy"));
                int nextYear = year + 1;
                List<JSONObject> dimen = new LinkedList<>();
                dimen.add(dimension);
                for (JSONObject b : bank) {
                    String state = "待完善";
                    modelMapper.addScore(modelId, dimen, b.getString("name"), state, year);
                    modelMapper.addScore(modelId, dimen, b.getString("name"), state, nextYear);
                }
            } else {
                //子叶-维度：删除对应子叶数据，同时删除成绩，更新维度
                deleteLeafByType(dimension, type);
                modelMapper.updateDimesion(dimension);
            }
        }
    }

    /**
     * 递归查询当前id维度下的所有下级维度
     */
    private void handler(int id, List<JSONObject> needUpdate) {
        List<JSONObject> list = modelMapper.findDimensionByParentId(id);
        needUpdate.addAll(list);
        for (JSONObject a : list) {
            if (a.getIntValue("ifLeaf") == 0) handler(a.getIntValue("id"), needUpdate);
        }
    }

    /**
     * 根据子叶类型删除子叶数据，同时删除成绩
     */
    private void deleteLeafByType(JSONObject leaf, String type) {
        int id = leaf.getIntValue("id");
        //单项选择
        if ("single".equals(type)) {
            modelMapper.deleteOptionById(id);
        }
        //累计扣分
        if ("deduct".equals(type)) {
            modelMapper.deleteDeductDimensionById(id);
            modelMapper.deleteDeductById(id);
        }
        //运行监控
        if ("monitor".equals(type)) {
            modelMapper.deleteMonitorById(id);
        }
        //统计分析
        if ("analysis".equals(type)) {
            modelMapper.deleteAnalysisById(id);
        }
        //删除成绩
        modelMapper.deleteScoreById(id);
    }

    /**
     * 根据子叶类型添加子叶数据
     */
    private void addLeafByType(String modelId, JSONObject jsonObject, String type) {
        List<JSONObject> bank = modelMapper.findBank(modelId);//参与的银行
        int dimensionId = jsonObject.getIntValue("id");
        //单项选择，获取得分项
        if ("single".equals(type)) {
            List<JSONObject> optionList = new LinkedList<>();//得分项列表
            List<JSONObject> option = jsonObject.getJSONArray("option").toJavaList(JSONObject.class);
            for (JSONObject object : option) {
                object.put("dimensionId", dimensionId);
            }
            optionList.addAll(option);
            for (JSONObject b : bank) {
                String bankName = b.getString("name");
                if (0 != optionList.size()) modelMapper.addOption(bankName, optionList);
            }
        }
        //累计扣分，获取扣分项
        if ("deduct".equals(type)) {
            List<JSONObject> deductDimensionList = new LinkedList<>();//扣分项结构列表
            //根据数据源、起止时间、状态获取扣分项列表
            JSONObject param = new JSONObject();//请求参数
            JSONObject source = jsonObject.getJSONObject("source");
            List<JSONObject> chooseState = jsonObject.getJSONArray("chooseState").toJavaList(JSONObject.class);
            String dateStart = jsonObject.getString("dateStart");
            String dateEnd = jsonObject.getString("dateEnd");
            String year = Utils.getCurrentTime("yyyy");
            //保存扣分维度结构
            JSONObject deductDimension = new JSONObject();
            deductDimension.put("dimensionId", jsonObject.getIntValue("id"));
            deductDimension.put("source", source.toJSONString());
            deductDimension.put("dateStart", dateStart);
            deductDimension.put("dateEnd", dateEnd);
            deductDimension.put("chooseState", jsonObject.getJSONArray("chooseState").toJSONString());
            deductDimension.put("chooseCondition", jsonObject.getJSONArray("chooseCondition").toJSONString());
            deductDimension.put("statisticsRule", jsonObject.getString("statisticsRule"));
            String calculationRule = jsonObject.getString("calculationRule");
            if (calculationRule.equals("百分比")) calculationRule += "#" + jsonObject.getJSONArray("condition");
            deductDimension.put("calculationRule", calculationRule);
            deductDimension.put("rule", jsonObject.getJSONArray("rule").toJSONString());
            deductDimensionList.add(deductDimension);
            //创建扣分维度结构
            for (JSONObject b : bank) {
                String bankName = b.getString("name");
                if (0 != deductDimensionList.size()) modelMapper.addDeductDimension(bankName, deductDimensionList);
            }
            //查询扣分项
            param.put("configDataId", source.getString("resourceId"));
            JSONObject query = new JSONObject();
            JSONObject search = new JSONObject();
            JSONObject status = new JSONObject();//筛选的状态
            StringBuilder value = new StringBuilder();
            for (JSONObject object : chooseState) {
                value.append(object.getString("value")).append(",");
            }
            String substring = value.substring(0, value.lastIndexOf(","));
            status.put("value", substring);
            search.put("status", status);
            JSONObject Field_xxx_create_time = new JSONObject();//筛选的起止时间
//            Field_xxx_create_time.put("startTime", year + "-" + dateStart);
//            Field_xxx_create_time.put("endTime", year + "-" + dateEnd);
            search.put("Field_xxx_create_time", Field_xxx_create_time);
            query.put("search", search);
            param.put("query", query);
            String cookie = RpcUtil.getCookie();
            String result = processEngineRpc.getFormData(cookie, param);
            JSONObject json = JSONObject.parseObject(result);
            if ("200".equals(json.getString("status"))) {
                List<JSONObject> data = json.getJSONObject("data").getJSONArray("instancesData").toJavaList(JSONObject.class);
                //有筛选条件进行筛选
                JSONArray chooseCondition = jsonObject.getJSONArray("chooseCondition");
                if (!chooseCondition.isEmpty()) {
                    List<JSONObject> condition = chooseCondition.toJavaList(JSONObject.class);
                    HashMap<String, String> map = new HashMap<>();
                    condition.forEach(j -> map.put(j.getString("text"), j.getString("value")));
                    Set<String> keySet = map.keySet();
                    Iterator<JSONObject> iterator = data.iterator();
                    while (iterator.hasNext()) {
                        JSONObject next = iterator.next();
                        //有一项不符合筛选项则删除
                        for (String a : keySet) {
                            String string = next.getString(a);
                            if (string == null || !string.equals(map.get(a))) {
                                iterator.remove();
                                break;
                            }
                        }
                    }
                }
                //组装数据
                List<JSONObject> deductList = new LinkedList<>();//扣分项列表
                for (JSONObject d : data) {
                    JSONObject jo = new JSONObject();
                    jo.put("bankName", d.getString("Field_xxx_create_dept") == null ? "" : d.getString("Field_xxx_create_dept"));
                    jo.put("dimensionId", dimensionId);
                    jo.put("billId", d.getString("id"));
                    jo.put("name", d.getString("Field_xxx_title"));
                    jo.put("location",jsonObject.getJSONObject("source").toJSONString());
                    jo.put("createTime", d.getString("Field_xxx_create_time").substring(0, 10));
                    jo.put("state", d.getString("status"));
                    jo.put("year", year);
                    deductList.add(jo);
                }
                //新增累计扣分扣分项
                if (0 != deductList.size()) assessMapper.addSourceData(deductList);
            } else {
                log.warn("查询流程引擎失败，接口：/api/flow/api/v1/bfm/instances/model/list，返回信息：" + json.toJSONString());
            }
        }
        //运行监控，获取结构
        if ("monitor".equals(type)) {
            JSONArray array = jsonObject.getJSONArray("bankSpot");
            if (array != null) {
                List<JSONObject> monitorList = new LinkedList<>();//运行监控列表
                List<JSONObject> bankSpot = array.toJavaList(JSONObject.class);
                for (JSONObject bank1 : bankSpot) {
                    JSONObject temp = new JSONObject();
                    temp.putAll(jsonObject);
                    temp.put("bankName", bank1.getString("name"));
                    temp.put("bankId", bank1.getIntValue("id"));
                    temp.put("dimensionId", dimensionId);
                    temp.put("resources", bank1.getJSONArray("resources").toJSONString());
                    temp.put("rule", jsonObject.getJSONArray("rule").toJSONString());
                    monitorList.add(temp);
                }
                //新增运行监控结构
                if (0 != monitorList.size()) modelMapper.addMonitor(monitorList);
            }
        }
        //统计分析
        if ("analysis".equals(type)) {
            List<JSONObject> analysisList = new LinkedList<>();//统计分析列表
            if (jsonObject.getIntValue("timeLimit") == 1) {
                jsonObject.put("rule", jsonObject.getJSONArray("rule").toJSONString());
            } else {
                jsonObject.put("rule", "[]");
            }
            jsonObject.put("deduct", jsonObject.getDoubleValue("deduct"));
            jsonObject.put("dimensionId", dimensionId);
            analysisList.add(jsonObject);
            //新增统计分析结构
            for (JSONObject b : bank) {
                String bankName = b.getString("name");
                if (0 != analysisList.size()) modelMapper.addAnalysis(bankName, analysisList);
            }
        }
    }

    @Override
    @Transactional
    public void deleteDimension(JSONObject param) {
        JSONObject dimension = param.getJSONObject("dimension");//维度
        int id = dimension.getIntValue("id");
        String type = dimension.getString("type");
        //递归查询该维度下所有的维度，删除维度，如果是子叶删除对应子叶数据，同时删除成绩
        List<JSONObject> list = new LinkedList<>();//需要删除的维度
        recursiveQuery(id, list);
        list.add(dimension);//加上自己
        if (list.size() != 0) modelMapper.deleteDimensionByList(list);
        for (JSONObject sub : list) {
            if (sub.getIntValue("ifLeaf") == 1) deleteLeafByType(sub, type);
        }
    }

    /**
     * 递归查询该维度下所有的维度
     */
    private List<JSONObject> recursiveQuery(int id, List<JSONObject> list) {
        List<JSONObject> subs = modelMapper.findDimensionByParentId(id);
        if (!subs.isEmpty()) {
            for (JSONObject sub : subs) {
                recursiveQuery(sub.getIntValue("id"), list);
            }
        }
        list.addAll(subs);
        return list;
    }
}
