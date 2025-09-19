package com.IKov.auth_service.web.dto.validation.annotation;

import com.IKov.auth_service.web.dto.validation.validators.PasswordMatchesValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchesValidator.class)
@Documented
public @interface PasswordMatches {

    String message() default "Passwords do not matches";

    Class<?>[] group() default {};

    Class<? extends Payload>[] payload() default {};

    String passwordField() default "password";

    String confirmationPasswordField() default "passwordConfirmation";

}
