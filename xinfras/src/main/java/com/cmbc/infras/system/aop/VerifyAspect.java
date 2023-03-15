package com.cmbc.infras.system.aop;

import com.alibaba.fastjson.JSON;
import com.cmbc.infras.system.service.FlowFormService;
import com.cmbc.infras.util.UserContext;
import com.cmbc.infras.util.YmlConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

@Slf4j
@Aspect
@Component
public class VerifyAspect {

    @Resource
    private FlowFormService flowFormService;

    @Pointcut("@annotation(com.cmbc.infras.system.aop.VerifyAnnotation)")
    public void pointcut() {
    }

    @Before("pointcut()")
    public void beforeInvoke(JoinPoint jp) {
        if (YmlConfig.getBoolValue("no-verify")) return;
        Method method = getTargetMethod(jp);
        Assert.notNull(method, "您无权操作");
        VerifyAnnotation verify = method.getAnnotation(VerifyAnnotation.class);
        if (verify == null) throw new RuntimeException("取得注解对象为空！");
        Object[] args = jp.getArgs();
        //没有参数,取不到bankId,不存在传参越权
        if (args.length == 0) throw new RuntimeException("无参方法,无法验证参数");
        //默认bankId是第一个参数,或者第一个参数.bankId
        Object paramObj = args[0];
        if (paramObj == null || paramObj.getClass().isPrimitive()) {
            log.info("参数为空,无需校验参数！方法名:{},参数:{}", getTargetMethod(jp), JSON.toJSONString(paramObj));
            return;
        }
        //param是bankId
        if (paramObj instanceof String) {
            String id = (String) paramObj;
            if (StringUtils.isBlank(id)) {
                log.info("参数为空,无需校验！");
                return;
            }
            if (checkContain(id)) {
                return;
            }
            throw new RuntimeException("越权访问！");
        }
        //日志显示用-targetClassName
        String targetClassName = getTargetClassName(jp);
        //参数对象里的bankId
        String paramName = verify.tag();
        Object paramValue = getParamValue(paramObj, paramName, targetClassName);
        if (paramValue == null) {
            log.info("参数为空,无需校验！！");
            return;
        }
        if (paramValue instanceof String) {
            String id = (String) paramValue;
            if (StringUtils.isBlank(id)) {
                log.info("参数为空,无需校验！");
                return;
            }
            if (checkContain(id)) {
                return;
            }
            throw new RuntimeException("越权访问！！");
        } else {
            throw new RuntimeException("非String类型参数！！");
        }
    }

    private boolean checkContain(String param) {
        String bankId = UserContext.getUserBankId();
        if (param.equals(bankId)) {
            return true;
        }
        String sessionId = UserContext.getAuthToken();
        String ids = flowFormService.getCacheSubBankIdstr(bankId, sessionId);

        if (ids.contains(param)) {
            return true;
        } else {
            return false;
        }
    }

    private Method getTargetMethod(JoinPoint jp) {
        Method proxyMethod = ((MethodSignature) jp.getSignature()).getMethod();
        try {
            return jp.getTarget().getClass().getMethod(proxyMethod.getName(), proxyMethod.getParameterTypes());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getTargetClassName(JoinPoint jp) {
        return jp.getTarget().getClass().getName() + "." + jp.getSignature().getName();
    }

    private Object getParamValue(Object paramObj, String paramName, String methodName) {
        Object result = null;
        Field field = ReflectionUtils.findField(paramObj.getClass(), paramName);
        if (field == null) {
            log.error("参数属性未找到,方法名:{},参数名:{}", methodName, paramName);
            return result;
        }
        field.setAccessible(true);
        result = ReflectionUtils.getField(field, paramObj);
        if (result == null) {
            log.error("参数值为空．方法名:{},参数名:{}", methodName, paramName);
        }
        return result;
    }

}
