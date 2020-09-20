package ca.vanzyl.concord.plugins.toolsupport.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface OptionWithEquals {

    String[] name();

    String description() default "";

    boolean required() default false;

    String[] allowedValues() default {};
}
