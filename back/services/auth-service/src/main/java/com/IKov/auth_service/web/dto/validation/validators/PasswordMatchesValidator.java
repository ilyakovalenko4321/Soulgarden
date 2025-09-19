package com.IKov.auth_service.web.dto.validation.validators;

import com.IKov.auth_service.web.dto.validation.annotation.PasswordMatches;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Валидатор, который проверяет, совпадают ли значения двух полей пароля
 * в объекте. Используется для подтверждения пароля при регистрации.
 */
public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    private String passwordFieldName;
    private String confirmationPasswordFieldName;

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
        this.passwordFieldName = constraintAnnotation.passwordField();
        this.confirmationPasswordFieldName = constraintAnnotation.confirmationPasswordField();
    }

    /**
     * Выполняет валидацию.
     *
     * @param objectToValidate Объект, который необходимо валидировать.
     * @param context Контекст валидатора.
     * @return true, если пароли совпадают, иначе false.
     */
    @Override
    public boolean isValid(Object objectToValidate, ConstraintValidatorContext context) {
        if (objectToValidate == null) {
            return true;
        }

        try {
            String password = getFieldValue(objectToValidate, passwordFieldName);
            String confirmation = getFieldValue(objectToValidate, confirmationPasswordFieldName);

            if (password == null || confirmation == null) {
                return false;
            }
            return password.equals(confirmation);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Вспомогательный метод для безопасного получения значения поля по его имени
     * с использованием рефлексии.
     */
    private String getFieldValue(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(object);
        return value != null ? value.toString() : null;
    }
}