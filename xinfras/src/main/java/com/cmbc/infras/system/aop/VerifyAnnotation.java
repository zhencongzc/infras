package com.cmbc.infras.system.aop;

import java.lang.annotation.*;

/**
 * 验证水平越权注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface VerifyAnnotation {

    String tag() default "bankId";

}
