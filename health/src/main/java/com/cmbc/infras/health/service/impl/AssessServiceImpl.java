package com.cmbc.infras.health.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.health.mapper.AssessMapper;
import com.cmbc.infras.health.mapper.ModelMapper;
import com.cmbc.infras.health.rpc.ProcessEngineRpc;
import com.cmbc.infras.health.rpc.RpcUtil;
import com.cmbc.infras.health.service.AssessService;
import com.cmbc.infras.health.thread.CreateReport;
import com.cmbc.infras.health.util.ChartUtil;
import com.cmbc.infras.health.util.FileUtil;
import com.cmbc.infras.health.util.FileZipUtil;
import com.cmbc.infras.health.util.MyHeaderFooter;
import com.cmbc.infras.util.NumberUtils;
import com.cmbc.infras.util.Utils;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.io.IOUtils;
import org.jfree.chart.ChartUtils;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AssessServiceImpl implements AssessService {

    @Value("${health.document}")
    private String documentUri;

    @Value("${health.pdf}")
    private String pdfUri;

    @Resource
    private AssessService assessService;

    @Resource
    private ModelMapper modelMapper;
    @Resource
    private AssessMapper assessMapper;
    @Resource
    private ProcessEngineRpc processEngineRpc;

    @Override
    public List<JSONObject> quickFind(int isAdmin, int isAuditRole, String name, String word) {
        List<JSONObject> result;
        if (isAdmin == 1 || isAuditRole == 1) {
            result = assessMapper.adminQuickFind(word);
            for (JSONObject r : result) {
                JSONObject json = scoreRank(r.getString("model_id"));
                r.put("redNumber", json.getIntValue("totalRedNumber"));
            }
        } else {
            result = assessMapper.quickFind(name, word);
            //管理员和审核人提示未审核的记录，提交人提示待处理的记录
            for (JSONObject r : result) {
                AtomicInteger redNumber = new AtomicInteger();
                int year = Integer.parseInt(Utils.getCurrentTime("yyyy"));
                JSONObject json = assessDimension((r.getString("model_id")), name, isAdmin, isAuditRole);
                //计算当年数据
                JSONArray thisYear = json.getJSONArray(year + "");
                if (thisYear != null) {
                    List<JSONObject> list = thisYear.toJavaList(JSONObject.class);
                    list.forEach((a) -> redNumber.getAndAdd(a.getIntValue("redNumber")));
                }
                //计算次年数据
                year++;
                JSONArray nextYear = json.getJSONArray(year + "");
                if (nextYear != null) {
                    List<JSONObject> list = nextYear.toJavaList(JSONObject.class);
                    list.forEach((a) -> redNumber.getAndAdd(a.getIntValue("redNumber")));
                }
                r.put("redNumber", redNumber);
            }
        }
        return result;
    }

    @Override
    public JSONObject scoreRank(String modelId) {
        DecimalFormat df = new DecimalFormat("#0.00");
        //查询当年不同银行的成绩
        int year = Integer.parseInt(Utils.getCurrentTime("yyyy"));
        List<JSONObject> scoreRank = assessMapper.scoreRank(modelId, year);
        int totalRedNumber = 0;//测评列表用
        //未处理信息红色高亮
        for (JSONObject l : scoreRank) {
            AtomicInteger redNumber = new AtomicInteger();
            JSONObject json = assessDimension(modelId, l.getString("bankName"), 1, 1);
            //计算当年数据
            JSONArray thisYear = json.getJSONArray(year + "");
            if (thisYear != null) {
                List<JSONObject> list = thisYear.toJavaList(JSONObject.class);
                list.forEach((a) -> redNumber.getAndAdd(a.getIntValue("redNumber")));
            }
            //计算次年数据
            year++;
            JSONArray nextYear = json.getJSONArray(year + "");
            if (nextYear != null) {
                List<JSONObject> list = nextYear.toJavaList(JSONObject.class);
                list.forEach((a) -> redNumber.getAndAdd(a.getIntValue("redNumber")));
            }
            l.put("redNumber", redNumber);
            totalRedNumber += redNumber.intValue();
            //保留2位小数
            l.put("score", df.format(l.getDoubleValue("score")));
        }
        Double totalScore = assessMapper.findModelTotalScore(modelId);
        JSONObject res = new JSONObject();
        res.put("list", scoreRank);
        res.put("totalScore", totalScore);
        res.put("totalRedNumber", totalRedNumber);
        return res;
    }

    @Override
    public JSONObject historyScoreRank(String modelId, int version) {
        //找到所有组织当前version的测评
        List<Integer> listId = assessMapper.findReportByVersion(modelId, version);
        if (listId.size() != 0) {
            List<JSONObject> listReport = assessMapper.allAssessResult(listId);
            //组装成绩
            List<JSONObject> list = new LinkedList<>();
            for (JSONObject r : listReport) {
                Double score = 0D;
                List<JSONObject> dimen = r.getJSONObject("report").getJSONArray("dimension").toJavaList(JSONObject.class);
                for (JSONObject d : dimen) {
                    score += d.getDoubleValue("result");
                }
                JSONObject j = new JSONObject();
                j.put("bankName", r.getString("name"));
                j.put("score", score);
                j.put("id", r.getIntValue("id"));
                list.add(j);
            }
            //按照成绩降序
            Collections.sort(list, (o1, o2) -> {
                double val = o1.getDoubleValue("score") - o2.getDoubleValue("score");
                if (val < 0) return 1;
                if (val > 0) return -1;
                return 0;
            });
            //封装总分
            List<JSONObject> dimension = listReport.get(0).getJSONObject("report").getJSONArray("dimension").toJavaList(JSONObject.class);
            Double totalScore = 0D;
            for (JSONObject d : dimension) {
                totalScore += d.getDoubleValue("score");
            }
            //结果
            JSONObject res = new JSONObject();
            res.put("list", list);
            res.put("totalScore", totalScore);
            res.put("version", version);
            return res;
        }
        return null;
    }

    @Override
    public JSONObject assessDimension(String modelId, String name, int isAdmin, int isAuditRole) {
        JSONObject result = new JSONObject();
        //查询当年数据
        int year = Integer.parseInt(Utils.getCurrentTime("yyyy"));
        List<JSONObject> score1 = assessMapper.findScore(modelId, name, year);
        if (isAdmin == 1 || isAuditRole == 1) {
            //管理员和审核人界面，消息提示提示未审核的记录
            for (JSONObject s : score1) {
                AtomicInteger redNumber = new AtomicInteger();
                List<JSONObject> leafs = assessMapper.findLeaf(modelId, s.getString("id"));
                if (!leafs.isEmpty()) {
                    List<JSONObject> leafScores = assessMapper.findLeafScore(modelId, name, leafs, year);
                    leafScores.forEach((a) -> {
                        if ("待审核".equals(a.getString("state"))) redNumber.getAndIncrement();
                    });
                    if ("single".equals(s.getString("type")) || "deduct".equals(s.getString("type")))
                        s.put("redNumber", redNumber);
                }
            }
        } else {
            //提交人界面，消息提示提示待处理的记录
            for (JSONObject s : score1) {
                AtomicInteger redNumber = new AtomicInteger();
                List<JSONObject> leafs = assessMapper.findLeaf(modelId, s.getString("id"));
                if (!leafs.isEmpty()) {
                    List<JSONObject> leafScores = assessMapper.findLeafScore(modelId, name, leafs, year);
                    leafScores.forEach((a) -> {
                        if (a.getIntValue("auditResult") == 0) redNumber.getAndIncrement();
                    });
                    if ("single".equals(s.getString("type")) || "deduct".equals(s.getString("type")))
                        s.put("redNumber", redNumber);
                }
            }
        }
        result.put(year + "", score1);
        //查询周期起止时间,到达起始时间显示次年数据，起止周期内可以填写次年数据
        year++;
        int date = Integer.parseInt(Utils.getCurrentTime("MMdd"));
        JSONObject cycleDate = assessMapper.findCycleDate(modelId);
        int cycleStart = cycleDate.getIntValue("cycleStart");
        int cycleEnd = cycleDate.getIntValue("cycleEnd");
        if (date >= cycleStart) {
            List<JSONObject> score2 = assessMapper.findScore(modelId, name, year);
            if (date <= cycleEnd) {
                //可以编辑次年数据，提示红色消息
                result.put("allowEdit", 1);
                if (isAdmin == 1 || isAuditRole == 1) {
                    //管理员和审核人界面，消息提示提示未审核的记录
                    for (JSONObject s : score2) {
                        AtomicInteger redNumber1 = new AtomicInteger();
                        List<JSONObject> leafs = assessMapper.findLeaf(modelId, s.getString("id"));
                        if (!leafs.isEmpty()) {
                            List<JSONObject> leafScores = assessMapper.findLeafScore(modelId, name, leafs, year);
                            leafScores.forEach((a) -> {
                                if ("待审核".equals(a.getString("state"))) redNumber1.getAndIncrement();
                            });
                            if ("single".equals(s.getString("type")) || "deduct".equals(s.getString("type")))
                                s.put("redNumber", redNumber1);
                        }
                    }
                } else {
                    //提交人界面，消息提示提示待处理的记录
                    for (JSONObject s : score2) {
                        AtomicInteger redNumber1 = new AtomicInteger();
                        List<JSONObject> leafs = assessMapper.findLeaf(modelId, s.getString("id"));
                        if (!leafs.isEmpty()) {
                            List<JSONObject> leafScores = assessMapper.findLeafScore(modelId, name, leafs, year);
                            leafScores.forEach((a) -> {
                                if (a.getIntValue("auditResult") == 0) redNumber1.getAndIncrement();
                            });
                            if ("single".equals(s.getString("type")) || "deduct".equals(s.getString("type")))
                                s.put("redNumber", redNumber1);
                        }
                    }
                }
            } else {
                result.put("allowEdit", 0);//不可编辑次年数据
            }
            result.put(year + "", score2);
        }
        return result;
    }

    @Override
    public List<JSONObject> record(String modelId, String name, int id, int year) {
        List<JSONObject> list = assessMapper.findRecord(modelId, name, id, year);
        return list;
    }

    @Override
    public List<JSONObject> findSingle(String modelId, String id, String name, int year) {
        //查询子叶维度
        List<JSONObject> leaf = assessMapper.findLeaf(modelId, id);
        for (JSONObject jo : leaf) {
            int leafId = jo.getIntValue("id");
            //得分项
            List<JSONObject> option = assessMapper.findOption(name, leafId);
            List<JSONObject> document = new LinkedList<>();
            for (JSONObject jsonObject : option) {
                if (0 != jsonObject.getIntValue("ifDocument")) {
                    JSONObject j = new JSONObject();
                    int optionId = jsonObject.getIntValue("id");
                    j.put("optionId", optionId);
                    j.put("optionName", jsonObject.getString("name"));
                    //根据optionId查询所有证明文件
                    List<JSONObject> list = assessMapper.findDocumentById(optionId);
                    if (list.size() != 0) j.put("list", list);
                    document.add(j);
                }
            }
            jo.put("option", option);
            jo.put("document", document);
            //维度分数和选项序号
            JSONObject result = assessMapper.findResult(leafId, name, year);
            jo.put("sort", result.getString("sort") == null ? null : result.getIntValue("sort"));
            jo.put("result", result.getDoubleValue("score"));
            jo.put("isRed", result.getIntValue("auditResult") == 0 ? 1 : 0);
            jo.put("state", result.getString("state"));
            jo.put("advice", result.getString("advice"));
        }
        return leaf;
    }

    @Override
    public BaseResult<String> uploadDocument(MultipartFile file, int id) {
        String date = Utils.getCurrentTime("yyyyMMdd");
        String fileName = file.getOriginalFilename();
        String newName = UUID.randomUUID().toString() + fileName.substring(fileName.lastIndexOf("."));//服务器文件名
        String filePath = documentUri + "/" + date + "/" + id;
        boolean b = FileUtil.uploadDocument(file, newName, filePath);
        if (!b) return BaseResult.fail("上传" + fileName + "失败");
        String url = filePath + "/" + newName;
        return BaseResult.success(url);
    }

    private BaseResult<String> ensureDocument(int id, List<JSONObject> urls) {
        for (JSONObject u : urls) {
            String fileName = u.getString("fileName");
            String url = u.getString("url");
            assessMapper.insertUrl(id, url, fileName);
        }
        return BaseResult.success("成功");
    }

    @Override
    public void downloadDocument(HttpServletResponse response, String document, String fileName) {
        try {
            //将文件进行下载
            OutputStream out = response.getOutputStream();
            File file = new File(document);
            byte[] data = Files.readAllBytes(file.toPath());//服务器存储地址
            response.reset();
            response.setContentType("application/msexcel;charset=UTF-8");
            response.setHeader("Content-disposition", "attachment; fileName=" + URLEncoder.encode(fileName, "UTF-8"));
            response.addHeader("Content-Length", "" + data.length);
            response.setContentType("application/octet-stream;charset=UTF-8");
            IOUtils.write(data, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void commitSingle(JSONObject param) {
        int ifCommit = param.getIntValue("ifCommit");
        String name = param.getString("name");
        int year = param.getIntValue("year");
        List<JSONObject> list = param.getJSONArray("list").toJavaList(JSONObject.class);
        //做处理记录
        String modelId = param.getString("modelId");
        int dimensionId = param.getIntValue("rootId");
        String bankName = param.getString("name");
        String type = ifCommit == 0 ? "保存" : "提交";
        String handler = param.getString("userName");
        String detail = "";
        for (JSONObject l : list) {
            int id = l.getIntValue("id");//维度id
            int newSort = l.getIntValue("sort");//新选项
            JSONObject res = assessMapper.findResult(id, name, year);
            Integer oldSort = res.getInteger("sort");//旧选项
            String dimensionName = assessMapper.findDimensionName(id);//维度名称
            String newName = assessMapper.findSingleSortName(id, newSort);
            //旧选项为null时
            if (oldSort == null) {
                detail += "修改了 '" + dimensionName + "' ，由 '未选择' 改为 '" + newName + "' ；\n";
            } else {
                String oldName = assessMapper.findSingleSortName(id, oldSort);
                //如果前后选项不一样做记录
                if (newSort != oldSort) {
                    detail += "修改了 '" + dimensionName + "' ，由 '" + oldName + "' 改为 '" + newName + "' ；\n";
                }
            }
        }
        assessMapper.makeRecord(modelId, dimensionId, bankName, type, handler, detail, year);
        //提交逻辑
        JSONObject dimension = modelMapper.findDimensionById(param.getIntValue("rootId"));
        String state = ifCommit == 0 ? "待完善" : dimension.getIntValue("ifAudit") == 0 ? "自动" : "待审核";//状态
        for (JSONObject j : list) {
            //登记审核结果，1表示提交人已保存或提交，0表示未操作过
            j.put("auditResult", 1);
            //如果sort为null，设置result为0
            if (j.getInteger("sort") == null || ifCommit == 0) j.put("result", 0);
        }
        assessMapper.commitSingle(year, name, list, state);
        //证明材料上传确定
        for (JSONObject l : list) {
            JSONArray document = l.getJSONArray("document");
            if (document.size() != 0) {
                List<JSONObject> listDoc = document.toJavaList(JSONObject.class);
                for (JSONObject doc : listDoc) {
                    ensureDocument(doc.getIntValue("id"), doc.getJSONArray("urls").toJavaList(JSONObject.class));
                }
            }
        }
        //计算成绩，查询所有子叶维度成绩总和，更新根维度成绩
        String rootId = param.getString("rootId");
        List<JSONObject> listLeaf = assessMapper.findLeaf(modelId, rootId);
        Double score = assessMapper.selectSumByList(name, year, listLeaf);
        assessMapper.updateResult(rootId, name, year, score, state);
    }

    @Override
    public BaseResult<String> deleteDocument(int id, String document) {
        boolean b = FileUtil.DeleteFolder(document);
        if (b) {
            assessMapper.deleteDocument(id);
            return BaseResult.success("删除成功");
        }
        return BaseResult.fail("删除失败");
    }

    @Override
    public List<JSONObject> findSingleAudit(String modelId, String id, String name, int year) {
        //判断维度状态，为“待完善”时，不展示描述、材料、实际得分
        String state = assessMapper.findState(modelId, id, name, year);
        //查询子叶维度、得分项
        List<JSONObject> leaf = assessMapper.findLeaf(modelId, id);
        for (JSONObject jo : leaf) {
            JSONObject description = assessMapper.findDescription(jo.getIntValue("id"), name, year);
            if ("待完善".equals(state)) {
                jo.put("description", "未提交");
                jo.put("document", "未提交");
                //审核人员不能提交审核
                jo.put("commitState", 0);
            } else {
                if (null != description) {
                    jo.put("description", description.getString("name"));
                    List<JSONObject> list = assessMapper.findDocumentById(description.getIntValue("id"));
                    jo.put("document", list);
                } else {
                    jo.put("description", "未选择");
                    jo.put("document", "未提交");
                }
                //审核人员能提交审核
                jo.put("commitState", 1);
            }
            //查询该维度得分分数和选项序号
            JSONObject result = assessMapper.findResult(jo.getIntValue("id"), name, year);
            jo.put("auditResult", result.getIntValue("auditResult"));
            jo.put("sort", result.getString("sort") == null ? null : result.getIntValue("sort"));
            //单个维度状态
            String state1 = result.getString("state");
            jo.put("state", state1);
            jo.put("advice", result.getString("advice"));
            jo.put("isRed", "待审核".equals(state1) ? 1 : 0);//待审核的标红
            if ("待完善".equals(state1)) {
                jo.put("result", 0);
            } else {
                jo.put("result", result.getDoubleValue("score"));
            }
        }
        return leaf;
    }

    @Override
    public void commitSingleAudit(JSONObject param) {
        String modelId = param.getString("modelId");
        String name = param.getString("name");
        int year = param.getIntValue("year");
        List<JSONObject> list = param.getJSONArray("list").toJavaList(JSONObject.class);
        assessMapper.commitAudit(name, year, list);
        //计算成绩，查询所有子叶维度成绩总和，更新根维度成绩
        String state = "已审核";
        String rootId = param.getString("rootId");
        List<JSONObject> listLeaf = assessMapper.findLeaf(modelId, rootId);
        Double score = assessMapper.selectSumByList(name, year, listLeaf);
        assessMapper.updateResult(rootId, name, year, score, state);
        //做处理记录
        int dimensionId = param.getIntValue("rootId");
        String bankName = param.getString("name");
        String type = "审核";
        String handler = param.getString("userName");
        String detail = "";
        for (JSONObject l : list) {
            int id = l.getIntValue("id");//维度id
            String dimensionName = assessMapper.findDimensionName(id);//维度名称
            int a = l.getIntValue("auditResult");//审核结果
            String auditResult = a == 1 ? "合格" : "不合格";
            String advice = l.getString("advice") == null ? "无" : l.getString("advice");//处理建议
            detail += "'" + dimensionName + "' 审核结果为 '" + auditResult + "' ，处理建议：" + advice + "；\n";
        }
        assessMapper.makeRecord(modelId, dimensionId, bankName, type, handler, detail, year);
    }

    @Override
    public List<JSONObject> findDeduct(String modelId, String id, String name, int year) {
        //查询子叶维度
        List<JSONObject> leaf = assessMapper.findLeaf(modelId, id);
        for (JSONObject jo : leaf) {
            int leafId = jo.getIntValue("id");
            //查询日期范围内的扣分项
            JSONObject deductLeaf = modelMapper.findDeductDimensionByName(leafId, name);
            if (null != deductLeaf) {
                String dateStart = year + "-" + deductLeaf.getString("dateStart");
                String dateEnd = year + "-" + deductLeaf.getString("dateEnd");
                List<JSONObject> deduct = assessMapper.findDeductByDate(name, leafId, year);
                for (JSONObject j : deduct) {
                    j.remove("location");
                }
                jo.put("deduct", deduct);
                //扣分项维度结构
                jo.put("timeScope", dateStart + "至" + dateEnd);
                jo.put("resourceName", deductLeaf.getJSONObject("source").getString("name"));
                jo.put("resourceId", deductLeaf.getJSONObject("source").getString("resourceId"));
                jo.put("chooseState", deductLeaf.getJSONArray("chooseState"));
                jo.put("chooseCondition", deductLeaf.getJSONArray("chooseCondition"));
                jo.put("calculationRule", deductLeaf.getString("calculationRule").split("#")[0]);
                jo.put("standardValue", deductLeaf.getIntValue("standardValue"));
                //维度分数和选项序号
                JSONObject result = assessMapper.findResult(leafId, name, year);
                jo.put("sort", result.getIntValue("sort"));
                jo.put("result", result.getDoubleValue("score"));
                jo.put("isRed", result.getIntValue("auditResult") == 0 ? 1 : 0);
                jo.put("state", result.getString("state"));
                jo.put("advice", result.getString("advice"));
            }
        }
        return leaf;
    }

    @Override
    public List<JSONObject> getSourceData(JSONObject params) throws Exception {
        //根据数据源、起止时间、状态、筛选条件获取扣分项列表
        JSONObject param = new JSONObject();//请求参数
        List<JSONObject> chooseState = params.getJSONArray("chooseState").toJavaList(JSONObject.class);
        String timeScope = params.getString("timeScope");
        //组装参数
        param.put("configDataId", params.getString("resourceId"));
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
        //暂时取消日期过滤
//        Field_xxx_create_time.put("startTime", timeScope.substring(0, 10));
//        Field_xxx_create_time.put("endTime", timeScope.substring(11, 21));
        search.put("Field_xxx_create_time", Field_xxx_create_time);
        query.put("search", search);
        param.put("query", query);
        String cookie = RpcUtil.getCookie();
        String result = processEngineRpc.getFormData(cookie, param);
        JSONObject json = JSONObject.parseObject(result);
        if (!"200".equals(json.getString("status")))
            throw new Exception("查询流程引擎失败，接口：/api/flow/api/v1/bfm/instances/model/list，返回信息：" + json.toJSONString());
        List<JSONObject> data = json.getJSONObject("data").getJSONArray("instancesData").toJavaList(JSONObject.class);
        //有筛选条件进行筛选
        JSONArray chooseCondition = params.getJSONArray("chooseCondition");
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
        //封装数据
        List<JSONObject> option = new LinkedList<>();
        for (JSONObject d : data) {
            JSONObject jo = new JSONObject();
            jo.put("bankName", d.getString("Field_xxx_create_dept") == null ? "" : d.getString("Field_xxx_create_dept"));
            jo.put("name", d.getString("Field_xxx_title"));
            jo.put("billId", d.getString("id"));
            jo.put("createTime", d.getString("Field_xxx_create_time").substring(0, 10));
            jo.put("state", d.getString("status"));
            option.add(jo);
        }
        //筛选出当前组织的数据
        String name = params.getString("name");
        List<JSONObject> valid = new LinkedList<>();
        for (JSONObject j : option) {
            if (name.equals(j.getString("bankName"))) valid.add(j);
        }
        return valid;
    }

    private void addSourceData(int dimensionId, String name, int year, List<JSONObject> list) {
        if (list.size() != 0) {
            HashMap<String, JSONObject> map = new HashMap<>();//存放新数据
            for (JSONObject j : list) {
                map.put(j.getString("billId"), j);
            }
            //查询旧数据，覆盖相同billId的数据，新增不存在的数据
            List<JSONObject> listDeduct = assessMapper.findDeduct(name, dimensionId, year);
            List<JSONObject> needUpdate = new LinkedList<>();//需要更新的数据
            List<JSONObject> needInsert = new LinkedList<>();//需要新增的数据
            for (JSONObject j : listDeduct) {
                String billId = j.getString("billId");
                if (map.containsKey(billId)) {
                    JSONObject data = map.get(billId);
                    j.put("billId", data.getString("billId"));
                    j.put("name", data.getString("name"));
                    j.put("createTime", data.getString("createTime"));
                    j.put("state", data.getString("state"));
                    j.put("year", year);
                    needUpdate.add(j);
                    map.remove(billId);
                }
            }
            //需要新增的数据
            JSONObject temp = modelMapper.findDeductDimension(dimensionId);
            map.forEach((k, v) -> {
                JSONObject t = new JSONObject();
                t.put("bankName", name);
                t.put("dimensionId", dimensionId);
                t.put("billId", v.getString("billId"));
                t.put("name", v.getString("name"));
                t.put("location", temp.getString("source"));
                t.put("createTime", v.getString("createTime"));
                t.put("state", v.getString("state"));
                t.put("year", year);
                needInsert.add(t);
            });
            //更新数据、新增数据
            if (needUpdate.size() != 0) assessMapper.updateSourceData(needUpdate);
            if (needInsert.size() != 0) assessMapper.addSourceData(needInsert);
        }
    }

    @Override
    public void deleteSourceData(JSONObject param) {
        assessMapper.deleteSourceData(param.getIntValue("id"));
    }

    @Override
    @Transactional
    public void commitDeduct(JSONObject param) {
        int ifCommit = param.getIntValue("ifCommit");
        String name = param.getString("name");
        int year = param.getIntValue("year");
        List<JSONObject> list = param.getJSONArray("list").toJavaList(JSONObject.class);
        JSONObject dimension = modelMapper.findDimensionById(param.getIntValue("rootId"));
        //做处理记录
        String modelId = param.getString("modelId");
        int dimenId = param.getIntValue("rootId");
        String bankName = param.getString("name");
        String type = ifCommit == 0 ? "保存" : "提交";
        String handler = param.getString("userName");
        String detail = "";
        for (JSONObject l : list) {
            int id = l.getIntValue("id");//维度id
            //维度名称
            String dimensionName = assessMapper.findDimensionName(id);
            //原来全部单据
            List<JSONObject> bills = assessMapper.findDeductList(bankName, id);
            StringBuilder oldBill = new StringBuilder();//原来单据
            for (int i = 0; i < bills.size(); i++) {
                oldBill.append("'").append(bills.get(i).getString("name")).append("'");
                if (i != bills.size() - 1) oldBill.append("，");
            }
            //更新单据信息，查询最新全部单据
            JSONArray array = l.getJSONArray("list");
            StringBuilder newBill = new StringBuilder();//最新单据
            if (array.size() != 0) {
                List<JSONObject> bill = array.toJavaList(JSONObject.class);
                for (int i = 0; i < bill.size(); i++) {
                    newBill.append("'").append(bill.get(i).getString("name")).append("'");
                    if (i != bill.size() - 1) newBill.append("，");
                }
                addSourceData(id, name, year, array.toJavaList(JSONObject.class));
            }
            if (!oldBill.toString().equals(newBill.toString()))
                detail += "修改了 '" + dimensionName + "' ，由 [" + oldBill + "] 改为 [" + newBill + "] ；\n";
        }
        assessMapper.makeRecord(modelId, dimenId, bankName, type, handler, detail, year);
        //修改状态
        String state;
        if (ifCommit == 0) {
            state = "待完善";
        } else {
            if (dimension.getIntValue("ifAudit") == 0) {
                state = "自动";
            } else {
                state = "待审核";
            }
        }
        //计算单条成绩
        List<JSONObject> listResult = new LinkedList<>();
        for (JSONObject json : list) {
            int dimensionId = json.getIntValue("id");
            JSONObject deductDimension = modelMapper.findDeductDimension(dimensionId);
            Double score = modelMapper.findDimensionScore(dimensionId);
            Double deduct = 0D;
            int count = assessMapper.findDeductCount(name, json.getIntValue("id"));//单据总数
            List<JSONObject> rule = deductDimension.getJSONArray("rule").toJavaList(JSONObject.class);//计算规则
            String[] calculationRule = deductDimension.getString("calculationRule").split("#");
            if (calculationRule[0].equals("计数")) {
                for (JSONObject r : rule) {
                    int start = r.getIntValue("start");
                    int end = r.getString("end").equals("*") || r.getString("end").equals("∞") ? Integer.MAX_VALUE : r.getIntValue("end");
                    if (count >= start && count < end) {
                        deduct = r.getDouble("score");
                        break;
                    }
                }
            }
            if (calculationRule[0].equals("百分比")) {
                List<JSONObject> condition = JSONArray.parseArray(calculationRule[1]).toJavaList(JSONObject.class);
                Double validCount = assessMapper.findConditionDeductCount(name, dimensionId, condition).getDouble("count");
                if (count != 0) {
                    Double rate = validCount / count;
                    for (int i = 0; i < rule.size(); i++) {
                        JSONObject r = rule.get(i);
                        Double start = r.getDouble("start") / 100;
                        Double end = r.getDouble("end") / 100;
                        if (rate >= start && rate < end) {
                            deduct = r.getDouble("score");
                            break;
                        }
                        //最后一个判断项包含后面的值
                        if (i == rule.size() - 1) {
                            if (rate >= start && rate <= end) {
                                deduct = r.getDouble("score");
                                break;
                            }
                        }
                    }
                }
            }
            if (calculationRule[0].equals("单独计数")) {
                DecimalFormat df = new DecimalFormat("#0.00");
                double standardValue = json.getDoubleValue("standardValue");//标准值
                if (standardValue == 0 || count >= standardValue) {
                    deduct = 0d;
                } else {
                    Double rate = count / standardValue;//得分率
                    deduct = Double.valueOf(df.format(score * (1 - rate)));
                }
            }
            JSONObject j = new JSONObject();
            j.put("id", dimensionId);
            j.put("score", ifCommit == 0 ? 0 : score - deduct);
            j.put("auditResult", 1);
            j.put("name", name);
            j.put("year", year);
            listResult.add(j);
            assessMapper.updateScore(listResult, state);
        }
        //计算成绩，查询所有子叶维度成绩总和，更新根维度成绩
        String rootId = param.getString("rootId");
        List<JSONObject> listLeaf = assessMapper.findLeaf(modelId, rootId);
        Double score = assessMapper.selectSumByList(name, year, listLeaf);
        assessMapper.updateResult(rootId, name, year, score, state);
    }

    @Override
    public List<JSONObject> findDeductAudit(String modelId, String id, String name, int year) {
        //判断维度状态，为“待完善”时，不展示完成情况、实际得分
        String state = assessMapper.findState(modelId, id, name, year);
        //查询子叶维度
        List<JSONObject> leaf = assessMapper.findLeaf(modelId, id);
        for (JSONObject jo : leaf) {
            int leafId = jo.getIntValue("id");
            //查询日期范围内的扣分项
            JSONObject deductLeaf = modelMapper.findDeductDimensionByName(leafId, name);
            if (null != deductLeaf) {
                String dateStart = year + "-" + deductLeaf.getString("dateStart");
                String dateEnd = year + "-" + deductLeaf.getString("dateEnd");
                List<JSONObject> deduct = assessMapper.findDeductByDate(name, leafId, year);
                for (JSONObject j : deduct) {
                    j.put("location", j.getJSONObject("location"));
                }
                if ("待完善".equals(state)) {
                    jo.put("deduct", "未提交");
                    //审核人员不能提交审核
                    jo.put("commitState", 0);
                } else {
                    jo.put("deduct", deduct);
                    //审核人员能提交审核
                    jo.put("commitState", 1);
                }
                //时间范围和所选状态
                jo.put("timeScope", dateStart + "至" + dateEnd);
                jo.put("chooseState", deductLeaf.getJSONArray("chooseState"));
                jo.put("chooseCondition", deductLeaf.getJSONArray("chooseCondition"));
                jo.put("calculationRule", deductLeaf.getString("calculationRule").split("#")[0]);
                jo.put("standardValue", deductLeaf.getIntValue("standardValue"));
                //维度分数和选项序号
                JSONObject result = assessMapper.findResult(leafId, name, year);
                jo.put("auditResult", result.getIntValue("auditResult"));
                jo.put("sort", result.getIntValue("sort"));
                //单个维度状态
                String state1 = result.getString("state");
                jo.put("state", state1);
                jo.put("advice", result.getString("advice"));
                jo.put("isRed", "待审核".equals(state1) ? 1 : 0);//待审核的标红
                if ("待完善".equals(state1)) {
                    jo.put("result", 0);
                } else {
                    jo.put("result", result.getDoubleValue("score"));
                }
            }
        }
        return leaf;
    }

    @Override
    @Transactional
    public void updateFormData(JSONObject param) throws Exception {
        String bankName = param.getString("name");
        int dimensionId = param.getIntValue("id");
        int year = param.getIntValue("year");
        //删除当前银行该维度的表单数据
        assessMapper.deleteFormDataByNameAndDimension(bankName, dimensionId, year);
        //查询最新表单数据，全部添加入库
        JSONObject param1 = param.getJSONObject("param");
        List<JSONObject> list = assessService.getSourceData(param1);
        List<JSONObject> needInsert = new LinkedList<>();
        JSONObject temp = modelMapper.findDeductDimension(dimensionId);
        list.forEach(v -> {
            JSONObject t = new JSONObject();
            t.put("bankName", bankName);
            t.put("dimensionId", dimensionId);
            t.put("billId", v.getString("billId"));
            t.put("name", v.getString("name"));
            t.put("location", temp.getString("source"));
            t.put("createTime", v.getString("createTime"));
            t.put("state", v.getString("state"));
            t.put("year", year);
            needInsert.add(t);
        });
        //新增数据
        if (needInsert.size() != 0) assessMapper.addSourceData(needInsert);
    }

    @Override
    public void saveStandardValue(JSONObject param) {
        assessMapper.updateStandardValue(param.getString("name"), param.getJSONArray("list").toJavaList(JSONObject.class));
    }

    @Override
    public void commitDeductAudit(JSONObject param) {
        String modelId = param.getString("modelId");
        String name = param.getString("name");
        int year = param.getIntValue("year");
        List<JSONObject> list = param.getJSONArray("list").toJavaList(JSONObject.class);
        assessMapper.commitAudit(name, year, list);
        //计算成绩，查询所有子叶维度成绩总和，更新根维度成绩
        String state = "已审核";
        String rootId = param.getString("rootId");
        List<JSONObject> listLeaf = assessMapper.findLeaf(modelId, rootId);
        Double score = assessMapper.selectSumByList(name, year, listLeaf);
        assessMapper.updateResult(rootId, name, year, score, state);
        //做处理记录
        int dimensionId = param.getIntValue("rootId");
        String bankName = param.getString("name");
        String type = "审核";
        String handler = param.getString("userName");
        String detail = "";
        for (JSONObject l : list) {
            int id = l.getIntValue("id");//维度id
            String dimensionName = assessMapper.findDimensionName(id);//维度名称
            int a = l.getIntValue("auditResult");//审核结果
            String auditResult = a == 1 ? "合格" : "不合格";
            String advice = l.getString("advice") == null ? "无" : l.getString("advice");//处理建议
            detail += "'" + dimensionName + "' 审核结果为 '" + auditResult + "' ，处理建议：" + advice + "；\n";
        }
        assessMapper.makeRecord(modelId, dimensionId, bankName, type, handler, detail, year);
        //更新标准值
        assessMapper.updateStandardValue(name, list);
    }

    @Override
    public List<JSONObject> findMonitor(String modelId, String id, String name, int year) {
        //查询子叶维度
        List<JSONObject> leaf = assessMapper.findLeaf(modelId, id);
        for (JSONObject jo : leaf) {
            int leafId = jo.getIntValue("id");
            List<JSONObject> monitor = modelMapper.findMonitor(leafId);
            jo.put("rule", monitor.get(0).getJSONArray("rule"));
            //维度分数和选项序号
            JSONObject res = assessMapper.findResult(leafId, name, year);
            jo.put("result", res.getDoubleValue("score"));
            //数据详情：1，获取测点id、获取实时值
            JSONObject monitor1 = modelMapper.findMonitorByIdAndBankName(leafId, name);
            if (monitor1 == null) continue;//说明当前银行没有此项测评
            JSONObject param = new JSONObject();
            param.put("resources", monitor1.getJSONArray("resources"));
            String result = processEngineRpc.getSpotData(InfrasConstant.KE_RPC_COOKIE, param);
            JSONObject json = JSONObject.parseObject(result);
            //2，封装数据
            JSONObject detail = new JSONObject();//数据详情
            List<JSONObject> data = new LinkedList<>();//测点id及实时值
            Double average = 0D;//平均值
            if ("00".equals(json.getString("error_code"))) {
                //存储测点值
                List<JSONObject> resources = JSONObject.parseObject(json.getString("data")).getJSONArray("resources").toJavaList(JSONObject.class);
                List<Double> value = new LinkedList<>();
                resources.forEach((a) -> {
                    String realValue = a.getString("real_value");
                    if (NumberUtils.isNumeric(realValue)) value.add(Double.parseDouble(realValue));
                    //封装data
                    JSONObject j = new JSONObject();
                    String resourceId = a.getString("resource_id");
                    j.put("spotId", resourceId);
                    j.put("realValue", realValue);
                    data.add(j);
                });
                //计算平均值
                for (Double v : value) {
                    average += v;
                }
                if (value.size() != 0) average /= value.size();
            }
            detail.put("data", data);
            DecimalFormat df = new DecimalFormat("#0.00");
            detail.put("average", df.format(average));
            jo.put("detail", detail);
        }
        return leaf;
    }

    @Override
    public List<JSONObject> findAnalysis(String modelId, String id, String name, int year) {
        //查询子叶维度
        List<JSONObject> leaf = assessMapper.findLeaf(modelId, id);
        for (JSONObject jo : leaf) {
            int leafId = jo.getIntValue("id");
            JSONObject analysis = modelMapper.findAnalysis(leafId);
            jo.put("rule", analysis.getJSONArray("rule"));
            //维度分数和选项序号
            JSONObject result = assessMapper.findResult(leafId, name, year);
            jo.put("result", result.getDoubleValue("score"));
        }
        return leaf;
    }

    @Override
    public List<JSONObject> historyQuickFind(String name, String word, int start, int pageSize) {
        return assessMapper.historyQuickFind(name, word, start, pageSize);
    }

    @Override
    public List<JSONObject> adminHistoryQuickFind(String word, int start, int pageSize) {
        List<String> allModelId = assessMapper.findAllModelId();
        if (allModelId.size() == 0) return new LinkedList<>();
        return assessMapper.adminHistoryQuickFind(allModelId, word, start, pageSize);
    }

    @Override
    public List<JSONObject> getAssessTotal(String name, String word, int start, int pageSize) {
        return assessMapper.getAssessTotal(name, word, start, pageSize);
    }

    @Override
    public List<JSONObject> adminGetAssessTotal(String word, int start, int pageSize) {
        List<String> allModelId = assessMapper.findAllModelId();
        if (allModelId.size() == 0) return new LinkedList<>();
        return assessMapper.adminGetAssessTotal(allModelId, word, start, pageSize);
    }

    @Override
    public JSONObject assessResult(int id) throws Exception {
        JSONObject report = assessMapper.assessResult(id);
        if (null == report) throw new Exception("未查询到此报告");
        report.put("report", JSONObject.parseObject(report.getString("report")));
        return report;
    }

    @Override
    public void checkReport(HttpServletResponse response, String modelId, String name) {
        try {
            //生成一份报告
            Thread thread = new Thread(new CreateReport(modelId, modelMapper, assessMapper, true));
            thread.start();
            thread.join(0);
            int year = Integer.parseInt(Utils.getCurrentTime("yyyy"));
            int id = 0;
            List<JSONObject> reportId = assessMapper.findReportId(modelId, name, year);
            if (reportId.size() != 0) id = reportId.get(0).getIntValue("id");
            JSONObject report = assessMapper.assessResult(id);
            if (report != null) {
                String fileName = report.getString("title") + "-" + Utils.getCurrentTime("yyyyMMdd") + ".pdf";
                String url = makePdf(report);
                OutputStream out = response.getOutputStream();
                File file = new File(url);
                byte[] data = Files.readAllBytes(file.toPath());//服务器存储地址
                response.reset();
                response.setContentType("application/msexcel;charset=UTF-8");
                response.setHeader("Content-disposition", "attachment; fileName=" + URLEncoder.encode(fileName, "UTF-8"));
                response.addHeader("Content-Length", "" + data.length);
                response.setContentType("application/octet-stream;charset=UTF-8");
                IOUtils.write(data, out);
                out.flush();
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void correctExport(HttpServletResponse response, JSONObject param) {
        JSONObject report = param.getJSONObject("report");
        if (report != null) {
            String fileName = report.getString("title") + "-" + Utils.getCurrentTime("yyyyMMdd") + ".pdf";
            report.put("report", report.getJSONObject("report").toJSONString());
            String url = makePdf(report);
            //将文件进行下载
            try {
                OutputStream out = response.getOutputStream();
                File file = new File(url);
                byte[] data = Files.readAllBytes(file.toPath());//服务器存储地址
                response.reset();
                response.setContentType("application/msexcel;charset=UTF-8");
                response.setHeader("Content-disposition", "attachment; fileName=" + URLEncoder.encode(fileName, "UTF-8"));
                response.addHeader("Content-Length", "" + data.length);
                response.setContentType("application/octet-stream;charset=UTF-8");
                IOUtils.write(data, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void export(HttpServletResponse response, int id) {
        JSONObject report = assessMapper.assessResult(id);
        if (report != null) {
            String fileName = report.getString("title") + "-" + Utils.getCurrentTime("yyyyMMdd") + ".pdf";
            String url = makePdf(report);
            //将文件进行下载
            try {
                OutputStream out = response.getOutputStream();
                File file = new File(url);
                byte[] data = Files.readAllBytes(file.toPath());//服务器存储地址
                response.reset();
                response.setContentType("application/msexcel;charset=UTF-8");
                response.setHeader("Content-disposition", "attachment; fileName=" + URLEncoder.encode(fileName, "UTF-8"));
                response.addHeader("Content-Length", "" + data.length);
                response.setContentType("application/octet-stream;charset=UTF-8");
                IOUtils.write(data, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String makePdf(JSONObject report) {
        JSONObject data = JSONObject.parseObject(report.getString("report"));
        List<JSONObject> dimension = JSONArray.parseArray(data.getString("dimension")).toJavaList(JSONObject.class);
        JSONObject analysis = JSONObject.parseObject(data.getString("analysis"));
        List<JSONObject> detail = JSONArray.parseArray(data.getString("detail")).toJavaList(JSONObject.class);
        String name = report.getString("name");
        String title = report.getString("title");
        String createTime = report.getString("createTime");
        createTime = createTime.substring(0, 10);
        String date = Utils.getCurrentTime("yyyyMMdd");
        String url = pdfUri + "/" + date + "/" + "/" + title + "/" + name + "-" + title + "-" + date + ".pdf";
        try {
            Document doc = new Document();
            File dirFile = new File(url);
            if (!dirFile.getParentFile().exists()) dirFile.getParentFile().mkdirs();
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(dirFile));
            writer.setPageEvent(new MyHeaderFooter());//页眉/页脚
            doc.open();
            for (int i = 0; i < 10; i++) {
                doc.add(new Paragraph(" "));
            }
            //中文字体
            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            //字体模板
            Font titleFont = new Font(bfChinese, 26, Font.BOLD);
            Font firstFont = new Font(bfChinese, 14, Font.BOLD);
            Font secondFont = new Font(bfChinese, 10, Font.NORMAL);
            Font blueFont = new Font(bfChinese, 10, Font.NORMAL);
            blueFont.setColor(BaseColor.BLUE);
            Font greenFont = new Font(bfChinese, 10, Font.NORMAL);
            greenFont.setColor(BaseColor.GREEN);
            Font redFont = new Font(bfChinese, 10, Font.NORMAL);
            redFont.setColor(BaseColor.RED);
            //第一页
            Paragraph pa11 = new Paragraph(name + "-" + title, titleFont);
            pa11.setAlignment(1);
            doc.add(pa11);
            Paragraph pa12 = new Paragraph(createTime, titleFont);
            pa12.setAlignment(1);
            doc.add(pa12);
            //第二页 测评维度
            doc.newPage();
            Paragraph pa21 = new Paragraph("测评维度", firstFont);
            pa21.setAlignment(0);
            doc.add(pa21);
            doc.add(new Paragraph(" ", secondFont));
            //测评维度表格
            int dimensionColumn = 4;//列数
            PdfPTable dimensionTable = new PdfPTable(dimensionColumn);
            int[] cellsWidth = new int[]{2, 1, 1, 1};//定义表格的宽度
            dimensionTable.setWidths(cellsWidth);//单元格宽度
            dimensionTable.setWidthPercentage(100);//表格的宽度百分比
            dimensionTable.getDefaultCell().setPadding(2);//单元格的间隔
            dimensionTable.getDefaultCell().setBorderWidth(2);//边框宽度
            dimensionTable.getDefaultCell().setBackgroundColor(BaseColor.WHITE);//设置表格底色
            dimensionTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            //单元格模板
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBackgroundColor(BaseColor.LIGHT_GRAY);//亮灰色
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);//水平居中
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);//垂直居中
            PdfPCell textCell = new PdfPCell();
            textCell.setHorizontalAlignment(Element.ALIGN_CENTER);//水平居中
            textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);//垂直居中
            for (int i = 0; i < dimensionColumn; i++) {
                if (i == 0) {
                    titleCell.setPhrase(new Paragraph("维度", secondFont));
                    dimensionTable.addCell(titleCell);
                }
                if (i == 1) {
                    titleCell.setPhrase(new Paragraph("总分值", secondFont));
                    dimensionTable.addCell(titleCell);
                }
                if (i == 2) {
                    titleCell.setPhrase(new Paragraph("实际得分", secondFont));
                    dimensionTable.addCell(titleCell);
                }
                if (i == 3) {
                    titleCell.setPhrase(new Paragraph("得分率", secondFont));
                    dimensionTable.addCell(titleCell);
                }
            }
            for (JSONObject json : dimension) {
                for (int i = 0; i < dimensionColumn; i++) {
                    if (i == 0) {
                        textCell.setPhrase(new Paragraph(json.getString("name"), secondFont));
                        dimensionTable.addCell(textCell);
                    }
                    if (i == 1) {
                        textCell.setPhrase(new Paragraph(json.getString("score"), secondFont));
                        dimensionTable.addCell(textCell);
                    }
                    if (i == 2) {
                        textCell.setPhrase(new Paragraph(json.getString("result"), secondFont));
                        dimensionTable.addCell(textCell);
                    }
                    if (i == 3) {
                        textCell.setPhrase(new Paragraph(json.getString("rate"), secondFont));
                        dimensionTable.addCell(textCell);
                    }
                }
            }
            doc.add(dimensionTable);
            doc.add(new Paragraph(" ", titleFont));
            //测评分析
            Paragraph pa22 = new Paragraph("测评分析", firstFont);
            pa22.setAlignment(0);
            doc.add(pa22);
            doc.add(new Paragraph(" ", secondFont));
            Chunk chunk1 = new Chunk("共有评分项目数：", secondFont);
            Chunk chunkBlue = new Chunk(analysis.getString("total"), blueFont);
            Chunk chunk2 = new Chunk("                                             已满足评分要求项目数：", secondFont);
            Chunk chunkGreen = new Chunk(analysis.getString("valid"), greenFont);
            Chunk chunk3 = new Chunk("                                             未满足评分要求项目数：", secondFont);
            Chunk chunkRed = new Chunk(analysis.getString("fail"), redFont);
            Paragraph pa221 = new Paragraph();
            pa221.add(chunk1);
            pa221.add(chunkBlue);
            pa221.add(chunk2);
            pa221.add(chunkGreen);
            pa221.add(chunk3);
            pa221.add(chunkRed);
            pa221.setAlignment(1);
            doc.add(pa221);
            doc.add(new Paragraph(" ", titleFont));
            //测评详情
            Paragraph pa23 = new Paragraph("测评详情", firstFont);
            pa23.setAlignment(0);
            doc.add(pa23);
            doc.add(new Paragraph(" ", secondFont));
            //测评详情表格
            int detailColumn = 6;//列数
            PdfPTable detailTable = new PdfPTable(detailColumn);
            int[] cellsWidth2 = new int[]{1, 4, 2, 1, 1, 1};//定义表格的宽度
            detailTable.setWidths(cellsWidth2);//单元格宽度
            detailTable.setWidthPercentage(100);//表格的宽度百分比
            detailTable.getDefaultCell().setPadding(2);//单元格的间隔
            detailTable.getDefaultCell().setBorderWidth(2);//边框宽度
            detailTable.getDefaultCell().setBackgroundColor(BaseColor.WHITE);//设置表格底色
            detailTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            for (int i = 0; i < detailColumn; i++) {
                if (i == 0) {
                    titleCell.setPhrase(new Paragraph("序号", secondFont));
                    detailTable.addCell(titleCell);
                }
                if (i == 1) {
                    titleCell.setPhrase(new Paragraph("评分维度", secondFont));
                    detailTable.addCell(titleCell);
                }
                if (i == 2) {
                    titleCell.setPhrase(new Paragraph("评分类型", secondFont));
                    detailTable.addCell(titleCell);
                }
                if (i == 3) {
                    titleCell.setPhrase(new Paragraph("总分", secondFont));
                    detailTable.addCell(titleCell);
                }
                if (i == 4) {
                    titleCell.setPhrase(new Paragraph("得分", secondFont));
                    detailTable.addCell(titleCell);
                }
                if (i == 5) {
                    titleCell.setPhrase(new Paragraph("评价", secondFont));
                    detailTable.addCell(titleCell);
                }
            }
            int row = 1;
            for (JSONObject json : detail) {
                for (int i = 0; i < detailColumn; i++) {
                    if (i == 0) {
                        textCell.setPhrase(new Paragraph(String.valueOf(row++), secondFont));
                        detailTable.addCell(textCell);
                    }
                    if (i == 1) {
                        textCell.setPhrase(new Paragraph(json.getString("location"), secondFont));
                        detailTable.addCell(textCell);
                    }
                    if (i == 2) {
                        textCell.setPhrase(new Paragraph(json.getString("type"), secondFont));
                        detailTable.addCell(textCell);
                    }
                    if (i == 3) {
                        textCell.setPhrase(new Paragraph(json.getString("score"), secondFont));
                        detailTable.addCell(textCell);
                    }
                    if (i == 4) {
                        textCell.setPhrase(new Paragraph(json.getString("result"), secondFont));
                        detailTable.addCell(textCell);
                    }
                    if (i == 5) {
                        String assess = json.getString("assess");
                        if ("已满足".equals(assess)) {
                            textCell.setPhrase(new Paragraph(assess, greenFont));
                            detailTable.addCell(textCell);
                        } else {
                            textCell.setPhrase(new Paragraph(assess, redFont));
                            detailTable.addCell(textCell);
                        }
                    }
                }
            }
            doc.add(detailTable);
            //关闭文档
            doc.close();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    @Override
    public void exportZipNow(HttpServletResponse response, String modelId) {
        try {
            //生成一次报告
            Thread thread = new Thread(new CreateReport(modelId, modelMapper, assessMapper, true));
            thread.start();
            thread.join(0);
            //查询最新version
            int version;
            JSONObject reportVersion = modelMapper.findReportVersion(modelId);
            if (reportVersion == null) {
                version = 0;
            } else {
                version = reportVersion.getIntValue("version");
            }
            //导出
            List<Integer> listId = assessMapper.findReportByVersion(modelId, version);
            if (listId.size() != 0) {
                List<JSONObject> listReport = assessMapper.allAssessResult(listId);
                for (JSONObject jsonObject : listReport) {
                    makePdf(jsonObject);
                }
                String title = listReport.get(0).getString("title");
                //生成机构排名列表、排名柱状图pdf
                int year = Integer.parseInt(Utils.getCurrentTime("yyyy"));
                List<JSONObject> scoreRank = assessMapper.scoreRank(modelId, year);
                makeScoreRank(scoreRank, title);
                //打包地址
                String date = Utils.getCurrentTime("yyyyMMdd");
                String url = pdfUri + "/" + date + "/" + title;
                //将文件进行打包下载
                OutputStream out = response.getOutputStream();
                byte[] data = FileZipUtil.createZip(url);//服务器存储地址
                response.reset();
                response.setHeader("Content-Disposition", "attachment;fileName=" + modelId + "-" + version + "-" + date + ".zip");
                response.addHeader("Content-Length", "" + data.length);
                response.setContentType("application/octet-stream;charset=UTF-8");
                IOUtils.write(data, out);
                out.flush();
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exportZip(HttpServletResponse response, String modelId, Integer version) {
        //找到所有组织当前version的测评并生成pdf
        List<Integer> listId = assessMapper.findReportByVersion(modelId, version);
        if (listId.size() != 0) {
            List<JSONObject> listReport = assessMapper.allAssessResult(listId);
            for (JSONObject jsonObject : listReport) {
                makePdf(jsonObject);
            }
            String title = listReport.get(0).getString("title");
            //生成机构排名列表、排名柱状图pdf
            int year = Integer.parseInt(Utils.getCurrentTime("yyyy"));
            List<JSONObject> scoreRank = assessMapper.scoreRank(modelId, year);
            makeScoreRank(scoreRank, title);
            //打包地址
            String date = Utils.getCurrentTime("yyyyMMdd");
            String url = pdfUri + "/" + date + "/" + title;
            //将文件进行打包下载
            try {
                OutputStream out = response.getOutputStream();
                byte[] data = FileZipUtil.createZip(url);//服务器存储地址
                response.reset();
                response.setHeader("Content-Disposition", "attachment;fileName=" + modelId + "-" + version + "-" + date + ".zip");
                response.addHeader("Content-Length", "" + data.length);
                response.setContentType("application/octet-stream;charset=UTF-8");
                IOUtils.write(data, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void makeScoreRank(List<JSONObject> scoreRank, String title) {
        //文件地址
        String date = Utils.getCurrentTime("yyyyMMdd");
        String url = pdfUri + "/" + date + "/" + title + "/" + "评分排名-" + date + ".pdf";
        try {
            Document doc = new Document();
            File dirFile = new File(url);
            if (!dirFile.getParentFile().exists()) dirFile.getParentFile().mkdirs();
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(dirFile));
            doc.open();
            //中文字体
            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            //字体模板
            Font firstFont = new Font(bfChinese, 14, Font.BOLD);
            Font secondFont = new Font(bfChinese, 10, Font.NORMAL);
            Font blueFont = new Font(bfChinese, 10, Font.NORMAL);
            blueFont.setColor(BaseColor.BLUE);
            Font greenFont = new Font(bfChinese, 10, Font.NORMAL);
            greenFont.setColor(BaseColor.GREEN);
            Font redFont = new Font(bfChinese, 10, Font.NORMAL);
            redFont.setColor(BaseColor.RED);
            //单元格模板
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBackgroundColor(BaseColor.LIGHT_GRAY);//亮灰色
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);//水平居中
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);//垂直居中
            PdfPCell textCell = new PdfPCell();
            textCell.setHorizontalAlignment(Element.ALIGN_CENTER);//水平居中
            textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);//垂直居中
            //测评详情
            Paragraph pa = new Paragraph("评分排名", firstFont);
            pa.setAlignment(0);
            doc.add(pa);
            doc.add(new Paragraph(" ", secondFont));
            //测评详情表格
            int detailColumn = 3;//列数
            PdfPTable detailTable = new PdfPTable(detailColumn);
            int[] cellsWidth = new int[]{1, 1, 1};//定义表格的宽度
            detailTable.setWidths(cellsWidth);//单元格宽度
            detailTable.setWidthPercentage(100);//表格的宽度百分比
            detailTable.getDefaultCell().setPadding(2);//单元格的间隔
            detailTable.getDefaultCell().setBorderWidth(2);//边框宽度
            detailTable.getDefaultCell().setBackgroundColor(BaseColor.WHITE);//设置表格底色
            detailTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            for (int i = 0; i < detailColumn; i++) {
                if (i == 0) {
                    titleCell.setPhrase(new Paragraph("序号", secondFont));
                    detailTable.addCell(titleCell);
                }
                if (i == 1) {
                    titleCell.setPhrase(new Paragraph("分行名称", secondFont));
                    detailTable.addCell(titleCell);
                }
                if (i == 2) {
                    titleCell.setPhrase(new Paragraph("得分", secondFont));
                    detailTable.addCell(titleCell);
                }
            }
            int row = 1;
            for (JSONObject json : scoreRank) {
                for (int i = 0; i < detailColumn; i++) {
                    if (i == 0) {
                        textCell.setPhrase(new Paragraph(String.valueOf(row++), secondFont));
                        detailTable.addCell(textCell);
                    }
                    if (i == 1) {
                        textCell.setPhrase(new Paragraph(json.getString("bankName"), secondFont));
                        detailTable.addCell(textCell);
                    }
                    if (i == 2) {
                        textCell.setPhrase(new Paragraph(json.getString("score"), secondFont));
                        detailTable.addCell(textCell);
                    }
                }
            }
            doc.add(detailTable);
            //柱状图
            doc.add(new Paragraph(" ", secondFont));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            String year = Utils.getCurrentTime("yyyy");
            for (JSONObject j : scoreRank) {
                dataset.addValue(j.getDoubleValue("score"), year, j.getString("bankName"));
            }
            ChartUtils.writeChartAsJPEG(bos, ChartUtil.barChart("评分排名柱状图", "", "分数", dataset),
                    1050, 525);
            Image image = Image.getInstance(bos.toByteArray());
            image.scalePercent(50);
            doc.add(image);
            //关闭文档
            doc.close();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        JSONObject j = new JSONObject();
        String date = "2021-11-12";
        j.put("date", date);
    }

}
