package lphy.graphicalModel;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ParameterInfo {
    String name();
    String description();
    Class type() default Object.class;
    boolean optional() default false;
}