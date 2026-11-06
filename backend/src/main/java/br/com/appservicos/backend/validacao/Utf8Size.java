package br.com.appservicos.backend.validacao;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Utf8SizeValidator.class)
public @interface Utf8Size {

	String message() default "O valor deve ter no máximo {max} bytes UTF-8";

	int max();

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
