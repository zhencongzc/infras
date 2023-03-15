package com.cmbc.infras.system.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.Device;
import com.cmbc.infras.dto.event.AlarmCount;
import com.cmbc.infras.dto.monitor.Spot;
import com.cmbc.infras.dto.ops.SiteStatis;
import com.cmbc.infras.dto.rpc.SpotValDto;
import com.cmbc.infras.dto.rpc.SpotValVo;
import com.cmbc.infras.dto.rpc.alarm.FieldItem;
import com.cmbc.infras.dto.rpc.alarm.TermItem;
import com.cmbc.infras.dto.rpc.alarm.WhereCountIterm;
import com.cmbc.infras.dto.rpc.event.*;
import com.cmbc.infras.redis.DataRedisUtil;
import com.cmbc.infras.system.mapper.BankMapper;
import com.cmbc.infras.system.mapper.DeviceSpotMapper;
import com.cmbc.infras.system.rpc.EventRpc;
import com.cmbc.infras.system.rpc.HistoryAlarmRpc;
import com.cmbc.infras.system.vo.AlarmKpiVo;
import com.cmbc.infras.system.vo.BankKpiVo;
import com.cmbc.infras.util.AlarmParamUtils;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

@Service
public class WorkBenchService {
    private static final Logger log = LoggerFactory.getLogger(WorkBenchService.class);
    /**
     * 告警级别1的数量-当前周期的紧急告警
     */
    public static final String ALARM_NUM = "alarmNum";
    /**
     * 告警级别1的数量-上一个周期的告警
     */
    public static final String PRE_ALARM_NUM = "preAlarmNum";
    /**
     * 告警级别1的响应数量-当前周期的
     */
    public static final String RESPONSE_NUM = "responseNum";
    /**
     * 前一周期的响应趋势
     */
    public static final String PRE_RESPONSE_NUM = "preResponseNum";
    /**
     * 响应趋势
     */
    public static final String RESPONSE_TREND = "responseTrend";
    /**
     * 告警级别1的响应率-当前周期的
     */
    public static final String RATE_VAL = "rateVal";
    /**
     * 告警级别1的响应率-前一周期的
     */
    public static final String PRE_RATE_VAL = "preRateVal";
    /**
     * 响应率
     */
    public static final String RATE = "rate";
    /**
     * state
     */
    public static final String STATE = "state";


    @Resource
    private BankMapper bankMapper;

    @Resource
    private DeviceSpotMapper deviceSpotMapper;

    @Resource
    private EventRpc eventRpc;
    @Resource
    private HistoryAlarmRpc historyAlarmRpc;

    @Resource
    private ExecutorService cachedThreadPool;

    /**
     * 获取银行列表
     *
     * @param level 0 总行 1 分行 2 二级分行
     */
    public List<Bank> getBankListByLevel(int level) {
        List<Bank> banks = bankMapper.getBanksByLevel(level);
        if (banks == null) {
            return new ArrayList<>();
        } else {
            return banks;
        }
    }

    /**
     * 获取告警指标
     *
     * @param type day,week
     */
    public List<AlarmKpiVo> getAlarmKpiByType(String type) {
        List<AlarmKpiVo> result = new LinkedList<>();
        //设置互斥锁日统计5分钟，周统计60分钟，成功的话开启线程更新缓存
        if ("day".equals(type)) {
            if (DataRedisUtil.addStringToRedisByExpireTime("alarm_kpi_day_mutex", "1", 1000 * 60 * 5l) != null) {
                cachedThreadPool.execute(() -> {
                    log.info("告警指标alarmKPI（日）开始更新缓存...");
                    long l = System.currentTimeMillis();
                    List<AlarmKpiVo> list = getAlarmKpi(type);
                    DataRedisUtil.addStringToRedis("alarm_kpi_day", JSON.toJSONString(list));
                    log.info("告警指标alarmKPI（日）更新缓存成功！耗时：{}ms", System.currentTimeMillis() - l);
                });
            }
            String str = DataRedisUtil.getStringFromRedis("alarm_kpi_day");
            result = JSONArray.parseArray(str).toJavaList(AlarmKpiVo.class);
        }
        if ("week".equals(type)) {
            if (DataRedisUtil.addStringToRedisByExpireTime("alarm_kpi_week_mutex", "1", 1000 * 60 * 60l) != null) {
                cachedThreadPool.execute(() -> {
                    log.info("告警指标alarmKPI（周）开始更新缓存...");
                    long l = System.currentTimeMillis();
                    List<AlarmKpiVo> list = getAlarmKpi(type);
                    DataRedisUtil.addStringToRedis("alarm_kpi_week", JSON.toJSONString(list));
                    log.info("告警指标alarmKPI（周）更新缓存成功！耗时：{}ms", System.currentTimeMillis() - l);
                });
            }
            String str = DataRedisUtil.getStringFromRedis("alarm_kpi_week");
            result = JSONArray.parseArray(str).toJavaList(AlarmKpiVo.class);
        }
        return result;
    }

    private List<AlarmKpiVo> getAlarmKpi(String type) {
        String account = "admin";
        List<AlarmKpiVo> result = new ArrayList<>();
        // 1. 根据type 拿到告警的时间段
        Map<String, Long> timeStampsByType = genTimeStampsByType(type);
        Long beginTime = timeStampsByType.get("beginTime"), endTime = timeStampsByType.get("endTime");
        Long beginTimePre = timeStampsByType.get("beginTimePre"), endTimePre = timeStampsByType.get("endTimePre");
        // 2. 拿到40家银行的bankId
        List<Bank> banks = this.getBankListByLevel(1);
        // 3. 获取当前周期和前一个周期的 全量告警记录
        // 4. 从告警记录中获取KPI
        Map<String, String> alarmLevel1Nums = this.getAlarmNum(beginTime, endTime, beginTimePre, endTimePre, account, banks, cachedThreadPool,
                true);
        String alarmLevel1Num = alarmLevel1Nums.getOrDefault(ALARM_NUM, "-");
        String preAlarmLevel1Num = alarmLevel1Nums.getOrDefault(PRE_ALARM_NUM, "0");
        String level1ResponseTrend = alarmLevel1Nums.getOrDefault(RESPONSE_TREND, "0");
        String rate = alarmLevel1Nums.getOrDefault(RATE, "-");
        // 趋势为与前一个周期对比的结果,高则为红色,低为绿色;
        long alarmLevel1Trend = Long.parseLong(alarmLevel1Num) - Long.parseLong(preAlarmLevel1Num);
        AlarmKpiVo a = new AlarmKpiVo("紧急告警个数", alarmLevel1Num, String.valueOf(alarmLevel1Trend), alarmLevel1Trend >= 0 ? "1" : "0");
        AlarmKpiVo b = new AlarmKpiVo("紧急告警响应率", rate, level1ResponseTrend + "%", Long.parseLong(level1ResponseTrend) >= 0 ? "1" : "0");
        Map<String, String> alarmOtherLevelNums = this.getAlarmNum(beginTime, endTime, beginTimePre, endTimePre, account, banks, cachedThreadPool,
                false);
        String alarmOtherNum = alarmOtherLevelNums.getOrDefault(ALARM_NUM, "-");
        String preAlarmOtherNum = alarmOtherLevelNums.getOrDefault(PRE_ALARM_NUM, "0");
        String otherResponseTrend = alarmOtherLevelNums.getOrDefault(RESPONSE_TREND, "0");
        String rateOfOther = alarmOtherLevelNums.getOrDefault(RATE, "-");
        long otherTrend = Long.parseLong(alarmOtherNum) - Long.parseLong(preAlarmOtherNum);
        AlarmKpiVo c = new AlarmKpiVo("其它告警", alarmOtherNum, String.valueOf(otherTrend), otherTrend >= 0 ? "1" : "0");
        AlarmKpiVo d = new AlarmKpiVo("其它告警响应率", rateOfOther, otherResponseTrend + "%", Long.parseLong(otherResponseTrend) >= 0 ? "1" : "0");
        // 5. 返回最终结果
        result.add(a);
        result.add(b);
        result.add(c);
        result.add(d);
        return result;
    }

    public List<BankKpiVo> getBankKpiByType(String type) {
        List<BankKpiVo> result = new LinkedList<>();
        //设置互斥锁日统计5分钟，周统计60分钟，成功的话开启线程更新缓存
        if ("day".equals(type)) {
            if (DataRedisUtil.addStringToRedisByExpireTime("bank_kpi_day_mutex", "1", 1000 * 60 * 5l) != null) {
                cachedThreadPool.execute(() -> {
                    log.info("银行告警指标bankKPI（日）开始更新缓存...");
                    long l = System.currentTimeMillis();
                    List<BankKpiVo> list = getBankKpi(type);
                    DataRedisUtil.addStringToRedis("bank_kpi_day", JSON.toJSONString(list));
                    log.info("银行告警指标bankKPI（日）更新缓存成功！耗时：{}ms", System.currentTimeMillis() - l);
                });
            }
            String str = DataRedisUtil.getStringFromRedis("bank_kpi_day");
            result = JSONArray.parseArray(str).toJavaList(BankKpiVo.class);
        }
        if ("week".equals(type)) {
            if (DataRedisUtil.addStringToRedisByExpireTime("bank_kpi_week_mutex", "1", 1000 * 60 * 60l) != null) {
                cachedThreadPool.execute(() -> {
                    log.info("银行告警指标bankKPI（周）开始更新缓存...");
                    long l = System.currentTimeMillis();
                    List<BankKpiVo> list = getBankKpi(type);
                    DataRedisUtil.addStringToRedis("bank_kpi_week", JSON.toJSONString(list));
                    log.info("银行告警指标bankKPI（周）更新缓存成功！耗时：{}ms", System.currentTimeMillis() - l);
                });
            }
            String str = DataRedisUtil.getStringFromRedis("bank_kpi_week");
            result = JSONArray.parseArray(str).toJavaList(BankKpiVo.class);
        }
        return result;
    }

    /**
     * 40家分行kpi
     * PUE可从工程组态中的测点获取,
     * 目前在集中监控中已构建了绑定关系,
     * 可通过识别银行编号、设备编号、设备类型、测点类型来判断,
     * 先通过银行编号和设备类型获取设备编号,
     * 再通过设备编号和测点类型获取测点编号,
     * 最终从KE系统工程组态获取实时测点值
     */
    private List<BankKpiVo> getBankKpi(String type) {
        List<BankKpiVo> result = new ArrayList<>();
        String account = "admin";
        // 1. 拿到40家银行的bankId
        List<Bank> dbBanks = this.getBankListByLevel(1);
        List<String> bankIds = new ArrayList<>();
        if (dbBanks == null || dbBanks.isEmpty()) {
            return result;
        }
        // 1.1 组装银行信息
        for (Bank bank : dbBanks) {
            String bankId = bank.getBankId();
            // 1.1.1 抽取bankId
            bankIds.add(bankId);
            // 1.1.2 抽取银行基本信息
            BankKpiVo bankKpiVo = new BankKpiVo();
            bankKpiVo.setBankId(bankId);
            bankKpiVo.setBankName(bank.getBankName());
            result.add(bankKpiVo);
        }
        // 2. 银行pues
        List<Device> banksPues = deviceSpotMapper.getBanksPues(bankIds);
        // 3. 银行pue设备测点spots（实时pue）
        List<String> deviceIds = new ArrayList<>();
        if (banksPues == null) {
            return result;
        }
        // 3.1 缓存pue的deviceId
        for (Device banksPue : banksPues) {
            String deviceId = banksPue.getDeviceId();
            deviceIds.add(deviceId);
            BankKpiVo bankKpiVo = result.stream().filter(x -> banksPue.getBankId().equals(x.getBankId())).findAny().orElse(null);
            if (bankKpiVo != null) {
                bankKpiVo.setPueDeviceId(deviceId);
            }
        }
        List<Spot> devicesPueSpots = deviceSpotMapper.getDevicesPueSpots(deviceIds);
        // 4. 发起ke请求
        if (devicesPueSpots == null) {
            return result;
        }
        SpotValDto spotValDto = new SpotValDto();
        List<SpotValDto.SpotVal> resources = spotValDto.getResources();
        for (Spot devicesPueSpot : devicesPueSpots) {
            // 4.1 拿到测点id
            SpotValDto.SpotVal spotVal = new SpotValDto.SpotVal();
            spotVal.setResource_id(devicesPueSpot.getSpotId());
            resources.add(spotVal);
            // 4.2 缓存spotId
            BankKpiVo bankKpiVo = result.stream().filter(x -> devicesPueSpot.getDeviceId().equals(x.getPueDeviceId())).findAny().orElse(null);
            if (bankKpiVo != null) {
                bankKpiVo.setPueSpotId(devicesPueSpot.getSpotId());
            }
        }
        String spotLastStr = eventRpc.getSpotLast(AlarmParamUtils.createCookie(account), spotValDto);
        BaseResult<SpotValVo> spotValVoR = this.parseSpotsResult(spotLastStr);
        if (!spotValVoR.isSuccess() || spotValVoR.getData() == null) {
            return result;
        }
        // 5. ke测点的实时值
        List<SpotValVo.SpotVal> resourcesOfSpot = spotValVoR.getData().getResources();
        for (SpotValVo.SpotVal spotVal : resourcesOfSpot) {
            BankKpiVo bankKpiVo = result.stream().filter(x -> spotVal.getResource_id().equals(x.getPueSpotId())).findAny().orElse(null);
            if (bankKpiVo != null) {
                bankKpiVo.setPue(spotVal.getReal_value());
            }
        }
        // 6. 紧急告警处理率 和 趋势
        // 6.1 根据type 拿到告警的时间段
        Map<String, Long> timeStampsByType = genTimeStampsByType(type);
        Long beginTime = timeStampsByType.get("beginTime"), endTime = timeStampsByType.get("endTime");
        Long beginTimePre = timeStampsByType.get("beginTimePre"), endTimePre = timeStampsByType.get("endTimePre");
        result.stream().forEach(bankKpiVo -> {
            ArrayList<Bank> banks_item = new ArrayList<Bank>() {{
                add(new Bank(bankKpiVo.getBankId(), bankKpiVo.getBankName()));
            }};
            Map<String, String> alarmNum = this.getAlarmNum(beginTime, endTime, beginTimePre, endTimePre, account, banks_item, cachedThreadPool,
                    true);
            String rate = alarmNum.getOrDefault("rate", "-");
            String state = alarmNum.getOrDefault("state", "-");
            bankKpiVo.setRate(rate);
            bankKpiVo.setState(state);
        });
        return result;
    }


    /**
     * 获取告警数量
     * {“rate”:"1","state":"1"}
     *
     * @param banks 银行列表
     * @return
     */
    private Map<String, String> getAlarmNum(Long beginTime, Long endTime, Long beginTimePre, Long endTimePre, String account, List<Bank> banks,
                                            ExecutorService executorService, Boolean isAlarmLevel1) {
        Map<String, String> result = new HashMap<>();
        // 6. 紧急告警率
        FieldItem eventLevel;
        if (isAlarmLevel1) {
            eventLevel = new FieldItem("event_level", "in", new int[]{1});
        } else {
            eventLevel = new FieldItem("event_level", "in", new int[]{2, 3, 4, 5});
        }
        List<FieldItem> unhandledCondition = new LinkedList<>();//未处理未恢复的条件
        unhandledCondition.add(new FieldItem("is_accept", "in", new int[]{0}));//未处理
        unhandledCondition.add(new FieldItem("is_recover", "in", new int[]{0}));//未恢复
        // 6.1 紧急告警响应率(紧急告警响应率=已受理告警或已确认告警或已恢复告警数量  ➗  发生的紧急告警数量  *  100%)
        // 6.1.1 紧急告警个数
        CompletableFuture<Long> alarmNumFuture = this.synGetAlarmNum(executorService, beginTime, endTime, account, banks, eventLevel, null);
        CompletableFuture<Long> preAlarmNumFuture = this.synGetAlarmNum(executorService, beginTimePre, endTimePre, account, banks, eventLevel, null);
        // 6.1.2 紧急告警未处理个数
        CompletableFuture<Long> responseNumFuture = this.synGetAlarmNum(executorService, beginTime, endTime, account, banks, eventLevel, unhandledCondition);
        CompletableFuture<Long> preResponseNumFuture = this.synGetAlarmNum(executorService, beginTimePre, endTimePre, account, banks, eventLevel, unhandledCondition);
        CompletableFuture.allOf(alarmNumFuture, preAlarmNumFuture, responseNumFuture, preResponseNumFuture);
        try {
            Long alarmNum = alarmNumFuture.get();
            Long preAlarmNum = preAlarmNumFuture.get();
            Long responseNum = alarmNum - responseNumFuture.get();
            Long preResponseNum = preAlarmNum - preResponseNumFuture.get();
            if (log.isDebugEnabled()) {
                log.debug("bankName(0)={}, alarmNum={}, preAlarmNum={}, responseNum={}, preResponseNum={}", banks.get(0).getBankName(), alarmNum, preAlarmNum, responseNum, preResponseNum);
            }
            BigDecimal rate = this.genAlarmRate(responseNum, alarmNum);
            BigDecimal preRate = this.genAlarmRate(preResponseNum, preAlarmNum);
            // 6.1.3 紧急告警处理 趋势
            int responseTrend = rate.subtract(preRate).intValue();
            // 缓存一下中间结果，便于其他接口复用
            result.put(ALARM_NUM, String.valueOf(alarmNum));
            result.put(PRE_ALARM_NUM, String.valueOf(preAlarmNum));
            result.put(RESPONSE_NUM, String.valueOf(responseNum));
            result.put(PRE_RESPONSE_NUM, String.valueOf(preResponseNum));
            result.put(RESPONSE_TREND, String.valueOf(responseTrend));
            result.put(RATE_VAL, String.valueOf(rate.longValue()));
            result.put(PRE_RATE_VAL, String.valueOf(preRate.longValue()));
            // 最终结果
            result.put(RATE, rate.longValue() + "%");
            result.put(STATE, responseTrend > 0 ? "1" : "0");
        } catch (InterruptedException e) {
            log.error("异步获取告警数据失败 InterruptedException ：{}", e.getMessage());
        } catch (ExecutionException e) {
            log.error("异步获取告警数据失败 ExecutionException ：{}", e.getMessage());
        } catch (Exception e) {
            log.error("异步获取告警数据失败 Exception ：{}", e.getMessage());
        }
        return result;
    }

    /**
     * 异步获取告警数
     */
    private CompletableFuture<Long> synGetAlarmNum(ExecutorService executorService, Long beginTime, Long endTime, String account, List<Bank> banks,
                                                   FieldItem eventLevel, List<FieldItem> otherCondition) {
        return CompletableFuture.supplyAsync(() -> {
            long alarmCountNum = getHistoryEvent(beginTime, endTime, account, banks, eventLevel, otherCondition);
            return alarmCountNum;
        }, executorService);
    }

    /**
     * 生成时间戳-每天一个周期轮回
     */
    private static Long genTimeStampForDay(Long preDay) {
        LocalDateTime today = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        if (preDay != null) {
            today = today.plusDays(preDay);
        }
        log.info("距离今天的前 preDay:{} 天的 凌晨时间为 = {}", preDay, today);
        return today.toInstant(ZoneOffset.ofHours(8)).getEpochSecond();
    }

    /**
     * 生成时间戳-每周4 为一个周期轮回
     */
    private static Long genTimeStampForWeek(Long preDay) {
        LocalDateTime today = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        if (dayOfWeek.getValue() > 4) {
            today = today.plusDays(-(dayOfWeek.getValue() - 4L));
        } else {
            today = today.plusDays(-(dayOfWeek.getValue() + 3L));
        }
        if (preDay != null) {
            today = today.plusDays(preDay);
        }
        log.info("距离今天的前 preDay:{} 天的 凌晨时间为 = {}", preDay, today);
        return today.toInstant(ZoneOffset.ofHours(8)).getEpochSecond();
    }

    /**
     * 生成-根据操作类型生成一组时间戳
     */
    private static Map<String, Long> genTimeStampsByType(String type) {
        Map<String, Long> result = new HashMap<>(4);
        Long nowTimeStamp = Instant.now().getEpochSecond();
        Long beginTime = nowTimeStamp, endTime = nowTimeStamp;
        Long beginTimePre = nowTimeStamp, endTimePre = nowTimeStamp;
        if ("day".equals(type)) {
            beginTime = genTimeStampForDay(0L);
            endTime = Instant.now().getEpochSecond();
            beginTimePre = genTimeStampForDay(-1L);
            endTimePre = beginTime;
        }
        if ("week".equals(type)) {
            beginTime = genTimeStampForWeek(0L);
            endTime = Instant.now().getEpochSecond();
            beginTimePre = genTimeStampForWeek(-7L);
            endTimePre = beginTime;
        }
        result.put("beginTime", beginTime);
        result.put("endTime", endTime);
        result.put("beginTimePre", beginTimePre);
        result.put("endTimePre", endTimePre);
        return result;
    }

    /**
     * 生成-告警响应率
     */
    private static BigDecimal genAlarmRate(Long num, Long divisorNum) {
        try {
            if (divisorNum == null || divisorNum.longValue() == 0) {
                return BigDecimal.valueOf(100);
            }
            return BigDecimal.valueOf(num).divide(BigDecimal.valueOf(divisorNum), 2, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * http请求-获取历史事件
     */
    private int getHistoryEvent(Long beginTime, Long endTime, String account, List<Bank> banks, FieldItem eventLevel,
                                List<FieldItem> otherCondition) {
        //准备参数
        List<String> ids = new LinkedList<>();//银行id
        for (Bank bank : banks) {
            ids.add(bank.getBankId());
        }
        //组装入参
        List<TermItem> terms = new ArrayList<>();//最终参数
        List<String> bothIds = new ArrayList<>();
        List<String> preIds = new ArrayList<>();
        for (String id : ids) {
            bothIds.add("%/" + id + "/%");
            preIds.add("%/" + id);
        }
        //共用项
        FieldItem gteItem = new FieldItem("event_time", "gte", beginTime);
        FieldItem lteItem = new FieldItem("event_time", "lte", endTime);
        FieldItem maskItem = new FieldItem("masked", "in", Arrays.asList(0));
        //Term1
        FieldItem resourceItem = new FieldItem("resource_id", "in", ids);
        //Term2
        FieldItem eventItemAll = new FieldItem("event_location", "like_any", bothIds);
        //Term3
        FieldItem eventItemPre = new FieldItem("event_location", "like_any", preIds);
        TermItem term1 = new TermItem().add(resourceItem).add(gteItem).add(lteItem).add(maskItem).add(eventLevel);
        TermItem term2 = new TermItem().add(eventItemAll).add(gteItem).add(lteItem).add(maskItem).add(eventLevel);
        TermItem term3 = new TermItem().add(eventItemPre).add(gteItem).add(lteItem).add(maskItem).add(eventLevel);
        //添加筛选条件
        if (null != otherCondition) {
            for (FieldItem a : otherCondition) {
                term1.add(a);
                term2.add(a);
                term3.add(a);
            }
        }
        terms.add(term1);
        terms.add(term2);
        terms.add(term3);
        WhereCountIterm whereParam = new WhereCountIterm(terms);
        // 打印查询条件日志
        LocalDateTime beginDateTime = LocalDateTime.ofEpochSecond(beginTime, 0, ZoneOffset.ofHours(8));
        LocalDateTime endDateTime = LocalDateTime.ofEpochSecond(endTime, 0, ZoneOffset.ofHours(8));
        if (log.isDebugEnabled())
            log.debug("beginTime:{}, endTime:{} bankName(0):{} 查询条件：{}", beginDateTime, endDateTime, banks != null && banks.size() > 0 ?
                    banks.get(0).getBankName() : null, JSON.toJSONString(whereParam));
        //解析数据
        String str = historyAlarmRpc.getHistoryAlarmCount(AlarmParamUtils.createCookie(account), whereParam);
        AlarmCount alarmCount = AlarmParamUtils.parseCountResult(str);
        return alarmCount.getCount();
    }

    /**
     * 生成-历史告警参数
     */
    private static EventParam genHistoryParam(Long beginTime, Long endTime, List<QueryCondition> extendConditions) {
        List<QueryCondition> list = new ArrayList<>();
        if (extendConditions != null) list.addAll(extendConditions);
        if (beginTime != null) list.add(new QueryCondition("event_time", "gte", beginTime));
        if (endTime != null) list.add(new QueryCondition("event_time", "lte", endTime));
        // 只查告警(notin 7) 查事件(eq 7) --这里只查历史告警实时告警
        list.add(new QueryCondition("event_type", "notin", 7));
        //默认条件--
        list.add(new QueryCondition("masked", "eq", 0));
        list.add(new QueryCondition("cep_processed", "eq", 0));
        // 默认等级
        WhereCondition where = new WhereCondition(list);
        List<SortCondition> sorts = new ArrayList() {{
            add(new SortCondition("event_time", "DESC"));
        }};
        //分页查询参数
        PageCondition page = new PageCondition("1", 99999);
        EventParam param = new EventParam();
        param.setWhere(where);
        param.setSorts(sorts);
        param.setPage(page);
        return param;
    }

    /**
     * 转换-告警数量结果
     */
    private static BaseResult<AlarmCount> parseAlarmCountResult(String countStr) {
        JSONObject countObj = JSONObject.parseObject(countStr);
        String countCode = countObj.getString("error_code");
        if (!"00".equals(countCode)) {
            return BaseResult.fail("查询历史告警数量失败！");
        }
        String countData = countObj.getString("data");
        AlarmCount alarmCount = JSON.parseObject(countData, AlarmCount.class);
        return BaseResult.success(alarmCount);
    }

    /**
     * 转换-测点结果
     */
    private static BaseResult<SpotValVo> parseSpotsResult(String spotsStr) {
        JSONObject spotObject = JSONObject.parseObject(spotsStr);
        String spotCode = spotObject.getString("error_code");
        if (!"00".equals(spotCode)) {
            return BaseResult.fail("查询测点实时值失败！");
        }
        String spotData = spotObject.getString("data");
        SpotValVo spotValVo = JSON.parseObject(spotData, SpotValVo.class);
        return BaseResult.success(spotValVo);
    }
}
