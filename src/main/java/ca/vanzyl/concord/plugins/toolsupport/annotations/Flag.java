package ca.vanzyl.concord.plugins.toolsupport.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// packer build: -debug or -force
// helm install: --wait
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Flag {

    String[] name();

    String description() default "";

    Class<?> omitFor() default void.class;
}