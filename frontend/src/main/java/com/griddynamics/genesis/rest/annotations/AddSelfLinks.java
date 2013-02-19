package com.griddynamics.genesis.rest.annotations;

import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface AddSelfLinks {
    RequestMethod[] methods() default {RequestMethod.GET};
    Class<?> modelClass();

}
