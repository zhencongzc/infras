package com.cmbc.infras.system.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.rpc.BaseResultForFlow;
import com.cmbc.infras.dto.rpc.SpotValDto;
import com.cmbc.infras.dto.rpc.SpotValVo;
import com.cmbc.infras.system.rpc.EventRpc;
import com.cmbc.infras.system.service.DataConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * Description:集中监控设置
 * Author: zhencong
 * Date: 2022-2-14
 */
@Slf4j
@RestController
@RequestMapping("/config")
public class DataConfigController {

    @Resource
    private DataConfigService dataConfigService;

    @Resource
    private EventRpc eventRpc;

    /**
     * 快速查询/高级查询
     * rule为空的时候为快速查询，按照word内容模糊查询；（条件：银行名称）
     * rule不为空的时候为高级查询，按照query内容进行条件查询。
     */
    @PostMapping("/quickFind")
    public BaseResult<List<JSONObject>> quickFind(@RequestBody JSONObject param) {
        int pageSize = param.getInteger("pageSize");
        int pageCount = param.getInteger("pageCount");
        int start = pageSize * (pageCount - 1);
        //快速查询
        if (param.getJSONObject("rule") == null) {
            List<JSONObject> list = dataConfigService.quickFind(param.getString("word"), start, pageSize);
            int total = dataConfigService.getTotalByWord(param.getString("word"));
            BaseResult<List<JSONObject>> result = new BaseResult<>(true, null, list, total, pageSize, pageCount);
            return result;
        }
        //高级查询
        List<JSONObject> list = dataConfigService.advancedQuery(param.getJSONObject("rule"), start, pageSize);
        int total = dataConfigService.getTotalByQuery(param.getJSONObject("rule"));
        BaseResult<List<JSONObject>> result = new BaseResult<>(true, null, list, total, pageSize, pageCount);
        return result;
    }

    /**
     * 快捷查询-创建
     * 条件：银行名称、上级银行、排列顺序、银行层级、联系人1、联系人2、城市
     */
    @PostMapping("/fastQuery/create")
    public BaseResult<List<JSONObject>> createFastQuery(@RequestBody JSONObject param) {
        dataConfigService.createFastQuery(param);
        return BaseResult.success("");
    }

    /**
     * 快捷查询-查询
     */
    @PostMapping("/fastQuery/find")
    public BaseResult<List<JSONObject>> findFastQuery(@RequestBody JSONObject param) {
        List<JSONObject> list = dataConfigService.findFastQuery(param.getString("account"));
        return BaseResult.success(list);
    }

    /**
     * 快捷查询-删除
     */
    @PostMapping("/fastQuery/delete")
    public BaseResult<List<JSONObject>> deleteFastQuery(@RequestBody JSONObject param) {
        dataConfigService.deleteFastQuery(param.getIntValue("id"));
        return BaseResult.success("");
    }

    /**
     * 银行编号/设备信息/测点信息-查询（从KE工程组态获取）
     */
    @PostMapping("/bankId/find")
    public BaseResult<List<JSONObject>> findBankId(@RequestBody JSONObject param) {
        try {
            List<JSONObject> res = dataConfigService.findBankId(param);
            return BaseResult.success(res);
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 银行设备测点信息-查询（从KE工程组态获取）
     */
    @PostMapping("/bankSpotListFromKeByResourceId/{resourceId}")
    public BaseResult<JSONObject> getBankSpotListFromKeByResourceId(@PathVariable String resourceId, @RequestBody JSONObject param) {
        try {
            JSONObject res = dataConfigService.getBankSpotListFromKeByResourceId(resourceId, param);
            return BaseResult.success(res);
        } catch (Exception e) {
            log.error("getBankSpotListByResourceId 出现异常：{}", e);
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 设备测点-查询（从KE工程组态获取）
     */
    @PostMapping("/bankSpotLatest")
    public BaseResult<SpotValVo> getBankSpotLatest(@RequestBody SpotValDto spotValDto) {
        try {
            String spotLastStr = eventRpc.getSpotLast(InfrasConstant.KE_RPC_COOKIE, spotValDto);
            JSONObject spotJo = JSONObject.parseObject(spotLastStr);
            String error_code = spotJo.getString("error_code");
            if (!"00".equals(error_code)) {
                return BaseResult.fail("查询测点实时值失败！");
            }
            String spotData = spotJo.getString("data");
            SpotValVo spotValVo = JSON.parseObject(spotData, SpotValVo.class);
            return BaseResult.success(spotValVo);
        } catch (Exception e) {
            log.error("getBankSpotLatest 出现异常：{}", e);
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 查询设备状态值
     */
    @GetMapping("/device/status/find")
    public BaseResult<JSONObject> findDeviceStatus(String bankId) {
        JSONObject res = dataConfigService.findDeviceStatus(bankId);
        return BaseResult.success(res);
    }

    /**
     * 排列顺序-查询
     */
    @PostMapping("/sortPosition/find")
    public BaseResult<List<Integer>> findSortPosition(@RequestBody JSONObject param) {
        List<Integer> res = dataConfigService.findSortPosition(param);
        return BaseResult.success(res);
    }

    /**
     * 银行信息-新增
     */
    @PostMapping("/bank/create")
    public BaseResult<List<JSONObject>> createBank(@RequestBody JSONObject param) {
        dataConfigService.createBank(param);
        return BaseResult.success("");
    }

    /**
     * 银行信息-查询
     */
    @PostMapping("/bank/find")
    public BaseResult<List<JSONObject>> findBank(@RequestBody JSONObject param) {
        List<JSONObject> res = dataConfigService.findBank(param);
        return BaseResult.success(res);
    }

    /**
     * 银行信息-编辑
     */
    @PostMapping("/bank/update")
    public BaseResult<List<JSONObject>> updateBank(@RequestBody JSONObject param) {
        dataConfigService.updateBank(param);
        return BaseResult.success("");
    }

    /**
     * 银行信息-删除
     */
    @PostMapping("/bank/delete")
    public BaseResult<List<JSONObject>> deleteBank(@RequestBody JSONObject param) {
        dataConfigService.deleteBank(param);
        return BaseResult.success("");
    }

    /**
     * 设备管理-查询
     */
    @PostMapping("/device/find")
    public BaseResult<List<JSONObject>> findDevice(@RequestBody JSONObject param) {
        List<JSONObject> res = dataConfigService.findDevice(param);
        return BaseResult.success(res);
    }

    /**
     * 测点类型-查询
     */
    @PostMapping("/spotType/find")
    public BaseResult<List<JSONObject>> findSpotType(@RequestBody JSONObject param) {
        List<JSONObject> res = dataConfigService.findSpotType(param);
        return BaseResult.success(res);
    }

    /**
     * 设备管理-保存
     */
    @PostMapping("/device/save")
    public BaseResult<List<JSONObject>> saveDevice(@RequestBody JSONObject param) {
        dataConfigService.saveDevice(param);
        return BaseResult.success("");
    }

    /**
     * 安全运行时间初始化（显示设置）
     */
    @PostMapping("/safeTime/initialize")
    public BaseResult<List<JSONObject>> initializeSafeTime() {
        dataConfigService.initializeSafeTime();
        return BaseResult.success("");
    }

    /**
     * 用户列表查询
     */
    @PostMapping("/userList")
    public BaseResult<List<JSONObject>> userList(@RequestBody JSONObject param) {
        List<JSONObject> res = dataConfigService.userList(param.getString("word"));
        return BaseResult.success(res, res.size());
    }

    /**
     * 查询KE用户列表
     */
    @GetMapping("/findEmployee")
    public BaseResult<List<JSONObject>> findEmployee() {
        List<JSONObject> res = dataConfigService.findEmployee();
        return BaseResult.success(res, res.size());
    }

    /**
     * 用户信息添加
     */
    @PostMapping("/user/add")
    public BaseResult<List<JSONObject>> addUser(@RequestBody JSONObject param) {
        dataConfigService.addUser(param.getJSONArray("list").toJavaList(JSONObject.class));
        return BaseResult.success("");
    }

    /**
     * 用户信息删除
     */
    @PostMapping("/user/delete")
    public BaseResult<List<JSONObject>> deleteUser(@RequestBody JSONObject param) {
        dataConfigService.deleteUser(param.getIntValue("id"));
        return BaseResult.success("");
    }

    /**
     * 工单列表查询
     */
    @GetMapping("/formList/find")
    public BaseResult<List<JSONObject>> findFormList() {
        try {
            List<JSONObject> res = dataConfigService.findFormList();
            return BaseResult.success(res, res.size());
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 用户信息保存
     */
    @PostMapping("/user/update")
    public BaseResult<List<JSONObject>> updateUser(@RequestBody JSONObject param) {
        dataConfigService.updateUser(param);
        return BaseResult.success("");
    }

    /**
     * 映射到其他用户
     */
    @PostMapping("/mapOtherUser")
    public BaseResult<List<JSONObject>> mapOtherUser(@RequestBody JSONObject param) {
        dataConfigService.mapOtherUser(param);
        return BaseResult.success("");
    }

    /**
     * 工单消息通知
     * 流程引擎工单变动会调用此接口，然后通知需要通知的用户
     */
    @PostMapping("/formMessage/inform")
    public BaseResultForFlow<List<JSONObject>> informFormMessage(@RequestBody JSONObject param) {
        try {
            dataConfigService.informFormMessage(param.getString("moduleKey"), param.getString("title"), param.getString("context"));
            return new BaseResultForFlow(200, "操作成功", "", false);
        } catch (Exception e) {
            e.printStackTrace();
            return new BaseResultForFlow(500, "服务器内部错误", "", false);
        }
    }
}
