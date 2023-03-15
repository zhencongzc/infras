package com.cmbc.infras.health.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.health.mapper.AssessMapper;
import com.cmbc.infras.health.mapper.CommonMapper;
import com.cmbc.infras.health.rpc.ProcessEngineRpc;
import com.cmbc.infras.health.service.CommonService;
import com.cmbc.infras.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.DecimalFormat;
import java.util.*;

@Service
@Slf4j
public class CommonServiceImpl implements CommonService {

    @Resource
    private CommonMapper commonMapper;
    @Resource
    private AssessMapper assessMapper;
    @Resource
    private ProcessEngineRpc processEngineRpc;

    @Override
    public BaseResult<JSONObject> findAuthority(int id) {
        //TODO 等门户完善用户信息后使用
//        JSONObject param = new JSONObject();
//        param.put("id", id);
//        String result = processEngineRpc.getRoleOrganization(param);
//        JSONObject json = JSONObject.parseObject(result);
//        if (!"200".equals(json.getString("code"))) return BaseResult.fail("获取角色组织信息失败");
//        JSONObject data = json.getJSONObject("data");
//        JSONObject res = new JSONObject();
//        res.put("name", data.getJSONObject("org").getString("name"));//组织名称
        JSONObject param = new JSONObject();
        param.put("orgId", 1);
        String result = processEngineRpc.getOrganizationAndRole(param);
        log.info("获取角色组织信息，返回参数result：" + result);
        JSONObject json = JSONObject.parseObject(result);
        if (!"200".equals(json.getString("code")))
            return BaseResult.fail("查询流程引擎失败，接口：/api/admin/rpc/dept/getTreeWithUser，返回信息：" + json.toJSONString());
        JSONObject data = json.getJSONArray("data").toJavaList(JSONObject.class).get(0);
        List<JSONObject> children = data.getJSONArray("children").toJavaList(JSONObject.class);
        List<JSONObject> userList = data.getJSONArray("userList").toJavaList(JSONObject.class);//总行人员
        //遍历data找到id对应的用户所属的组织
        JSONObject res = new JSONObject();
        String name = "";//组织名称
        String userName = "";//用户名称
        //在总行找
        for (JSONObject j : userList) {
            if (id == j.getIntValue("id")) {
                name = data.getString("name");
                res.put("name", name);
                userName = j.getString("name");
                res.put("userName", userName);
                break;
            }
        }
        //去分行找
        if (StringUtils.isEmpty(name)) {
            A:
            for (JSONObject child : children) {
                String bankName = child.getString("name");
                if (child.getJSONArray("children") != null) {
                    List<JSONObject> subChildren = child.getJSONArray("children").toJavaList(JSONObject.class);
                    name = handler(subChildren, id, name, bankName);//name#userName
                    if (!StringUtils.isEmpty(name)) break A;
                }
                if (child.getJSONArray("userList") != null) {
                    List<JSONObject> user = child.getJSONArray("userList").toJavaList(JSONObject.class);
                    for (JSONObject j : user) {
                        if (id == j.getIntValue("id")) {
                            name = bankName + "#" + j.getString("name");
                            break A;
                        }
                    }
                }
            }
            String[] split = name.split("#");
            res.put("name", split[0]);
            res.put("userName", split[1]);
        }
        //是管理员
        if (id == 1) {
            res.put("isAdmin", 1);
            List<JSONObject> list = commonMapper.findAllModel();
            res.put("isCommitRole", 1);
            res.put("isAuditRole", 1);
            res.put("EnableCommitModel", list);
            res.put("EnableAuditModel", list);
            return BaseResult.success(res);
        }
        //非管理员
        res.put("isAdmin", 0);
        //是否提交人，封装可以提交的模板
        List<JSONObject> commit = commonMapper.findAvailableModelCommit(id);
        if (0 != commit.size()) {
            res.put("isCommitRole", 1);
            res.put("EnableCommitModel", commit);
        } else {
            res.put("isCommitRole", 0);
        }
        //是否审核人，封装可以审核的模板
        List<JSONObject> audit = commonMapper.findAvailableModelAudit(id);
        if (0 != audit.size()) {
            res.put("isAuditRole", 1);
            res.put("EnableAuditModel", audit);
        } else {
            res.put("isAuditRole", 0);
        }
        return BaseResult.success(res);
    }

    private String handler(List<JSONObject> children, int id, String name, String bankName) {
        for (JSONObject child : children) {
            if (child.getJSONArray("children") != null) {
                List<JSONObject> subChildren = child.getJSONArray("children").toJavaList(JSONObject.class);
                name = handler(subChildren, id, name, bankName);
                if (!StringUtils.isEmpty(name)) return name;
            }
            if (child.getJSONArray("userList") != null) {
                List<JSONObject> userList = child.getJSONArray("userList").toJavaList(JSONObject.class);
                for (JSONObject j : userList) {
                    if (id == j.getIntValue("id")) return bankName + "#" + j.getString("name");
                }
            }
        }
        return "";
    }

    @Override
    public BaseResult<List<JSONObject>> evaluation(String modelId) {
        int year = Integer.parseInt(Utils.getCurrentTime("yyyy"));
        List<JSONObject> list = commonMapper.findDimensionScore(modelId, year);
        HashMap<String, List<JSONObject>> map = new HashMap<>();
        list.forEach((a) -> {
            String bankName = a.getString("bankName");
            if (map.get(bankName) == null) map.put(bankName, new LinkedList<>());
            JSONObject j = new JSONObject();
            j.put("id", a.getIntValue("id"));
            j.put("name", a.getString("name"));
            j.put("score", a.getDoubleValue("score"));
            map.get(bankName).add(j);
        });
        List<JSONObject> result = new LinkedList<>();
        DecimalFormat df = new DecimalFormat("0.00");
        map.forEach((k, v) -> {
            JSONObject j = new JSONObject();
            j.put("bankName", k);
            j.put("list", v);
            double score = 0d;
            for (JSONObject json : v) {
                score += json.getDoubleValue("score");
            }
            j.put("score", df.format(score));
            result.add(j);
        });
        //按照成绩降序
        result.sort((o1, o2) -> {
            double v = o1.getDoubleValue("score") - o2.getDoubleValue("score");
            if (v > 0) return -1;
            if (v < 0) return 1;
            return 0;
        });
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<JSONObject> maintainRate(String modelId, String name) {
        //实时成绩
        int year = Integer.parseInt(Utils.getCurrentTime("yyyy"));
        List<JSONObject> rank = assessMapper.scoreRank(modelId, year);
        Double result = 0D;
        for (JSONObject s : rank) {
            if (s.getString("bankName").equals(name)) result = s.getDoubleValue("score");
        }
        //年平均分
        Double average = 0D;
        List<JSONObject> list = assessMapper.findReportByYear(modelId, name, year);
        for (JSONObject l : list) {
            List<JSONObject> dimension = l.getJSONObject("report").getJSONArray("dimension").toJavaList(JSONObject.class);
            for (JSONObject d : dimension) {
                average += d.getDoubleValue("result");
            }
        }
        if (list.size() != 0) average /= list.size();
        //结果
        DecimalFormat df = new DecimalFormat("0.0");
        JSONObject res = new JSONObject();
        res.put("result", df.format(result));
        res.put("average", df.format(average));
        return BaseResult.success(res);
    }

    public static void main(String[] args) {
    }
}
