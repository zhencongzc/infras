package com.cmbc.infras.health.thread;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.health.mapper.AssessMapper;
import com.cmbc.infras.health.mapper.ModelMapper;
import com.cmbc.infras.health.rpc.ProcessEngineRpc;
import com.cmbc.infras.util.NumberUtils;
import com.cmbc.infras.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 运行监控线程
 */
@Slf4j
public class MonitorThread implements Runnable {

    private String modelId;
    private ModelMapper modelMapper;
    private AssessMapper assessMapper;
    private ProcessEngineRpc processEngineRpc;

    public MonitorThread(String modelId, ModelMapper modelMapper, AssessMapper assessMapper, ProcessEngineRpc processEngineRpc) {
        this.modelId = modelId;
        this.modelMapper = modelMapper;
        this.assessMapper = assessMapper;
        this.processEngineRpc = processEngineRpc;
    }

    @Override
    @Transactional
    public void run() {
        //从数据库获取结构、访问表单获取测点id、访问KE获取测点数据、计算并登记成绩
        //获取monitor的子叶维度id和score
        List<JSONObject> leaf = modelMapper.findMonitorLeaf(modelId);
        HashMap<Integer, Double> idScore = new HashMap<>();
        for (JSONObject j : leaf) {
            idScore.put(j.getIntValue("id"), j.getDoubleValue("score"));
        }
        //所有银行和测点
        List<JSONObject> spots = modelMapper.findMonitorList(leaf);
        //成绩
        List<JSONObject> listResult = new LinkedList<>();
        int year1 = Integer.parseInt(Utils.getCurrentTime("yyyy"));
        for (JSONObject spot : spots) {
            //获取测点数据
            JSONObject param = new JSONObject();
            param.put("resources", spot.getJSONArray("resources"));
            String result = processEngineRpc.getSpotData(InfrasConstant.KE_RPC_COOKIE, param);
            JSONObject json = JSONObject.parseObject(result);
            if ("00".equals(json.getString("error_code"))) {
                //存储测点值
                List<JSONObject> resources = JSONObject.parseObject(json.getString("data")).getJSONArray("resources").toJavaList(JSONObject.class);
                List<Double> value = new LinkedList<>();
                resources.forEach((a) -> {
                    String realValue = a.getString("real_value");
                    if (NumberUtils.isNumeric(realValue)) value.add(Double.parseDouble(realValue));
                });
                //计算平均值
                Double average = 0D;
                for (Double v : value) {
                    average += v;
                }
                if (value.size() != 0) average /= value.size();
                //根据rule计算成绩
                Double score = 0D;//扣分值
                List<JSONObject> rule = spot.getJSONArray("rule").toJavaList(JSONObject.class);
                for (JSONObject r : rule) {
                    Double start = r.getDouble("start");
                    Double end = r.getString("end").equals("*") || r.getString("end").equals("∞") ? Double.MAX_VALUE : r.getDouble("end");
                    if (average >= start && average < end) {
                        score = r.getDouble("score");
                        break;
                    }
                }
//                log.info("MonitorThread运行监控日志：银行名称：{}，测点类型:{}，测点值：{}，得分：{}", spot.getString("bankName"), spot.getString("spotType"), value.toString(), score);
                //封装成绩
                JSONObject j = new JSONObject();
                int dimensionId = spot.getIntValue("dimensionId");
                j.put("id", dimensionId);
                j.put("score", score);
                j.put("auditResult", score < idScore.get(dimensionId) ? 0 : 1);
                j.put("name", spot.getString("bankName"));
                j.put("year", year1);
                listResult.add(j);
            } else {
                log.warn("查询KE失败，接口：/api/v2/tsdb/status/last，返回信息：" + json.toJSONString());
            }
        }
        //登记成绩
        String state = "自动";
        assessMapper.updateAutomaticScore(listResult, state);
        //计算各银行总成绩
        List<JSONObject> banks = modelMapper.findBank(modelId);//所有参与的银行
        for (JSONObject b : banks) {
            String name = b.getString("name");
            List<JSONObject> list = new LinkedList<>();
            for (JSONObject i : leaf) {
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
