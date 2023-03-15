package com.cmbc.infras.health.thread;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.health.mapper.AssessMapper;
import com.cmbc.infras.health.mapper.ModelMapper;
import com.cmbc.infras.health.redis.DataRedisUtil;
import com.cmbc.infras.health.util.CommonUtils;
import com.cmbc.infras.util.Utils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 生成报告线程
 */
public class CreateReport implements Runnable {

    private static Logger logger = Logger.getLogger(CreateReport.class.getName());

    private String modelId;
    private ModelMapper modelMapper;
    private AssessMapper assessMapper;
    private boolean ignoreLock;//是否忽略分布式锁

    public CreateReport(String modelId, ModelMapper modelMapper, AssessMapper assessMapper, boolean ignoreLock) {
        this.modelId = modelId;
        this.modelMapper = modelMapper;
        this.assessMapper = assessMapper;
        this.ignoreLock = ignoreLock;
    }

    @Override
    @Transactional
    public void run() {
        JSONObject model = modelMapper.findModel(modelId);
        //模板未启动则返回
        if (model.getIntValue("startScore") == 0) return;
        //查询报告生成周期
        int cycleValue = model.getIntValue("cycleValue");
        String cycleUnit = model.getString("cycleUnit");
        long cycle = cycleValue * CommonUtils.change(cycleUnit) * 1000;//单位毫秒
        cycle -= 5000;//提前5秒释放锁，保证下个周期能拿到锁
        //尝试获取分布式锁
        boolean lock = DataRedisUtil.lock(modelId + "_lock", "lock", cycle);
        if (ignoreLock || lock) {
            long startTime = System.currentTimeMillis();
            List<JSONObject> listReport = new LinkedList<>();
            int year = Integer.parseInt(Utils.getCurrentTime("yyyy"));
            List<JSONObject> bank = modelMapper.findBank(modelId);
            JSONObject ver = modelMapper.findReportVersion(modelId);
            for (JSONObject b : bank) {
                String name = b.getString("name");
                JSONObject jo = new JSONObject();
                jo.put("modelId", model.getString("modelId"));
                jo.put("title", model.getString("title"));
                jo.put("description", model.getString("description"));
                jo.put("name", name);
                jo.put("year", year);
                //测评维度
                List<JSONObject> dimension = new LinkedList<>();
                List<JSONObject> scores = assessMapper.findScore(modelId, name, year);
                for (JSONObject score : scores) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("rootId", score.getIntValue("id"));
                    jsonObject.put("score", score.getDoubleValue("score"));
                    jsonObject.put("result", score.getDoubleValue("result"));
                    DecimalFormat df = new DecimalFormat("0%");
                    String score1 = df.format(score.getDoubleValue("result") / score.getDoubleValue("score"));
                    jsonObject.put("rate", score1);
                    jsonObject.put("name", score.getString("name"));
                    dimension.add(jsonObject);
                }
                //成绩和总分
                double reslut = 0D;//成绩
                double score1 = 0D;//总分
                for (JSONObject a : scores) {
                    reslut += a.getDoubleValue("result");
                    score1 += a.getDoubleValue("score");
                }
                //测评详情
                List<JSONObject> detail = new LinkedList<>();
                List<JSONObject> listLeaf = modelMapper.findLeaf(modelId);
                List<JSONObject> listType = modelMapper.findType();
                HashMap<String, String> mapType = new HashMap<>();
                for (JSONObject jo1 : listType) {
                    mapType.put(jo1.getString("name"), jo1.getString("description"));
                }
                //封装id和成绩
                List<Integer> leafId = modelMapper.findLeafId(modelId);
                List<JSONObject> auditResult = modelMapper.findAuditResult(name, year, leafId);
                HashMap<Integer, Double> mapScore = new HashMap<>();//id:成绩
                for (JSONObject j : auditResult) {
                    mapScore.put(j.getIntValue("id"), j.getDoubleValue("result"));
                }
                int valid = 0;//测评分析里已满足的项目数
                for (JSONObject leaf : listLeaf) {
                    JSONObject json = new JSONObject();
                    json.put("rootId", leaf.getIntValue("rootId"));
                    json.put("location", leaf.getString("location"));
                    json.put("type", mapType.get(leaf.getString("type")));
                    double score = leaf.getDoubleValue("score");
                    json.put("score", score);
                    int id = leaf.getIntValue("id");
                    double result = mapScore.get(id);
                    json.put("result", result);
                    if (result == score) {
                        json.put("assess", "已满足");
                        valid++;
                    } else {
                        json.put("assess", "可提升");
                    }
                    detail.add(json);
                }
                //测评分析
                JSONObject analysis = new JSONObject();
                int total = listLeaf.size();
                analysis.put("total", total);
                analysis.put("valid", valid);
                analysis.put("fail", total - valid);
                //封装信息
                JSONObject report = new JSONObject();
                report.put("dimension", dimension);
                report.put("analysis", analysis);
                report.put("detail", detail);
                report.put("result", reslut);
                report.put("score", score1);
                jo.put("report", JSONObject.toJSONString(report));
                listReport.add(jo);
            }
            int version = ver == null ? 0 : ver.getInteger("version");
            logger.info("数据库查询到的version=" + version);
            version++;
            String time = Utils.getCurrentTime("yyyy-MM-dd HH:mm:ss");
            modelMapper.createReport(listReport, version, time);
            logger.info("耗时:" + (System.currentTimeMillis() - startTime));
        }
    }
}