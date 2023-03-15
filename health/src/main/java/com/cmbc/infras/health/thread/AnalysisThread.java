package com.cmbc.infras.health.thread;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.health.dto.AlarmRequestParam;
import com.cmbc.infras.health.mapper.AssessMapper;
import com.cmbc.infras.health.mapper.ModelMapper;
import com.cmbc.infras.health.rpc.ProcessEngineRpc;
import com.cmbc.infras.health.redis.DataRedisUtil;
import com.cmbc.infras.health.rpc.RpcUtil;
import com.cmbc.infras.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 统计分析线程
 */
@Slf4j
public class AnalysisThread implements Runnable {

    private String modelId;
    private ModelMapper modelMapper;
    private AssessMapper assessMapper;
    private ProcessEngineRpc processEngineRpc;

    public AnalysisThread(String modelId, ModelMapper modelMapper, AssessMapper assessMapper, ProcessEngineRpc processEngineRpc) {
        this.modelId = modelId;
        this.modelMapper = modelMapper;
        this.assessMapper = assessMapper;
        this.processEngineRpc = processEngineRpc;
    }

    @Override
    @Transactional
    public void run() {
        //从数据库获取结构、访问KE数据、获取告警信息、计算并登记成绩
        //获取analysis的所有子叶维度id和score
        List<JSONObject> idAndScore = modelMapper.findAnalysisLeaf(modelId);
        HashMap<Integer, Double> idScore = new HashMap<>();
        for (JSONObject j : idAndScore) {
            idScore.put(j.getIntValue("id"), j.getDoubleValue("score"));
        }
        //所有银行和子叶维度信息
        List<JSONObject> leafs = modelMapper.findAnalysisList(idAndScore);
        //成绩
        List<JSONObject> listResult = new LinkedList<>();
        int year1 = Integer.parseInt(Utils.getCurrentTime("yyyy"));
        for (JSONObject leaf : leafs) {
            //获取银行bank_id
            String bankName = leaf.getString("bankName");
            //组装参数
            String bankId = "";
            JSONObject param = new JSONObject();
            JSONObject query = new JSONObject();
            JSONObject search = new JSONObject();
            search.put("Field_xxx_title", bankName);
            query.put("search", search);
            param.put("configDataId", "FHXX");//"分行信息"表单
            param.put("query", query);
            //从redis获取token，没有执行后台登录，获取token存入redis并设置90min失效时间
            String token = DataRedisUtil.getStringFromRedis("health_ke_token");
            if (token == null) {
                token = RpcUtil.getToken();
                DataRedisUtil.addStringToRedis("health_ke_token", token, 1000 * 60 * 90l);
            }
            String result = processEngineRpc.getFormDataByToken(token, param);
            JSONObject json = JSONObject.parseObject(result);
            if ("200".equals(json.getString("status"))) {
                JSONArray jsonArray = json.getJSONObject("data").getJSONArray("instancesData");
                if (0 != jsonArray.size()) {
                    JSONObject data = jsonArray.toJavaList(JSONObject.class).get(0);
                    bankId = data.getString("bank_id");
                }
            } else {
                log.warn("查询流程引擎失败，接口：/api/flow/api/v1/bfm/instances/model/list，返回信息：" + json.toJSONString());
            }
            //按照周期天数获取对应类型的告警数据
            if (!StringUtils.isEmpty(bankId)) {
                int cycle = leaf.getInteger("cycle");
                //获取系统当前时间毫秒值，向前减去cycle天，去掉最后三位作为请求参数
                long end = System.currentTimeMillis();
                long tem = 1000 * 60 * 60 * 24l * cycle;
                long start = (end - tem);
                start /= 1000;
                end /= 1000;
                //获取模板启动时间，如果大于start，则覆盖start值
                String stringFromRedis = DataRedisUtil.getStringFromRedis(modelId + "_startTime");
                if (stringFromRedis != null) {
                    Long startTime = Long.valueOf(stringFromRedis);
                    log.info(modelId + "模板启动时间为：" + startTime);
                    if (startTime > start) start = startTime;
                }
                //组装参数，获取告警信息
                int eventLevel = leaf.getInteger("eventLevel");
                String[] bankIdArr = {bankId};
                AlarmRequestParam param1 = new AlarmRequestParam(bankIdArr, start, end, new int[]{eventLevel});
                String result1 = processEngineRpc.getAlarmData(InfrasConstant.KE_RPC_COOKIE, param1);
                JSONObject json1 = JSONObject.parseObject(result1);
                if ("00".equals(json1.getString("error_code"))) {
                    List<JSONObject> eventList = json1.getJSONObject("data").getJSONArray("event_list").toJavaList(JSONObject.class);
                    int dimensionId = leaf.getIntValue("dimensionId");
                    Double score = modelMapper.findDimensionScore(dimensionId);//总分值
                    Double deduct = 0D;//扣分值
                    //根据timeLimit是否开启分别计算成绩
                    if (leaf.getIntValue("timeLimit") == 0) {
                        Double d = leaf.getDouble("deduct");//单个告警扣分分值
                        int count = 0;//不符合的告警数量
                        //根据alarmType类型计算
                        String alarmType = leaf.getString("alarmType");
                        if ("告警受理".equals(alarmType)) {
                            for (JSONObject event : eventList) {
                                //只判断未恢复的告警
                                if (event.getIntValue("is_recover") == 0) {
                                    long acceptTime = event.getLong("accept_time");//受理时间
                                    long confirmTime = event.getLong("confirm_time");//确认时间
                                    if (acceptTime == 0 && confirmTime == 0) count++;
                                }
                            }
                        }
                        if ("告警确认".equals(alarmType)) {
                            for (JSONObject event : eventList) {
                                //只判断未恢复的告警
                                if (event.getIntValue("is_recover") == 0) {
                                    long confirmTime = event.getLong("confirm_time");//确认时间
                                    if (confirmTime == 0) count++;
                                }
                            }
                        }
                        deduct = count * d;
                    } else {
                        //根据alarmType类型、rule计算得分
                        String alarmType = leaf.getString("alarmType");
                        if ("告警受理".equals(alarmType)) {
                            for (JSONObject event : eventList) {
                                //只判断未恢复的告警
                                if (event.getIntValue("is_recover") == 0) {
                                    Double temp = 0D;
                                    long eventTime = event.getLong("event_time");
                                    //如果没有受理时间按照当前时间处理
                                    long acceptTime = event.getLong("accept_time") == 0 ? System.currentTimeMillis() / 1000 : event.getLong("accept_time");
                                    long deductTime = (acceptTime - eventTime) / 60;//转化为分钟
                                    List<JSONObject> rule = leaf.getJSONArray("rule").toJavaList(JSONObject.class);
                                    for (JSONObject r : rule) {
                                        Double start1 = r.getDouble("start");
                                        Double end1 = r.getString("end").equals("*") ? Double.MAX_VALUE : r.getDouble("end");
                                        if (deductTime >= start1 && deductTime < end1) {
                                            temp = r.getDouble("score");
                                            break;
                                        }
                                    }
                                    deduct += temp;
                                }
                            }
                        }
                        if ("告警确认".equals(alarmType)) {
                            for (JSONObject event : eventList) {
                                //只判断未恢复的告警
                                if (event.getIntValue("is_recover") == 0) {

                                    Double temp = 0D;
                                    long eventTime = event.getLong("event_time");
                                    //如果没有确认时间按照当前时间处理
                                    long confirmTime = event.getLong("confirm_time") == 0 ? System.currentTimeMillis() / 1000 : event.getLong("confirm_time");
                                    long deductTime = (confirmTime - eventTime) / 60;//转化为分钟
                                    List<JSONObject> rule = leaf.getJSONArray("rule").toJavaList(JSONObject.class);
                                    for (JSONObject r : rule) {
                                        Double start1 = r.getDouble("start");
                                        Double end1 = r.getString("end").equals("*") ? Double.MAX_VALUE : r.getDouble("end");
                                        if (deductTime >= start1 && deductTime < end1) {
                                            temp = r.getDouble("score");
                                            break;
                                        }
                                    }
                                    deduct += temp;
                                }
                            }
                        }
                    }
                    //封装成绩
                    JSONObject j = new JSONObject();
                    j.put("id", leaf.getIntValue("dimensionId"));
                    double finalResult = (score - deduct) < 0 ? 0 : (score - deduct);
                    j.put("score", finalResult);
                    j.put("auditResult", finalResult < idScore.get(dimensionId) ? 0 : 1);
                    j.put("name", leaf.getString("bankName"));
                    j.put("year", year1);
                    listResult.add(j);
                } else {
                    log.warn("查询KE失败，接口：/api/v2/tsdb/status/event，返回信息：" + json1.toJSONString());
                }
            } else {
                log.warn("未查询到银行id");
            }
        }
        //登记成绩
        String state = "自动";
        assessMapper.updateAutomaticScore(listResult, state);
        //计算各银行总成绩
        List<JSONObject> banks = modelMapper.findBank(modelId);
        for (JSONObject bank : banks) {
            String name = bank.getString("name");
            List<JSONObject> list = new LinkedList<>();
            for (JSONObject i : idAndScore) {
                JSONObject j = new JSONObject();
                j.put("id", i.getIntValue("id"));
                list.add(j);
            }
            List<JSONObject> listMap = assessMapper.findDimensionByList(list);
            HashMap<Integer, Integer> idRoot = new HashMap<>();//存放子叶维度与根维度关系
            for (JSONObject jo : listMap) {
                idRoot.put(jo.getIntValue("id"), jo.getIntValue("rootId"));
            }
            HashMap<Integer, Double> result = new HashMap<>();//存放根维度id与score
            List<JSONObject> listScore = assessMapper.findIdAndScore(name, year1, list);
            for (JSONObject jo : listScore) {
                int id = jo.getIntValue("dimensionId");
                int rootId = idRoot.get(id);
                Double score = result.getOrDefault(rootId, 0D);
                result.put(rootId, score + jo.getDoubleValue("score"));
            }
            List<JSONObject> listResult1 = new LinkedList<>();
            result.forEach((k, v) -> {
                JSONObject j = new JSONObject();
                j.put("id", k);
                j.put("score", v);
                j.put("name", name);
                j.put("year", year1);
                listResult1.add(j);
            });
            //更新维度成绩
            assessMapper.updateResultByList(listResult1, state);
        }
    }
}
