package ca.vanzyl.concord.plugins.tool.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Option {

    String[] name();

    Class<?> omitFor() default void.class;

    String description() default "";
    
    String[] allowedValues() default {};
}