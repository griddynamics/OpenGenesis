package com.griddynamics.genesis.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RUNTIME)
@Constraint(validatedBy = StringMapValidator.class)
public @interface ValidMap {
    String message() default "NOT USED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int key_min() default 0;
    int key_max() default Integer.MAX_VALUE;

    int value_min() default 0;
    int value_max() default Integer.MAX_VALUE;

}
