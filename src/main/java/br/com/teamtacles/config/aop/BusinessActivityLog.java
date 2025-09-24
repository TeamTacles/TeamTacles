package br.com.teamtacles.config.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para marcar métodos de serviço que representam uma atividade de negócio significativa.
 * O LoggingAspect usará esta anotação para gerar logs de auditoria detalhados.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessActivityLog {

    String action();
}