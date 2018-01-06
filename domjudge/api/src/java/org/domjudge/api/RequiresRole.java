package org.domjudge.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.domjudge.proto.DomjudgeProto.User;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@interface RequiresRole {
  User.Role[] anyOf() default {};
  User.Role[] allOf() default {};
  boolean any() default false;
}
