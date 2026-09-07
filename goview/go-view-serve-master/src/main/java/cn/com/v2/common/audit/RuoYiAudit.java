package cn.com.v2.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RuoYiAudit
{
    String title();

    int businessType() default 0;

    boolean saveRequestData() default true;

    boolean saveResponseData() default false;
}
