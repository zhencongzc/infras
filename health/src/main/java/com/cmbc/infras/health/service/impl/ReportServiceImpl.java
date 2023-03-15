package com.cmbc.infras.health.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.health.contant.*;
import com.cmbc.infras.health.dto.FormRequestParam;
import com.cmbc.infras.health.dto.ReportRequestParam;
import com.cmbc.infras.health.mapper.AssessMapper;
import com.cmbc.infras.health.mapper.ModelMapper;
import com.cmbc.infras.health.rpc.ProcessEngineRpc;
import com.cmbc.infras.health.rpc.RpcUtil;
import com.cmbc.infras.health.service.ReportService;
import com.cmbc.infras.health.thread.AlarmCallable;
import com.cmbc.infras.health.util.ChartUtil;
import com.cmbc.infras.health.util.ExcelUtils;
import com.cmbc.infras.health.util.MD5;
import com.cmbc.infras.health.util.ThreadPoolUtil;
import jxl.Workbook;
import jxl.write.*;
import org.apache.commons.io.IOUtils;
import org.jfree.chart.ChartUtils;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
public class ReportServiceImpl implements ReportService {

    @Value("${health.report}")
    private String reportUri;

    @Resource
    private ModelMapper modelMapper;
    @Resource
    private AssessMapper assessMapper;
    @Resource
    private ProcessEngineRpc processEngineRpc;

    @Override
    public JSONObject findBank(int isAdmin, String name) throws Exception {
        JSONObject res = new JSONObject();
        JSONObject param = new JSONObject();
        param.put("orgId", 1);
        String result = processEngineRpc.getOrganizationAndRole(param);
        JSONObject json = JSONObject.parseObject(result);
        if (!"200".equals(json.getString("code")))
            throw new Exception("查询流程引擎失败，接口：/api/admin/rpc/dept/getTreeWithUser，返回信息：" + json.toJSONString());
        List<JSONObject> data = json.getJSONArray("data").toJavaList(JSONObject.class);
        //参与的组织
        JSONObject organization = data.get(0);
        List<JSONObject> children = organization.getJSONArray("children").toJavaList(JSONObject.class);
        for (JSONObject child : children) {
            child.remove("children");
        }
        //找到名称为name的分行，如果是非管理员，只返回本分行信息
        if (isAdmin == 0) {
            for (JSONObject child : children) {
                if (name.equals(child.getString("name"))) {
                    organization.put("children", child);
                    res.put("organization", organization);
                    return res;
                }
            }
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
        return res;
    }

    @Override
    public List<JSONObject> energyStatistics(JSONObject jsonObject) throws Exception {
        //开始日期
        String dateStart = jsonObject.getString("dateStart");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        long start = simpleDateFormat.parse(dateStart).getTime() / 1000;
        //结束日期
        String dateEnd = jsonObject.getString("dateEnd");
        long end = simpleDateFormat.parse(dateEnd).getTime() / 1000;
        List<JSONObject> organization = jsonObject.getJSONArray("organization").toJavaList(JSONObject.class);
        //返回数据
        List<JSONObject> res = new LinkedList<>();
        for (JSONObject organ : organization) {
            JSONObject bank = new JSONObject();
            bank.put("name", organ.getString("name"));
            //获取实时PUE、总功率、IT设施总功率的测点值
            FormRequestParam param = new FormRequestParam(organ.getString("id"));
            String cookie = RpcUtil.getCookie();
            String result = processEngineRpc.getInfrastructureData(cookie, param);
            JSONObject json = JSONObject.parseObject(result);
            if (!"200".equals(json.getString("status")))
                throw new Exception("查询流程引擎失败，接口：/api/flow/api/v1/bfm/instances/model/list，返回信息：" + json.toJSONString());
            List<JSONObject> data = json.getJSONObject("data").getJSONArray("instancesData").toJavaList(JSONObject.class);
            String pueSpot = "";
            String allPowerSpot = "";
            String ITPowerSpot = "";
            for (JSONObject j : data) {
                JSONArray field_xxx_zgxlfpa = j.getJSONArray("Field_xxx_zgxlfpa");
                if (null != field_xxx_zgxlfpa) {
                    //测点列表
                    List<JSONObject> spots = field_xxx_zgxlfpa.toJavaList(JSONObject.class);
                    for (JSONObject spot : spots) {
                        String type = spot.getString("spot_name");
                        if (SpotTypeEnum.belongSpotType(type)) {
                            if (type.equals(SpotTypeEnum.PUE.getDesc())) pueSpot = spot.getString("spot_id");
                            if (type.equals(SpotTypeEnum.ALL_POWER.getDesc())) allPowerSpot = spot.getString("spot_id");
                            if (type.equals(SpotTypeEnum.IT_POWER.getDesc())) ITPowerSpot = spot.getString("spot_id");
                        }
                    }

                }
            }
            //访问KE，根据日期、测点值获取每天的平均值数据
            List<String> ids = new LinkedList<>();
            ids.add(pueSpot);
            ids.add(allPowerSpot);
            ids.add(ITPowerSpot);
            ReportRequestParam param1 = new ReportRequestParam(start, end, ids);
            String str = JSON.toJSONString(param1);
            String check = MD5.getUpperMD5(str);
            String result1 = processEngineRpc.findData(InfrasConstant.KE_RPC_COOKIE, check, str);
            JSONObject json1 = JSONObject.parseObject(result1);
            if (!"00".equals(json1.getString("error_code")))
                throw new Exception("查询KE失败，接口：/api/v3/tsdb/orig/query_agg，返回信息：" + json1.toJSONString());
            JSONArray jsonArray = json1.getJSONObject("data").getJSONArray("time_series");
            if (jsonArray != null) {
                List<JSONObject> timeSeries = jsonArray.toJavaList(JSONObject.class);
                DecimalFormat df = new DecimalFormat("0.00");
                for (JSONObject timeSery : timeSeries) {
                    String name = timeSery.getString("name");
                    if (name.equals(pueSpot)) {
                        //计算平均PUE值
                        List<JSONArray> points = timeSery.getJSONArray("points").toJavaList(JSONArray.class);
                        double sum = 0;
                        for (JSONArray point : points) {
                            sum += point.getDouble(2);
                        }
                        bank.put("pue", df.format(sum / points.size()));
                    }
                    if (name.equals(allPowerSpot)) {
                        //计算平均总用电量
                        List<JSONArray> points = timeSery.getJSONArray("points").toJavaList(JSONArray.class);
                        double sum = 0;
                        for (JSONArray point : points) {
                            sum += point.getDouble(2);
                        }
                        bank.put("allPower", df.format(sum * 24 / 10000));
                    }
                    if (name.equals(ITPowerSpot)) {
                        //计算平均IT用电量
                        List<JSONArray> points = timeSery.getJSONArray("points").toJavaList(JSONArray.class);
                        double sum = 0;
                        for (JSONArray point : points) {
                            sum += point.getDouble(2);
                        }
                        bank.put("ITPower", df.format(sum * 24 / 10000));
                    }
                }
                bank.put("otherPower", df.format(bank.getDoubleValue("allPower") - bank.getDoubleValue("ITPower")));
            }
            res.add(bank);
        }
        //按照PUE升序排序
        Collections.sort(res, (o1, o2) -> {
            double val = o1.getDoubleValue("pue") - o2.getDoubleValue("pue");
            if (val < 0) return -1;
            if (val > 0) return 1;
            return 0;
        });
        return res;
    }

    @Override
    public void exportEnergy(HttpServletResponse response, JSONObject param) throws Exception {
        List<JSONObject> jsonObjects = energyStatistics(param);
        String dateStart = param.getString("dateStart");
        String dateEnd = param.getString("dateEnd");
        //生成excel
        File xlsFile = new File(reportUri + "/" + "能耗统计报表-" + dateStart + "-" + dateEnd + ".xls");
        if (!xlsFile.getParentFile().exists()) xlsFile.getParentFile().mkdirs();
        WritableWorkbook workbook;
        workbook = Workbook.createWorkbook(xlsFile);
        WritableSheet sheet = workbook.createSheet("能耗统计", 0);
        //定义单元格样式
        WritableCellFormat titleFormat = ExcelUtils.getFormat(0);
        WritableCellFormat headFormat = ExcelUtils.getFormat(1);
        WritableCellFormat tableFormat = ExcelUtils.getFormat(2);
        int row = 0;
        String[] title = new String[]{"分行名称", "PUE平均值", "总用电量平均值/万kwh", "IT用电量平均值/万kwh", "其他用电量平均值/万kwh"};
        for (int col = 0; col < 5; col++) {
            sheet.addCell(new Label(col, row, title[col], titleFormat));
        }
        row = 1;
        for (JSONObject j : jsonObjects) {
            for (int col = 0; col < 5; col++) {
                if (col == 0) sheet.addCell(new Label(col, row, j.getString("name"), headFormat));
                if (col == 1) sheet.addCell(new Label(col, row, j.getString("pue"), tableFormat));
                if (col == 2) sheet.addCell(new Label(col, row, j.getString("allPower"), tableFormat));
                if (col == 3) sheet.addCell(new Label(col, row, j.getString("ITPower"), tableFormat));
                if (col == 4) sheet.addCell(new Label(col, row, j.getString("otherPower"), tableFormat));
            }
            row++;
        }
        //数据统计
        WritableSheet sheet1 = workbook.createSheet("数据统计", 1);
        //统计报表折线图
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (JSONObject j : jsonObjects) {
            dataset.addValue(j.getDoubleValue("pue"), "PUE平均值    ", j.getString("name"));
            dataset.addValue(j.getDoubleValue("allPower"), "总用电量平均值/万kwh    ", j.getString("name"));
            dataset.addValue(j.getDoubleValue("ITPower"), "IT用电量平均值/万kwh    ", j.getString("name"));
            dataset.addValue(j.getDoubleValue("otherPower"), "其他用电量平均值/万kwh    ", j.getString("name"));
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsJPEG(bos, ChartUtil.lineChart("能耗统计报表", "", "", dataset),
                4000, 1000);
        WritableImage image = new WritableImage(2, 4, 20, 25, bos.toByteArray());
        sheet1.addImage(image);
        workbook.write();
        workbook.close();
        //将文件进行下载
        OutputStream out = response.getOutputStream();
        byte[] data = Files.readAllBytes(xlsFile.toPath());//服务器存储地址
        response.reset();
        response.setContentType("application/msexcel;charset=UTF-8");
        response.setHeader("Content-disposition", "attachment; fileName=" + URLEncoder.encode(xlsFile.getName(), "UTF-8"));
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream;charset=UTF-8");
        IOUtils.write(data, out);
        out.flush();
        out.close();
    }

    @Override
    public List<JSONObject> temperatureHumidity(JSONObject jsonObject) throws Exception {
        //开始日期
        String dateStart = jsonObject.getString("dateStart");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        long start = simpleDateFormat.parse(dateStart).getTime() / 1000;
        //结束日期
        String dateEnd = jsonObject.getString("dateEnd");
        long end = simpleDateFormat.parse(dateEnd).getTime() / 1000;
        List<JSONObject> organization = jsonObject.getJSONArray("organization").toJavaList(JSONObject.class);
        //返回数据
        List<JSONObject> res = new LinkedList<>();
        for (JSONObject organ : organization) {
            //获取实时PUE、总功率、IT设施总功率的测点值
            FormRequestParam param = new FormRequestParam(organ.getString("id"));
            String cookie = RpcUtil.getCookie();
            String result = processEngineRpc.getInfrastructureData(cookie, param);
            JSONObject json = JSONObject.parseObject(result);
            if (!"200".equals(json.getString("status")))
                throw new Exception("查询流程引擎失败，接口：/api/flow/api/v1/bfm/instances/model/list，返回信息：" + json.toJSONString());
            List<JSONObject> data = json.getJSONObject("data").getJSONArray("instancesData").toJavaList(JSONObject.class);
            List<String> listTem = new LinkedList<>();//所有温度测点
            List<String> listHum = new LinkedList<>();//所有湿度测点
            for (JSONObject j : data) {
                JSONArray field_xxx_zgxlfpa = j.getJSONArray("Field_xxx_zgxlfpa");
                if (null != field_xxx_zgxlfpa) {
                    //测点列表
                    List<JSONObject> spots = field_xxx_zgxlfpa.toJavaList(JSONObject.class);
                    for (JSONObject spot : spots) {
                        String type = spot.getString("spot_name");
                        if (SpotTypeEnum.belongSpotType(type)) {
                            if (type.equals(SpotTypeEnum.TEMPERATURE.getDesc())) listTem.add(spot.getString("spot_id"));
                            if (type.equals(SpotTypeEnum.HUMIDITY.getDesc())) listHum.add(spot.getString("spot_id"));
                        }
                    }

                }
            }
            //访问KE，根据日期、测点值获取每天的平均值数据
            List<String> ids = new LinkedList<>();
            ids.addAll(listTem);
            ids.addAll(listHum);
            ReportRequestParam param1 = new ReportRequestParam(start, end, ids);
            String str = JSON.toJSONString(param1);
            String check = MD5.getUpperMD5(str);
            String result1 = processEngineRpc.findData(InfrasConstant.KE_RPC_COOKIE, check, str);
            JSONObject json1 = JSONObject.parseObject(result1);
            if (!"00".equals(json1.getString("error_code")))
                throw new Exception("查询KE失败，接口：/api/v3/tsdb/orig/query_agg，返回信息：" + json1.toJSONString());
            JSONArray jsonArray = json1.getJSONObject("data").getJSONArray("time_series");
            if (jsonArray != null) {
                DecimalFormat df = new DecimalFormat("0.00");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                List<JSONObject> timeSeries = jsonArray.toJavaList(JSONObject.class);
                HashMap<String, List<JSONArray>> mapTimeSeries = new HashMap<>();//测点id:points数组
                timeSeries.forEach((a) -> mapTimeSeries.put(a.getString("name"), a.getJSONArray("points").toJavaList(JSONArray.class)));
                List<JSONObject> list = new LinkedList<>();//存放结果
                int dateCount = timeSeries.get(0).getJSONArray("points").size();//天数
                for (int i = 0; i < dateCount; i++) {
                    JSONObject j = new JSONObject();
                    //计算所有测点的平均温度
                    List<Double> sumTem = new LinkedList<>();
                    long dateNum = 0;
                    for (String s : listTem) {
                        List<JSONArray> points = mapTimeSeries.get(s);
                        if (points != null) {
                            if (i < points.size()) {
                                sumTem.add(points.get(i).getDoubleValue(2));
                                dateNum = points.get(i).getLongValue(0);
                            }
                        }
                    }
                    double temp = 0D;
                    for (Double v : sumTem) {
                        temp += v;
                    }
                    if (sumTem.size() != 0) j.put("temperature", df.format(temp / sumTem.size()));
                    //计算所有测点的平均湿度
                    List<Double> sumHum = new LinkedList<>();
                    for (String s : listHum) {
                        List<JSONArray> points = mapTimeSeries.get(s);
                        if (points != null) {
                            if (i < points.size()) {
                                sumHum.add(points.get(i).getDouble(2));
                                dateNum = points.get(i).getLongValue(0);
                            }
                        }
                    }
                    temp = 0;
                    for (Double v : sumHum) {
                        temp += v;
                    }
                    j.put("humidity", df.format(temp / sumHum.size()));
                    j.put("id", organ.getIntValue("id"));
                    j.put("name", organ.getString("name"));
                    j.put("date", sdf.format(dateNum * 1000));
                    list.add(j);
                }
                res.addAll(list);
            }
        }
        //按照日期升序排序
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Collections.sort(res, (o1, o2) -> {
            try {
                Date date1 = sdf.parse(o1.getString("date"));
                Date date2 = sdf.parse(o2.getString("date"));
                boolean val = date1.before(date2);
                if (val) {
                    return -1;
                }
                return 1;
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return -1;
        });
        return res;
    }

    @Override
    public void exportTemperatureHumidity(HttpServletResponse response, JSONObject jsonObject) throws Exception {
        //开始日期
        String dateStart = jsonObject.getString("dateStart");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        long start = simpleDateFormat.parse(dateStart).getTime() / 1000;
        //结束日期
        String dateEnd = jsonObject.getString("dateEnd");
        long end = simpleDateFormat.parse(dateEnd).getTime() / 1000;
        List<JSONObject> organization = jsonObject.getJSONArray("organization").toJavaList(JSONObject.class);
        //返回数据
        List<JSONObject> res = new LinkedList<>();
        for (JSONObject organ : organization) {
            JSONObject bank = new JSONObject();
            bank.put("name", organ.getString("name"));
            //获取实时PUE、总功率、IT设施总功率的测点值
            FormRequestParam param = new FormRequestParam(organ.getString("id"));
            String cookie = RpcUtil.getCookie();
            String result = processEngineRpc.getInfrastructureData(cookie, param);
            JSONObject json = JSONObject.parseObject(result);
            if (!"200".equals(json.getString("status")))
                throw new Exception("查询流程引擎失败，接口：/api/flow/api/v1/bfm/instances/model/list，返回信息：" + json.toJSONString());
            List<JSONObject> data = json.getJSONObject("data").getJSONArray("instancesData").toJavaList(JSONObject.class);
            List<String> listTem = new LinkedList<>();
            List<String> listHum = new LinkedList<>();
            for (JSONObject j : data) {
                JSONArray field_xxx_zgxlfpa = j.getJSONArray("Field_xxx_zgxlfpa");
                if (null != field_xxx_zgxlfpa) {
                    //测点列表
                    List<JSONObject> spots = field_xxx_zgxlfpa.toJavaList(JSONObject.class);
                    for (JSONObject spot : spots) {
                        String type = spot.getString("spot_name");
                        if (SpotTypeEnum.belongSpotType(type)) {
                            if (type.equals(SpotTypeEnum.TEMPERATURE.getDesc())) listTem.add(spot.getString("spot_id"));
                            if (type.equals(SpotTypeEnum.HUMIDITY.getDesc())) listHum.add(spot.getString("spot_id"));
                        }
                    }

                }
            }
            //访问KE，根据日期、测点值获取每天的平均值数据
            List<String> ids = new LinkedList<>();
            ids.addAll(listTem);
            ids.addAll(listHum);
            ReportRequestParam param1 = new ReportRequestParam(start, end, ids);
            String str = JSON.toJSONString(param1);
            String check = MD5.getUpperMD5(str);
            String result1 = processEngineRpc.findData(InfrasConstant.KE_RPC_COOKIE, check, str);
            JSONObject json1 = JSONObject.parseObject(result1);
            if (!"00".equals(json1.getString("error_code")))
                throw new Exception("查询KE失败，接口：/api/v3/tsdb/orig/query_agg，返回信息：" + json1.toJSONString());
            JSONArray jsonArray = json1.getJSONObject("data").getJSONArray("time_series");
            if (jsonArray != null) {
                DecimalFormat df = new DecimalFormat("0.00");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                List<JSONObject> timeSeries = jsonArray.toJavaList(JSONObject.class);
                HashMap<String, List<JSONArray>> mapTimeSeries = new HashMap<>();//测点id:points数组
                timeSeries.forEach((a) -> mapTimeSeries.put(a.getString("name"), a.getJSONArray("points").toJavaList(JSONArray.class)));
                List<JSONObject> list = new LinkedList<>();//存放结果
                int dateCount = timeSeries.get(0).getJSONArray("points").size();//天数
                for (int i = 0; i < dateCount; i++) {
                    JSONObject j = new JSONObject();
                    //计算所有测点的平均温度
                    List<Double> sumTem = new LinkedList<>();
                    long dateNum = 0;
                    for (String s : listTem) {
                        List<JSONArray> points = mapTimeSeries.get(s);
                        if (points != null) {
                            if (i < points.size()) {
                                sumTem.add(points.get(i).getDoubleValue(2));
                                dateNum = points.get(i).getLongValue(0);
                            }
                        }
                    }
                    double temp = 0D;
                    for (Double v : sumTem) {
                        temp += v;
                    }
                    if (sumTem.size() != 0) j.put("temperature", df.format(temp / sumTem.size()));
                    //计算所有测点的平均湿度
                    List<Double> sumHum = new LinkedList<>();
                    for (String s : listHum) {
                        List<JSONArray> points = mapTimeSeries.get(s);
                        if (points != null) {
                            if (i < points.size()) {
                                sumHum.add(points.get(i).getDouble(2));
                                dateNum = points.get(i).getLongValue(0);
                            }
                        }
                    }
                    temp = 0;
                    for (Double v : sumHum) {
                        temp += v;
                    }
                    j.put("humidity", df.format(temp / sumHum.size()));
                    j.put("id", organ.getIntValue("id"));
                    j.put("name", organ.getString("name"));
                    j.put("date", sdf.format(dateNum * 1000));
                    list.add(j);
                }
                bank.put("list", list);
            }
            res.add(bank);
        }
        //开始导出
        List<JSONObject> jsonObjects = res;
        //生成excel
        File xlsFile = new File(reportUri + "/" + "温湿度报表-" + dateStart + "-" + dateEnd + ".xls");
        if (!xlsFile.getParentFile().exists()) xlsFile.getParentFile().mkdirs();
        WritableWorkbook workbook;
        workbook = Workbook.createWorkbook(xlsFile);
        WritableSheet sheet = workbook.createSheet("温湿度", 0);
        //定义单元格样式
        WritableCellFormat titleFormat = ExcelUtils.getFormat(0);
        WritableCellFormat headFormat = ExcelUtils.getFormat(1);
        WritableCellFormat tableFormat = ExcelUtils.getFormat(2);
        //标签和日期
        int col = 0;
        int row;
        List<JSONObject> list = jsonObjects.get(0).getJSONArray("list").toJavaList(JSONObject.class);
        String[] title = new String[]{"分行名称", "数据"};
        for (row = 0; row < 2; row++) {
            sheet.addCell(new Label(col, row, title[row], titleFormat));
        }
        for (JSONObject j : list) {
            sheet.addCell(new Label(col, row++, j.getString("date"), headFormat));
        }
        col = 1;
        for (JSONObject j : jsonObjects) {
            row = 0;
            sheet.addCell(new Label(col, row, j.getString("name"), titleFormat));
            sheet.addCell(new Label(col + 1, row++, j.getString("name"), titleFormat));
            sheet.mergeCells(col, row - 1, col + 1, row - 1);
            JSONArray list1 = j.getJSONArray("list");
            if (list1 != null) {
                List<JSONObject> data = list1.toJavaList(JSONObject.class);
                sheet.addCell(new Label(col, row++, "温度/℃", titleFormat));
                for (JSONObject d : data) {
                    sheet.addCell(new Label(col, row++, d.getString("temperature"), tableFormat));
                }
                col++;
                row = 1;
                sheet.addCell(new Label(col, row++, "湿度/%RH", titleFormat));
                for (JSONObject d : data) {
                    sheet.addCell(new Label(col, row++, d.getString("humidity"), tableFormat));
                }
                col++;
            } else {
                col += 2;
            }
        }
        workbook.write();
        workbook.close();
        //将文件进行下载
        OutputStream out = response.getOutputStream();
        byte[] data = Files.readAllBytes(xlsFile.toPath());//服务器存储地址
        response.reset();
        response.setContentType("application/msexcel;charset=UTF-8");
        response.setHeader("Content-disposition", "attachment; fileName=" + URLEncoder.encode(xlsFile.getName(), "UTF-8"));
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream;charset=UTF-8");
        IOUtils.write(data, out);
        out.flush();
        out.close();
    }

    @Override
    public List<JSONObject> healthModel(JSONObject param) {
        return modelMapper.findAllModel();
    }

    @Override
    public List<JSONObject> healthScore(JSONObject param) throws Exception {
        List<JSONObject> organization = param.getJSONArray("organization").toJavaList(JSONObject.class);
        HashMap<String, Integer> nameId = new HashMap<>();
        organization.forEach((a) -> {
            nameId.put(a.getString("name"), a.getIntValue("id"));
        });
        String modelId = param.getString("modelId");
        Calendar calendar = new GregorianCalendar();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date dateStart = sdf.parse(param.getString("dateStart") + " 00:00:00");
        Date dateEnd = sdf.parse(param.getString("dateEnd") + " 00:00:00");
        //结束日期加1天
        calendar.setTime(dateEnd);
        calendar.add(calendar.DATE, 1);
        dateEnd = calendar.getTime();
        //返回数据
        List<JSONObject> result = new LinkedList<>();
        while (!dateStart.equals(dateEnd)) {
            String start = sdf.format(dateStart);
            //日期加1天
            calendar.setTime(dateStart);
            calendar.add(calendar.DATE, 1);
            dateStart = calendar.getTime();
            String end = sdf.format(dateStart);
            //获取每天第一条数据，封装
            List<JSONObject> list = assessMapper.findReportByDate(modelId, start, end, organization);
            list.forEach((a) -> {
                JSONObject report = a.getJSONObject("report");
                report.remove("detail");
                report.remove("analysis");
                List<JSONObject> dimension = report.getJSONArray("dimension").toJavaList(JSONObject.class);
                Double totalScore = 0D;
                for (JSONObject d : dimension) {
                    totalScore += d.getDouble("result");
                }
                a.remove("report");
                a.put("totalScore", totalScore);
                a.put("detail", report);
                a.put("date", start.substring(0, 10));
                a.put("id", nameId.get(a.getString("bankName")));
            });
            result.addAll(list);
        }
        //按照总分降序
        Collections.sort(result, (o1, o2) -> {
            double val = o1.getDoubleValue("totalScore") - o2.getDoubleValue("totalScore");
            if (val < 0) return 1;
            if (val > 0) return -1;
            return 0;
        });
        return result;
    }

    @Override
    public void exportHealthScore(HttpServletResponse response, JSONObject param) throws Exception {
        List<JSONObject> organization = param.getJSONArray("organization").toJavaList(JSONObject.class);
        String modelId = param.getString("modelId");
        Calendar calendar = new GregorianCalendar();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date dateStart = sdf.parse(param.getString("dateStart") + " 00:00:00");
        Date dateEnd = sdf.parse(param.getString("dateEnd") + " 00:00:00");
        //结束日期加1天
        calendar.setTime(dateEnd);
        calendar.add(calendar.DATE, 1);
        dateEnd = calendar.getTime();
        //返回数据
        List<JSONObject> result = new LinkedList<>();
        while (!dateStart.equals(dateEnd)) {
            JSONObject j = new JSONObject();
            String start = sdf.format(dateStart);
            //日期加1天
            calendar.setTime(dateStart);
            calendar.add(calendar.DATE, 1);
            dateStart = calendar.getTime();
            String end = sdf.format(dateStart);
            j.put("date", start.substring(0, 10));
            //获取每天第一条数据，封装
            List<JSONObject> list = assessMapper.findReportByDate(modelId, start, end, organization);
            list.forEach((a) -> {
                JSONObject report = a.getJSONObject("report");
                report.remove("detail");
                report.remove("analysis");
                List<JSONObject> dimension = report.getJSONArray("dimension").toJavaList(JSONObject.class);
                Double totalScore = 0D;
                for (JSONObject d : dimension) {
                    totalScore += d.getDouble("result");
                }
                a.remove("report");
                a.put("totalScore", totalScore);
                a.put("detail", report);
            });
            j.put("list", list);
            result.add(j);
        }
        //开始导出
        List<JSONObject> jsonObjects = result;
        String dateStart1 = param.getString("dateStart");
        String dateEnd1 = param.getString("dateEnd");
        //生成excel
        File xlsFile = new File(reportUri + "/" + "健康评分报表-" + modelId + "-" + dateStart1 + "-" + dateEnd1 + ".xls");
        if (!xlsFile.getParentFile().exists()) xlsFile.getParentFile().mkdirs();
        WritableWorkbook workbook;
        workbook = Workbook.createWorkbook(xlsFile);
        //定义单元格样式
        WritableCellFormat titleFormat = ExcelUtils.getFormat(0);
        WritableCellFormat headFormat = ExcelUtils.getFormat(1);
        WritableCellFormat tableFormat = ExcelUtils.getFormat(2);
        //第一页
        WritableSheet sheet = workbook.createSheet("健康评分", 0);
        //创建数据
        int col = 0;
        int row;
        for (JSONObject jsonObject : jsonObjects) {
            row = 0;
            sheet.addCell(new Label(col, row++, jsonObject.getString("date"), titleFormat));
            JSONArray list = jsonObject.getJSONArray("list");
            if (list.size() != 0) {
                List<JSONObject> listScore = list.toJavaList(JSONObject.class);
                //创建标题
                sheet.addCell(new Label(col++, row, "分行排名", titleFormat));
                sheet.addCell(new Label(col++, row, "总分", titleFormat));
                List<JSONObject> dimension = listScore.get(0).getJSONObject("detail").getJSONArray("dimension").toJavaList(JSONObject.class);
                for (JSONObject d : dimension) {
                    sheet.addCell(new Label(col++, row, d.getString("name"), titleFormat));
                }
                int n = dimension.size() + 2;
                sheet.mergeCells(col - n, row - 1, col - 1, row - 1);
                row++;
                //创建成绩
                for (JSONObject s : listScore) {
                    List<JSONObject> dimen = s.getJSONObject("detail").getJSONArray("dimension").toJavaList(JSONObject.class);
                    col -= n;
                    sheet.addCell(new Label(col++, row, s.getString("bankName"), headFormat));
                    sheet.addCell(new Label(col++, row, s.getDouble("totalScore") + "", tableFormat));
                    for (JSONObject d : dimen) {
                        sheet.addCell(new Label(col++, row, d.getDouble("result") + "", tableFormat));
                    }
                    row++;
                }
            } else {
                col++;
            }
        }
        //第二页
        WritableSheet sheet1 = workbook.createSheet("平均分", 1);
        //只保留有数据的对象
        List<JSONObject> valid = new LinkedList<>();
        for (JSONObject j : jsonObjects) {
            JSONArray list = j.getJSONArray("list");
            if (list.size() != 0) valid.add(j);
        }
        int count = valid.size();//有效天数
        if (count != 0) {
            JSONObject target = valid.get(0);//目标数据
            //将所有数据累加到target对象
            for (int i = 1; i < valid.size(); i++) {
                JSONObject jsonObject = jsonObjects.get(i);
                JSONArray list = jsonObject.getJSONArray("list");//需要累加的数据
                JSONArray listTarget = target.getJSONArray("list");//目标数据
                List<JSONObject> listScore = list.toJavaList(JSONObject.class);
                List<JSONObject> listTargetScore = listTarget.toJavaList(JSONObject.class);
                //遍历累加数据
                for (int i1 = 0; i1 < listScore.size(); i1++) {
                    JSONObject detail = listScore.get(i1).getJSONObject("detail");
                    JSONObject detailTarget = listTargetScore.get(i1).getJSONObject("detail");
                    //累加总分
                    detailTarget.put("result", detailTarget.getDoubleValue("result") + detail.getDoubleValue("result"));
                    //累计各维度分数
                    List<JSONObject> dimension = detail.getJSONArray("dimension").toJavaList(JSONObject.class);
                    List<JSONObject> dimensionTarget = detailTarget.getJSONArray("dimension").toJavaList(JSONObject.class);
                    for (int i2 = 0; i2 < dimension.size(); i2++) {
                        JSONObject dimen1 = dimension.get(i2);
                        JSONObject dimen2 = dimensionTarget.get(i2);
                        dimen2.put("result", dimen2.getDoubleValue("result") + dimen1.getDoubleValue("result"));
                    }
                }
            }
            //创建数据
            col = 0;
            row = 0;
            sheet1.addCell(new Label(col, row++, "平均分", titleFormat));
            JSONArray list = target.getJSONArray("list");
            List<JSONObject> listScore = list.toJavaList(JSONObject.class);
            //创建标题
            sheet1.addCell(new Label(col++, row, "分行排名", titleFormat));
            sheet1.addCell(new Label(col++, row, "总分", titleFormat));
            List<JSONObject> dimension = listScore.get(0).getJSONObject("detail").getJSONArray("dimension").toJavaList(JSONObject.class);
            for (JSONObject d : dimension) {
                sheet1.addCell(new Label(col++, row, d.getString("name"), titleFormat));
            }
            int n = dimension.size() + 2;
            sheet1.mergeCells(col - n, row - 1, col - 1, row - 1);
            row++;
            //创建成绩
            for (JSONObject s : listScore) {
                List<JSONObject> dimen = s.getJSONObject("detail").getJSONArray("dimension").toJavaList(JSONObject.class);
                col -= n;
                sheet1.addCell(new Label(col++, row, s.getString("bankName"), headFormat));
                sheet1.addCell(new Label(col++, row, s.getJSONObject("detail").getDouble("result") / count + "", tableFormat));
                for (JSONObject d : dimen) {
                    sheet1.addCell(new Label(col++, row, d.getDouble("result") / count + "", tableFormat));
                }
                row++;
            }
        }
        //输出
        workbook.write();
        workbook.close();
        //将文件进行下载
        OutputStream out = response.getOutputStream();
        byte[] data = Files.readAllBytes(xlsFile.toPath());//服务器存储地址
        response.reset();
        response.setContentType("application/msexcel;charset=UTF-8");
        response.setHeader("Content-disposition", "attachment; fileName=" + URLEncoder.encode(xlsFile.getName(), "UTF-8"));
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream;charset=UTF-8");
        IOUtils.write(data, out);
        out.flush();
        out.close();
    }

    @Override
    public List<JSONObject> alarmStatistics(JSONObject param) throws Exception {
        List<JSONObject> eventList = getAlarmData(param);
        return packageData(param, eventList);
    }

    private List<JSONObject> getAlarmData(JSONObject jsonObject) throws Exception {
        long time1 = System.currentTimeMillis();
        //单独获取分行数据，再组装
        List<JSONObject> organization = jsonObject.getJSONArray("organization").toJavaList(JSONObject.class);
        List<JSONObject> res = new LinkedList<>();
        CountDownLatch cdl = new CountDownLatch(organization.size());
        List<Future<List<JSONObject>>> listFuture = new LinkedList<>();
        for (int i = 0; i < organization.size(); i++) {
            String bankName = organization.get(i).getString("name");
            //组装参数
            JSONObject param = new JSONObject();
            JSONObject query = new JSONObject();
            JSONObject search = new JSONObject();
            search.put("Field_xxx_title", bankName);
            query.put("search", search);
            param.put("configDataId", "FHXX");//"分行信息"表单
            param.put("query", query);
            String cookie = RpcUtil.getCookie();
            String result = processEngineRpc.getFormData(cookie, param);
            JSONObject json = JSONObject.parseObject(result);
            if (!"200".equals(json.getString("status")))
                throw new Exception("查询流程引擎失败，接口：/api/flow/api/v1/bfm/instances/model/list，返回信息：" + json.toJSONString());
            JSONArray jsonArray = json.getJSONObject("data").getJSONArray("instancesData");
            if (0 != jsonArray.size()) {
                JSONObject data = new JSONObject();
                List<JSONObject> jsonObjects = jsonArray.toJavaList(JSONObject.class);
                for (JSONObject j : jsonObjects) {
                    if (bankName.equals(j.getString("Field_xxx_title"))) data = j;
                }
                String bankId = data.getString("bank_id");
                String[] bankIdArr = new String[]{bankId};
                //开启线程分别执行
                Future<List<JSONObject>> submit = ThreadPoolUtil.submit(new AlarmCallable(processEngineRpc, jsonObject, bankIdArr, cdl));
                listFuture.add(submit);
//                //组装参数，获取告警信息
//                JSONArray listEventLevel = jsonObject.getJSONArray("eventLevel");
//                int[] eventLevel = new int[listEventLevel.size()];
//                for (int j = 0; j < listEventLevel.size(); j++) {
//                    eventLevel[j] = listEventLevel.getIntValue(j);
//                }
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//                long start = sdf.parse(jsonObject.getString("dateStart")).getTime() / 1000;
//                long end = sdf.parse(jsonObject.getString("dateEnd")).getTime() / 1000;
//                AlarmRequestParam param1 = new AlarmRequestParam(bankIdArr, start, end, eventLevel);
//                String result1 = processEngineRpc.getAlarmData(InfrasConstant.KE_RPC_COOKIE, param1);
//                JSONObject json1 = JSONObject.parseObject(result1);
//                if (!"00".equals(json1.getString("error_code"))) throw new Exception("查询告警信息失败");
//                List<JSONObject> eventList = json1.getJSONObject("data").getJSONArray("event_list").toJavaList(JSONObject.class);
            }
        }
        //等线程全部结果，封装结果
        cdl.await(90, TimeUnit.SECONDS);
        for (Future<List<JSONObject>> future : listFuture) {
            List<JSONObject> eventList = future.get();
            res.addAll(eventList);
        }
        System.out.println("time:耗时 " + (System.currentTimeMillis() - time1));
        return res;
    }

    private List<JSONObject> packageData(JSONObject param, List<JSONObject> eventList) {
        //组装返回信息
        List<JSONObject> result = new LinkedList<>();
        //告警处理状态
        List<JSONObject> alarmHandleState = new LinkedList<>();
        JSONArray state = param.getJSONArray("state");
        for (int i = 0; i < state.size(); i++) {
            JSONObject j = new JSONObject();
            int code = state.getIntValue(i);
            int amount = 0;
            for (JSONObject e : eventList) {
                if (e.getIntValue("is_accept") == code) amount++;
            }
            j.put("name", AlarmStateEnum.getDesc(code));
            j.put("amount", amount);
            alarmHandleState.add(j);
        }
        //告警恢复状态
        List<JSONObject> alarmRecoverState = new LinkedList<>();
        JSONObject recover = new JSONObject();
        JSONObject unrecover = new JSONObject();
        int recoverAmount = 0;
        int unrecoverAmount = 0;
        for (JSONObject e : eventList) {
            if (e.getIntValue("is_recover") == 0) unrecoverAmount++;
            if (e.getIntValue("is_recover") == 1) recoverAmount++;
        }
        recover.put("name", "已恢复");
        recover.put("amount", recoverAmount);
        unrecover.put("name", "未恢复");
        unrecover.put("amount", unrecoverAmount);
        alarmRecoverState.add(recover);
        alarmRecoverState.add(unrecover);
        //告警等级
        List<JSONObject> alarmLevel = new LinkedList<>();
        for (AlarmLevelEnum type : AlarmLevelEnum.values()) {
            JSONObject j = new JSONObject();
            int code = type.getCode();
            int amount = 0;
            for (JSONObject e : eventList) {
                if (e.getIntValue("event_level") == code) amount++;
            }
            j.put("name", type.getDesc());
            j.put("amount", amount);
            alarmLevel.add(j);
        }
        JSONObject json1 = new JSONObject();
        json1.put("name", "告警处理状态");
        json1.put("list", alarmHandleState);
        result.add(json1);
        JSONObject json2 = new JSONObject();
        json2.put("name", "告警恢复状态");
        json2.put("list", alarmRecoverState);
        result.add(json2);
        JSONObject json3 = new JSONObject();
        json3.put("name", "告警等级");
        json3.put("list", alarmLevel);
        result.add(json3);
        return result;
    }

    @Override
    public void exportAlarmStatistics(HttpServletResponse response, JSONObject param) throws Exception {
        List<JSONObject> eventList = getAlarmData(param);
        String dateStart = param.getString("dateStart").substring(0, 10);
        String dateEnd = param.getString("dateEnd").substring(0, 10);
        //生成excel
        File xlsFile = new File(reportUri + "/" + "告警统计报表-" + dateStart + "-" + dateEnd + ".xls");
        if (!xlsFile.getParentFile().exists()) xlsFile.getParentFile().mkdirs();
        WritableWorkbook workbook;
        workbook = Workbook.createWorkbook(xlsFile);
        WritableSheet sheet = workbook.createSheet("告警列表", 0);
        //定义单元格样式
        WritableCellFormat titleFormat = ExcelUtils.getFormat(0);
        WritableCellFormat tableFormat = ExcelUtils.getFormat(2);
        //标题
        int row = 0;
        int col;
        String[] title = new String[]{"序号", "等级", "告警内容", "位置", "位置2", "位置3", "位置4", "位置5", "监控对象", "产生时间", "受理时间", "处理状态",
                "受理人", "受理描述", "恢复时间", "恢复状态", "处理建议", "告警类型", "触发值", "告警分类", "确认时间", "确认人", "确认描述"
        };
        for (col = 0; col < title.length; col++) {
            sheet.addCell(new Label(col, row, title[col], titleFormat));
        }
        sheet.mergeCells(3, 0, 7, 0);//合并位置
        //数据
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (JSONObject j : eventList) {
            row++;
            col = 0;
            sheet.addCell(new Label(col++, row, row + "", tableFormat));//序号
            sheet.addCell(new Label(col++, row, AlarmLevelEnum.getDesc(j.getIntValue("event_level")), tableFormat));//等级
            sheet.addCell(new Label(col++, row, j.getString("content"), tableFormat));//告警内容
            String[] eventSource = j.getString("event_source").split("/");//中国民生银行/北京/牛栏山镇银行/主机房/温湿度_02
            int length = eventSource.length;
            for (int i = 0; i < length - 1; i++) {
                sheet.addCell(new Label(col++, row, eventSource[i], tableFormat));//位置
            }
            for (int i = 0; i < 6 - length; i++) {
                sheet.addCell(new Label(col++, row, "", tableFormat));//空白位置
            }
            sheet.addCell(new Label(col++, row, eventSource[length - 1], tableFormat));//监控对象
            sheet.addCell(new Label(col++, row, j.getLongValue("event_time") == 0 ? "" :
                    sdf.format(new Date(j.getLongValue("event_time") * 1000)), tableFormat));//产生时间
            sheet.addCell(new Label(col++, row, j.getLongValue("accept_time") == 0 ? "" :
                    sdf.format(new Date(j.getLongValue("accept_time") * 1000)), tableFormat));//受理时间
            sheet.addCell(new Label(col++, row, AlarmStateEnum.getDesc(j.getIntValue("is_accept")), tableFormat));//处理状态
            sheet.addCell(new Label(col++, row, j.getString("accept_by"), tableFormat));//受理人
            sheet.addCell(new Label(col++, row, j.getString("accept_description"), tableFormat));//受理描述
            sheet.addCell(new Label(col++, row, j.getLongValue("recover_time") == 0 ? "" :
                    sdf.format(new Date(j.getLongValue("recover_time") * 1000)), tableFormat));//恢复时间
            sheet.addCell(new Label(col++, row, j.getIntValue("is_recover") == 0 ? "未恢复" : "已恢复", tableFormat));//恢复状态
            sheet.addCell(new Label(col++, row, j.getString("event_suggest"), tableFormat));//处理建议
            sheet.addCell(new Label(col++, row, AlarmTypeEnum.getDesc(j.getIntValue("event_type")), tableFormat));//告警类型
            sheet.addCell(new Label(col++, row, j.getString("event_snapshot"), tableFormat));//触发值
            sheet.addCell(new Label(col++, row, ConfirmTypeEnum.getDesc(j.getIntValue("confirm_type")), tableFormat));//告警分类
            sheet.addCell(new Label(col++, row, j.getLongValue("confirm_time") == 0 ? "" :
                    sdf.format(new Date(j.getLongValue("confirm_time") * 1000)), tableFormat));//确认时间
            sheet.addCell(new Label(col++, row, j.getString("confirm_by"), tableFormat));//确认人
            sheet.addCell(new Label(col++, row, j.getString("confirm_description"), tableFormat));//确认描述
        }
        //分类统计_柱状图
        WritableSheet sheet1 = workbook.createSheet("分类统计_柱状图", 1);
        List<JSONObject> jsonObjects = packageData(param, eventList);
        int x = 2, y = 4;
        for (JSONObject j : jsonObjects) {
            String name = j.getString("name");
            JSONArray list = j.getJSONArray("list");
            if (list.size() != 0) {
                List<JSONObject> data = list.toJavaList(JSONObject.class);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                for (JSONObject d : data) {
                    dataset.addValue(d.getIntValue("amount"), d.getString("name"), d.getString("name"));
                }
                ChartUtils.writeChartAsJPEG(bos, ChartUtil.barChart(name, "", "", dataset),
                        1050, 525);
                WritableImage image = new WritableImage(x, y, 12, 20, bos.toByteArray());
                y += 22;
                sheet1.addImage(image);
            }
        }
        workbook.write();
        workbook.close();
        //将文件进行下载
        OutputStream out = response.getOutputStream();
        byte[] data = Files.readAllBytes(xlsFile.toPath());//服务器存储地址
        response.reset();
        response.setContentType("application/msexcel;charset=UTF-8");
        response.setHeader("Content-disposition", "attachment; fileName=" + URLEncoder.encode(xlsFile.getName(), "UTF-8"));
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream;charset=UTF-8");
        IOUtils.write(data, out);
        out.flush();
        out.close();
    }

    @Override
    public List<JSONObject> findForm(JSONObject param) throws Exception {
        String cookie = RpcUtil.getCookie();
        //获取维护任务、演练任务表单id
        String result = processEngineRpc.getFormList(cookie);
        JSONObject json = JSONObject.parseObject(result);
        if (!"200".equals(json.getString("status")))
            throw new Exception("查询流程引擎失败，接口：/api/flow/api/v1/bfm/config/data/module/list，返回信息：" + json.toJSONString());
        List<JSONObject> validForm = new LinkedList<>();
        List<JSONObject> data = JSONArray.parseArray(json.getString("data")).toJavaList(JSONObject.class);
        for (JSONObject d : data) {
            if ("维护任务".equals(d.getString("name"))) {
                JSONObject j = new JSONObject();
                j.put("name", "预防性维护");
                j.put("resourceId", d.getString("resourceId"));
                validForm.add(j);
            }
            if ("演练任务".equals(d.getString("name"))) {
                JSONObject j = new JSONObject();
                j.put("name", "应急演练");
                j.put("resourceId", d.getString("resourceId"));
                validForm.add(j);
            }
        }
        return validForm;
    }

    @Override
    public JSONObject maintainDrill(JSONObject jsonObject) throws Exception {
        //单独获取分行数据，再组装
        List<JSONObject> formList = jsonObject.getJSONArray("formList").toJavaList(JSONObject.class);
        List<JSONObject> organization = jsonObject.getJSONArray("organization").toJavaList(JSONObject.class);
        String dateStart = jsonObject.getString("dateStart");
        String dateEnd = jsonObject.getString("dateEnd");
        //返回结果
        JSONObject res = new JSONObject();
        List<JSONObject> list = new LinkedList<>();//列表数据
        List<JSONObject> finishRate = new LinkedList<>();//任务完成率
        DecimalFormat df = new DecimalFormat("0.0%");
        for (JSONObject form : formList) {
            String resourceId = form.getString("resourceId");
            String name = form.getString("name");
            for (int i = 0; i < organization.size(); i++) {
                //根据数据源、起止时间、分行获取数据
                String id = organization.get(i).getString("id");
                String bankName = organization.get(i).getString("name");
                //组装参数
                JSONObject param = new JSONObject();
                JSONObject query = new JSONObject();
                JSONObject search = new JSONObject();
                //表单id
                param.put("configDataId", resourceId);
                //筛选的起止时间
                JSONObject createTime = new JSONObject();
                createTime.put("startTime", dateStart);
                createTime.put("endTime", dateEnd);
                search.put("Field_xxx_create_time", createTime);
                //分行
                JSONObject createDept = new JSONObject();
                createDept.put("value", id);
                createDept.put("label", new String[]{bankName});
                search.put("Field_xxx_create_dept", createDept);
                query.put("search", search);
                param.put("query", query);
                String cookie = RpcUtil.getCookie();
                String result = processEngineRpc.getFormData(cookie, param);
                JSONObject json = JSONObject.parseObject(result);
                if (!"200".equals(json.getString("status")))
                    throw new Exception("查询流程引擎失败，接口：/api/flow/api/v1/bfm/instances/model/list，返回信息：" + json.toJSONString());
                List<JSONObject> data = json.getJSONObject("data").getJSONArray("instancesData").toJavaList(JSONObject.class);
                int closed = 0;
                //列表数据
                for (JSONObject d : data) {
                    JSONObject jo = new JSONObject();
                    jo.put("bankName", d.getString("Field_xxx_create_dept") == null ? "" : d.getString("Field_xxx_create_dept"));
                    jo.put("resourceId", resourceId);
                    jo.put("type", name);
                    jo.put("title", d.getString("Field_xxx_title"));
                    String status = d.getString("status");
                    jo.put("state", status);
                    list.add(jo);
                    //统计已关闭的任务数
                    if ("已关闭".equals(status)) closed++;
                }
                //任务完成率
                JSONObject j = new JSONObject();
                j.put("bankName", bankName);
                j.put("total", data.size());
                String rate = data.size() == 0 ? "100.0%" : df.format((double) closed / data.size());
                j.put("rate", rate);
                j.put("rateValue", rate.substring(0, rate.lastIndexOf(".") + 2));
                j.put("closed", closed);
                j.put("resourceId", resourceId);
                j.put("type", name);
                finishRate.add(j);
            }
        }
        res.put("list", list);
        res.put("finishRate", finishRate);
        return res;
    }

    @Override
    public void exportMaintainDrill(HttpServletResponse response, JSONObject param) throws Exception {
        List<JSONObject> formList = param.getJSONArray("formList").toJavaList(JSONObject.class);
        JSONObject jsonObject = maintainDrill(param);
        JSONArray list = jsonObject.getJSONArray("list");//列表数据
        JSONArray finishRate = jsonObject.getJSONArray("finishRate");//统计数据
        //生成excel
        String dateStart = param.getString("dateStart");
        String dateEnd = param.getString("dateEnd");
        File xlsFile = new File(reportUri + "/" + "维护工作报表-" + dateStart + "-" + dateEnd + ".xls");
        if (!xlsFile.getParentFile().exists()) xlsFile.getParentFile().mkdirs();
        WritableWorkbook workbook;
        workbook = Workbook.createWorkbook(xlsFile);
        //定义单元格样式
        WritableCellFormat titleFormat = ExcelUtils.getFormat(0);
        WritableCellFormat tableFormat = ExcelUtils.getFormat(2);
        int page = 0;
        for (JSONObject form : formList) {
            String resourceId = form.getString("resourceId");
            String name = form.getString("name");
            //第一个Sheet
            WritableSheet sheet1 = workbook.createSheet(name + "列表", page++);
            //标题
            int row = 0;
            int col;
            String[] title = new String[]{"序号", "分行名称", "任务名称", "状态"};
            for (col = 0; col < title.length; col++) {
                sheet1.addCell(new Label(col, row, title[col], titleFormat));
            }
            //数据
            row++;
            if (list.size() != 0) {
                List<JSONObject> l = list.toJavaList(JSONObject.class);
                for (JSONObject j : l) {
                    if (resourceId.equals(j.getString("resourceId"))) {
                        col = 0;
                        sheet1.addCell(new Label(col++, row, row + "", tableFormat));//序号
                        sheet1.addCell(new Label(col++, row, j.getString("bankName"), tableFormat));
                        sheet1.addCell(new Label(col++, row, j.getString("title"), tableFormat));
                        sheet1.addCell(new Label(col++, row++, j.getString("state"), tableFormat));
                    }
                }
            }
            //第二个Sheet
            WritableSheet sheet2 = workbook.createSheet(name + "统计", page++);
            //标题
            int row2 = 0;
            int col2 = 0;
            sheet2.addCell(new Label(col2, row2++, name, titleFormat));
            sheet2.mergeCells(0, 0, 4, 0);
            String[] title2 = new String[]{"分行名称", "数量", "已完成数量", "待完成数量", "完成率"};
            for (col2 = 0; col2 < title2.length; col2++) {
                sheet2.addCell(new Label(col2, row2, title2[col2], titleFormat));
            }
            //数据
            row2++;
            if (finishRate.size() != 0) {
                List<JSONObject> l = finishRate.toJavaList(JSONObject.class);
                for (JSONObject j : l) {
                    if (resourceId.equals(j.getString("resourceId"))) {
                        col2 = 0;
                        sheet2.addCell(new Label(col2++, row2, j.getString("bankName"), tableFormat));
                        int total = Integer.parseInt(j.getString("total"));
                        sheet2.addCell(new Label(col2++, row2, total + "", tableFormat));
                        int closed = Integer.parseInt(j.getString("closed"));
                        sheet2.addCell(new Label(col2++, row2, closed + "", tableFormat));
                        int unfinish = total - closed;
                        sheet2.addCell(new Label(col2++, row2, unfinish + "", tableFormat));
                        sheet2.addCell(new Label(col2++, row2++, j.getString("rate"), tableFormat));
                    }
                }
            }
        }
        workbook.write();
        workbook.close();
        //将文件进行下载
        OutputStream out = response.getOutputStream();
        byte[] data = Files.readAllBytes(xlsFile.toPath());//服务器存储地址
        response.reset();
        response.setContentType("application/msexcel;charset=UTF-8");
        response.setHeader("Content-disposition", "attachment; fileName=" + URLEncoder.encode(xlsFile.getName(), "UTF-8"));
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream;charset=UTF-8");
        IOUtils.write(data, out);
        out.flush();
        out.close();
    }
}