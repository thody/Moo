package com.codiform.moo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Property {
	String source() default "";
	boolean translate() default false;
	boolean update() default false;
	Optionality optionality() default Optionality.DEFAULT;
}
