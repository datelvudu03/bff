package cz.kb.leon.bff.servicing.infra.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used over bff public methods of services that are called directly from fe.
 * When error 400 arises, aspect connected to this annotation (TranslateExceptionToFeObjectAspect) will build a special error return object for fe so fe can display different screens
 * in reaction to different 400 codes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TranslateExceptionToFeObject {

    boolean enabled() default true;

}
