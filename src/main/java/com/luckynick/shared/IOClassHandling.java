package com.luckynick.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface IOClassHandling {
    boolean sendViaNetwork() default false;
    SharedUtils.DataStorage dataStorage();
}
