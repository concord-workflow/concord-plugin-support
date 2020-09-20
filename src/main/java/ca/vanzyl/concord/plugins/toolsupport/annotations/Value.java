package ca.vanzyl.concord.plugins.toolsupport.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Only the value of this field is contributed to the CLI arguments */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Value
{
}