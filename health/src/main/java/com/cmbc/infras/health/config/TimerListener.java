package com.cmbc.infras.health.config;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.health.mapper.AssessMapper;
import com.cmbc.infras.health.mapper.ModelMapper;
import com.cmbc.infras.health.redis.DataRedisUtil;
import com.cmbc.infras.health.rpc.ProcessEngineRpc;
import com.cmbc.infras.health.thread.AnalysisThread;
import com.cmbc.infras.health.thread.CreateReport;
import com.cmbc.infras.health.thread.MonitorThread;
import com.cmbc.infras.health.util.CommonUtils;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import static com.cmbc.infras.health.controller.ModelController.*;
import static com.cmbc.infras.health.controller.ModelController.mapAnalysisService;

/**
 * 服务器启动时启动已经开启过的定时任务
 */
@Component
public class TimerListener implements ServletContextListener {

    private static Logger logger = Logger.getLogger(TimerListener.class.getName());

    @Resource
    private ModelMapper modelMapper;
    @Resource
    private AssessMapper assessMapper;
    @Resource
    private ProcessEngineRpc processEngineRpc;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        event.getServletContext().log("监听器已启动");
        //查询所有启动的模板，开启定时任务
        List<JSONObject> activeModel = modelMapper.findActiveModel();
        activeModel.forEach((model) -> {
            String modelId = model.getString("modelId");
            int cycleValue = model.getIntValue("cycleValue");
            String cycleUnit = model.getString("cycleUnit");
            long cycle = cycleValue * CommonUtils.change(cycleUnit);
            //启动创建模板
            if (model.getIntValue("startScore") == 1) {
                CreateReport runnable = new CreateReport(modelId, modelMapper, assessMapper, false);
                ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
                service.scheduleAtFixedRate(runnable, cycle, cycle, TimeUnit.SECONDS);
                mapReportService.put(modelId, service);
            }
            //运行监控：开启定时任务
            MonitorThread monitorThread = new MonitorThread(modelId, modelMapper, assessMapper, processEngineRpc);
            ScheduledExecutorService service1 = Executors.newSingleThreadScheduledExecutor();
            service1.scheduleAtFixedRate(monitorThread, 0, cycle, TimeUnit.SECONDS);
            mapMonitorService.put(modelId, service1);
            //统计分析：开启定时任务
            AnalysisThread analysisThread = new AnalysisThread(modelId, modelMapper, assessMapper, processEngineRpc);
            ScheduledExecutorService service2 = Executors.newSingleThreadScheduledExecutor();
            service2.scheduleAtFixedRate(analysisThread, 0, cycle, TimeUnit.SECONDS);
            mapAnalysisService.put(modelId, service2);
        });
        logger.info("mapReportService运行的线程为:" + mapReportService.keySet().toString());
        logger.info("mapMonitorService运行的线程为:" + mapMonitorService.keySet().toString());
        logger.info("mapAnalysisService运行的线程为:" + mapAnalysisService.keySet().toString());
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}

