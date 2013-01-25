package com.griddynamics.genesis.rest.annotations;

import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
public @interface LinkTo {
    LinkTarget rel() default LinkTarget.SELF;
    RequestMethod[] methods() default {RequestMethod.GET};
    Class<?> controller();
    String controllerMethod() default "";
    Class<?> clazz();
}

