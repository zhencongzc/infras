package com.cmbc.infras.health.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.health.contant.AssessmentTypeEnum;
import com.cmbc.infras.health.contant.DrillMissionSubTableIdEnum;
import com.cmbc.infras.health.mapper.AssessMapper;
import com.cmbc.infras.health.mapper.ModelMapper;
import com.cmbc.infras.health.mapper.StatisticsMapper;
import com.cmbc.infras.health.service.AssessService;
import com.cmbc.infras.health.service.CommonService;
import com.cmbc.infras.health.service.StatisticsService;
import com.cmbc.infras.health.thread.CreateReport;
import com.cmbc.infras.util.DateTimeUtils;
import com.cmbc.infras.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class StatisticsServiceImple implements StatisticsService {

    @Resource
    private CommonService commonService;
    @Resource
    private AssessService assessService;

    @Resource
    private ModelMapper modelMapper;
    @Resource
    private AssessMapper assessMapper;
    @Resource
    private StatisticsMapper statisticsMapper;

    @Override
    public List<JSONObject> rating(String modelId) throws Exception {
        JSONObject model = modelMapper.findModel(modelId);
        JSONArray gradation = model.getJSONArray("gradation");
        if (gradation == null) throw new Exception("未设置等级划分！");

        //查询模板成绩
        List<JSONObject> data = commonService.evaluation(modelId).getData();
        HashMap<String, Integer> map = new HashMap<>();//存放评分等级和该等级分数的数量
        List<JSONObject> rule = gradation.toJavaList(JSONObject.class);//评分规则
        for (JSONObject d : data) {
            double score = d.getDoubleValue("score");
            for (int i = 0; i < rule.size(); i++) {
                JSONObject r = rule.get(i);
                double start = r.getDoubleValue("start");
                double end = r.getDoubleValue("end");
                String name = r.getString("name");
                if (score >= start && score < end) {
                    map.putIfAbsent(name, 0);
                    map.put(name, map.get(name) + 1);
                    break;
                }
                //第一个判断项包含后面的值 90 <= 优 <= 100
                if (i == 0 && score == end) {
                    map.putIfAbsent(name, 0);
                    map.put(name, map.get(name) + 1);
                    break;
                }
            }
        }
        //返回参数
        rule.forEach(a -> a.put("count", map.getOrDefault(a.getString("name"), 0)));
        return rule;
    }

    @Override
    public List<JSONObject> trend(String modelId) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //从当前日期起往前推，每隔7天查一次模板，查询12周的，展示日期和总分的中位数
        List<JSONObject> res = new LinkedList<>();
        String date = DateTimeUtils.getCurrentTime("yyyy-MM-dd");
        long time = sdf.parse(date).getTime();
        for (int i = 0; i < 13; i++) {
            long t = 1000 * 60 * 60 * 24 * 7l * i;//向前推的时间
            String dateStart = sdf.format(time - t);
            JSONObject j = getMedianByDate(modelId, dateStart, i);
            res.add(j);
        }
        return res;
    }

    /**
     * 查询该模板当前日期各分行总成绩的中位数
     */
    private JSONObject getMedianByDate(String modelId, String dateStart, int i) throws Exception {
        //计算结束时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date parse = sdf.parse(dateStart);
        long time = parse.getTime() + 1000 * 60 * 60 * 24;
        String dateEnd = sdf.format(time);
        //查询当日数据，如果当天没有数据则生成一份报告再获取
        List<JSONObject> list = statisticsMapper.findReportByDate(modelId, dateStart, dateEnd);
        if (i == 0 && list.size() == 0) {
            Thread thread = new Thread(new CreateReport(modelId, modelMapper, assessMapper, true));
            thread.start();
            thread.join(0);
            list = statisticsMapper.findReportByDate(modelId, dateStart, dateEnd);
        }
        //创建按照成绩升序的TreeSet
        TreeSet<JSONObject> set = new TreeSet<>((o1, o2) -> {
            if (o1.getDoubleValue("result") - o2.getDoubleValue("result") < 0) return -1;
            return 1;
        });
        //计算成绩并存入TreeSet
        for (JSONObject j : list) {
            JSONObject report = j.getJSONObject("report");
            if (report.getDouble("result") == null) {
                double result = 0D;
                List<JSONObject> dimension = report.getJSONArray("dimension").toJavaList(JSONObject.class);
                for (JSONObject d : dimension) {
                    result += d.getDoubleValue("result");
                }
                report.put("result", result);
            }
            set.add(report);
        }
        //返回TreeSet中位数
        JSONObject res = new JSONObject();
        res.put("date", dateStart);
        ArrayList<JSONObject> l = new ArrayList<>(set);
        res.put("value", set.size() == 0 ? 0 : l.get(l.size() >> 1).getDoubleValue("result"));
        return res;
    }

    @Override
    public JSONObject overview(String modelId) {
        JSONObject res = new JSONObject();
        JSONObject model = modelMapper.findModel(modelId);
        res.put("title", model.getString("title"));//评分体系
        res.put("cycleUnit", model.getString("cycleUnit"));//统计周期
        res.put("year", DateTimeUtils.getCurrentTime("yyyy") + "年");//当前年度
        //评分等级
        JSONArray gradation = model.getJSONArray("gradation");
        String gra = "";
        if (gradation != null) {
            StringBuffer sb = new StringBuffer();
            List<JSONObject> array = gradation.toJavaList(JSONObject.class);
            for (JSONObject j : array) {
                sb.append(j.getString("name")).append("/");
            }
            gra = sb.substring(0, sb.length() - 1);
        }
        res.put("gradation", gra);//评分等级
        //参与单位数量
        int organCount = statisticsMapper.findBankCount(modelId);
        res.put("organCount", organCount);//参与单位数量
        //体系总分
        List<JSONObject> dimens = modelMapper.findRootDimension(modelId);
        double score = 0D;
        for (JSONObject d : dimens) {
            score += d.getDoubleValue("score");
        }
        res.put("score", score);//体系总分
        //维度信息
        List<JSONObject> list = new LinkedList<>();
        List<JSONObject> leafs = modelMapper.findLeafCount(modelId);//存放根维度id及子维度数量
        HashMap<Integer, Integer> map = new HashMap<>();
        leafs.forEach(a -> map.put(a.getIntValue("rootId"), a.getIntValue("count")));
        dimens.forEach(a -> {
            JSONObject j = new JSONObject();
            j.put("name", a.getString("name"));
            j.put("score", a.getDoubleValue("score"));
            j.put("count", map.getOrDefault(a.getIntValue("id"), 0));
            j.put("type", AssessmentTypeEnum.getType(a.getString("type")));
            list.add(j);
        });
        res.put("list", list);
        return res;
    }

    @Override
    public List<JSONObject> rank(String modelId) {
        return commonService.evaluation(modelId).getData();
    }

    @Override
    public List<JSONObject> scoreRate(String modelId) {
        List<JSONObject> data = commonService.evaluation(modelId).getData();
        int count = data.size();//参与的组织数
        //计算各维度的总成绩存入map
        List<JSONObject> dimens = modelMapper.findRootDimension(modelId);
        HashMap<Integer, Double> map = new HashMap<>();//维度id和总成绩
        for (JSONObject d : data) {
            List<JSONObject> list = d.getJSONArray("list").toJavaList(JSONObject.class);
            for (JSONObject j : list) {
                int id = j.getIntValue("id");
                double score = j.getDoubleValue("score");
                map.putIfAbsent(id, 0d);
                map.put(id, map.get(id) + score);
            }
        }
        //封装结果
        List<JSONObject> res = new LinkedList<>();
        DecimalFormat df = new DecimalFormat("#0");
        for (JSONObject d : dimens) {
            JSONObject j = new JSONObject();
            j.put("id", d.getIntValue("id"));
            j.put("name", d.getString("name"));
            //rate = 维度总成绩/维度总分值*100
            j.put("rate", df.format(map.get(d.getIntValue("id")) * 100 / (d.getDoubleValue("score") * count)));
            res.add(j);
        }
        return res;
    }

    @Override
    public List<JSONObject> dimensionTrend(String modelId) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //从当前日期起往前推，每隔7天查一次模板，查询12周的，展示日期和总分的中位数
        List<JSONObject> res = new LinkedList<>();
        String date = DateTimeUtils.getCurrentTime("yyyy-MM-dd");
        long time = sdf.parse(date).getTime();
        for (int i = 0; i < 13; i++) {
            long t = 1000 * 60 * 60 * 24 * 7l * i;//向前推的时间
            String dateStart = sdf.format(time - t);
            JSONObject j = getDimenMedianByDate(modelId, dateStart, i);
            res.add(j);
        }
        return res;
    }

    /**
     * 查询该模板当前日期各分行各维度的中位数
     */
    private JSONObject getDimenMedianByDate(String modelId, String dateStart, int i) throws Exception {
        //计算结束时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date parse = sdf.parse(dateStart);
        long time = parse.getTime() + 1000 * 60 * 60 * 24;
        String dateEnd = sdf.format(time);
        //查询当日数据
        List<JSONObject> list = statisticsMapper.findReportByDate(modelId, dateStart, dateEnd);
        if (i == 0 && list.size() == 0) {
            Thread thread = new Thread(new CreateReport(modelId, modelMapper, assessMapper, true));
            thread.start();
            thread.join(0);
            list = statisticsMapper.findReportByDate(modelId, dateStart, dateEnd);
        }
        //创建HashMap存放维度id和TreeSet
        HashMap<Integer, TreeSet<JSONObject>> map = new HashMap<>();
        //将各维度信息存入map
        for (JSONObject j : list) {
            JSONObject report = j.getJSONObject("report");
            List<JSONObject> dimension = report.getJSONArray("dimension").toJavaList(JSONObject.class);
            for (JSONObject d : dimension) {
                int id = d.getIntValue("rootId");
                //若果不存在，创建按照成绩升序的TreeSet
                map.putIfAbsent(id, new TreeSet<>((o1, o2) -> {
                    if (o1.getDoubleValue("result") - o2.getDoubleValue("result") < 0) {
                        return -1;
                    }
                    return 1;
                }));
                map.get(id).add(d);
            }
        }
        //封装结果
        List<JSONObject> dimens = modelMapper.findRootDimension(modelId);
        JSONObject res = new JSONObject();
        res.put("date", dateStart);
        List<JSONObject> list1 = new LinkedList<>();
        for (JSONObject d : dimens) {
            JSONObject j = new JSONObject();
            int id = d.getIntValue("id");
            TreeSet<JSONObject> set = map.get(id) == null ? new TreeSet<>() : map.get(id);
            ArrayList<JSONObject> l = new ArrayList<>(set);
            j.put("name", d.getString("name"));
            j.put("value", set.size() == 0 ? 0 : l.get(l.size() >> 1).getDoubleValue("result"));
            list1.add(j);
        }
        res.put("list", list1);
        return res;
    }

    @Override
    public List<JSONObject> branchScoreRate(String modelId, String name) {
        JSONObject jsonObject = assessService.assessDimension(modelId, name, 1, 1);
        String year = DateTimeUtils.getCurrentTime("yyyy");
        List<JSONObject> res = jsonObject.getJSONArray(year).toJavaList(JSONObject.class);
        DecimalFormat df = new DecimalFormat("#0%");
        for (JSONObject l : res) {
            double result = l.getDoubleValue("result");
            double score = l.getDoubleValue("score");
            String rate = df.format(result / score);
            l.put("rate", rate);
        }
        return res;
    }

    @Override
    public List<JSONObject> branchScoreTrend(String modelId, String name) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //从当前日期起往前推，每隔7天查一次模板，查询12周的，展示日期和总分的中位数
        List<JSONObject> res = new LinkedList<>();
        String date = DateTimeUtils.getCurrentTime("yyyy-MM-dd");
        long time = sdf.parse(date).getTime();
        for (int i = 0; i < 13; i++) {
            long t = 1000 * 60 * 60 * 24 * 7l * i;//向前推的时间
            String dateStart = sdf.format(time - t);
            JSONObject j = getDimenMedianByBank(modelId, name, dateStart, i);
            res.add(j);
        }
        return res;
    }

    /**
     * 查询该模板当前日期当前分行各维度的中位数
     */
    private JSONObject getDimenMedianByBank(String modelId, String name, String dateStart, int i) throws Exception {
        //计算结束时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date parse = sdf.parse(dateStart);
        long time = parse.getTime() + 1000 * 60 * 60 * 24;
        String dateEnd = sdf.format(time);
        //查询当日数据
        JSONObject data = statisticsMapper.findReportByBank(modelId, name, dateStart, dateEnd);
        if (i == 0 && data == null) {
            Thread thread = new Thread(new CreateReport(modelId, modelMapper, assessMapper, true));
            thread.start();
            thread.join(0);
            data = statisticsMapper.findReportByBank(modelId, name, dateStart, dateEnd);
        }
        //封装数据
        JSONObject res = new JSONObject();
        res.put("date", dateStart);
        List<JSONObject> dimens = modelMapper.findRootDimension(modelId);//维度信息
        if (data == null) {
            List<JSONObject> list = new LinkedList<>();
            for (JSONObject d : dimens) {
                JSONObject j = new JSONObject();
                j.put("name", d.getString("name"));
                j.put("value", 0);
                list.add(j);
            }
            res.put("list", list);
        } else {
            JSONObject report = data.getJSONObject("report");
            List<JSONObject> dimension = report.getJSONArray("dimension").toJavaList(JSONObject.class);
            List<JSONObject> list = new LinkedList<>();
            for (JSONObject d : dimension) {
                JSONObject j = new JSONObject();
                j.put("name", d.getString("name"));
                j.put("value", d.getDoubleValue("result"));
                list.add(j);
            }
            res.put("list", list);
        }
        return res;
    }

    @Override
    public List<JSONObject> branchDimensionRank(String modelId, String name) {
        List<JSONObject> dimens = modelMapper.findRootDimension(modelId);
        String year = DateTimeUtils.getCurrentTime("yyyy");
        List<JSONObject> res = new LinkedList<>();
        for (JSONObject d : dimens) {
            JSONObject j = new JSONObject();
            //查询所有分行该维度成绩
            int id = d.getIntValue("id");//维度id
            List<JSONObject> list = statisticsMapper.findScoreByDimensionId(modelId, id, year);
            //获取当前分行维度成绩
            double score = 0d;//成绩
            for (int i = 0; i < list.size(); i++) {
                JSONObject json = list.get(i);
                if (name.equals(json.getString("bankName"))) {
                    score = json.getDoubleValue("score");
                    break;
                }
            }
            //计算维度排名
            String rank = "";//排名
            for (int i = 0; i < list.size(); i++) {
                JSONObject json = list.get(i);
                if (score == json.getDoubleValue("score")) {
                    rank = "第" + (i + 1);
                    break;
                }
            }
            j.put("rank", rank);
            //封装维度名称
            String dimenName = d.getString("name");
            j.put("name", dimenName);
            res.add(j);
        }
        return res;
    }

    @Override
    public JSONObject branchOverview(String modelId, String name) {
        JSONObject res = new JSONObject();
        JSONObject model = modelMapper.findModel(modelId);
        res.put("title", model.getString("title"));//评分体系
        res.put("cycleUnit", model.getString("cycleUnit"));//统计周期
        int year = Integer.parseInt(Utils.getCurrentTime("yyyy"));
        res.put("year", year + "年");//当前年度
        //评分等级
        JSONArray gradation = model.getJSONArray("gradation");
        String gra = "";
        if (gradation != null) {
            StringBuffer sb = new StringBuffer();
            List<JSONObject> array = gradation.toJavaList(JSONObject.class);
            for (JSONObject j : array) {
                sb.append(j.getString("name")).append("/");
            }
            gra = sb.substring(0, sb.length() - 1);
        }
        res.put("gradation", gra);//评分等级
        res.put("name", name);//分行名称
        //查询各维度成绩
        List<JSONObject> list = statisticsMapper.findDimensionScoreByBank(modelId, year, name);
        //封装总成绩
        double score = 0d;
        for (JSONObject j : list) {
            score += j.getDoubleValue("score");
        }
        res.put("score", score);//总成绩
        //封装当前分行等级
        String rank = "";
        if (gradation != null) {
            List<JSONObject> rule = gradation.toJavaList(JSONObject.class);//评分规则
            for (int i = 0; i < rule.size(); i++) {
                JSONObject r = rule.get(i);
                double start = r.getDoubleValue("start");
                double end = r.getDoubleValue("end");
                String rankName = r.getString("name");
                if (score >= start && score < end) {
                    rank = rankName;
                    break;
                }
                //第一个判断项包含后面的值 90 <= 优 <= 100
                if (i == 0 && score == end) {
                    rank = rankName;
                    break;
                }
            }
        }
        res.put("rank", rank);//当前分行等级
        res.put("list", list);//各维度成绩
        return res;
    }

    @Override
    public List<JSONObject> branchScoreDetail(String modelId, String name) {
        int year = Integer.parseInt(Utils.getCurrentTime("yyyy"));
        //评分细则
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
            } else {
                json.put("assess", "可提升");
            }
            detail.add(json);
        }
        detail.forEach(a -> {
            String location = a.getString("location");
            int index = location.lastIndexOf(">");
            a.put("name", location.substring(index == -1 ? 0 : index + 1));
        });
        return detail;
    }

}
