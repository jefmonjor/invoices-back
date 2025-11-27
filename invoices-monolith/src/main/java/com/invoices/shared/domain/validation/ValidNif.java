package com.invoices.shared.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = NifValidator.class)
@Documented
public @interface ValidNif {
    String message() default "Invalid Spanish Tax ID (NIF/CIF/NIE)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
