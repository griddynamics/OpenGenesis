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
@Constraint(validatedBy = OptionEmailValidator.class)
public @interface OptionalEmail {
    String message() default "{org.hibernate.validator.constraints.Email.message}";

   	Class<?>[] groups() default { };

   	Class<? extends Payload>[] payload() default { };

}
