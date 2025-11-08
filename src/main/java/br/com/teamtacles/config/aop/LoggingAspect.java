package br.com.teamtacles.config.aop;

import br.com.teamtacles.user.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    // ===================================================================================
    // POINTCUTS
    // ===================================================================================

    // Intercepta todos os métodos dentro dos pacotes de controller
    @Pointcut("within(br.com.teamtacles.*.controller..*)")
    public void controllerPackagePointcut() {}


    // Intercepta todos os métodos dentro dos pacotes de service
    @Pointcut("execution(public * br.com.teamtacles.*.service..*(..))")
    public void servicePackagePointcut() {}

    // Intercepta todos os métodos anotados com @BusinessActivityLog
    @Pointcut("@annotation(businessActivityLog)")
    public void businessActivityPointcut(BusinessActivityLog businessActivityLog) {}


    // ===================================================================================
    // ADVICES
    // ===================================================================================

    // Advice que executa ANTES de qualquer método em controllers
    @Before("controllerPackagePointcut()")
    public void logBeforeRequest(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            log.info("==> Request [{} {}] | Controller [{}::{}]",
                    request.getMethod(), request.getRequestURI(),
                    joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
        }
    }

    // Advice que executa ANTES de um método anotado com @BusinessActivityLog.
    @Before("businessActivityPointcut(businessActivityLog)")
    public void logBusinessActivityStart(JoinPoint joinPoint, BusinessActivityLog businessActivityLog) {
        User actingUser = findUserArgument(joinPoint.getArgs());
        String userIdentifier = (actingUser != null) ? actingUser.getUsername() : "SYSTEM";

        log.info("[ACTION] User '{}' is attempting to perform: '{}'. Details: [{}]",
                userIdentifier,
                businessActivityLog.action(),
                getArgumentDetails(joinPoint.getArgs()));
    }

    // Advice que executa APÓS o retorno bem-sucedido de um método anotado com @BusinessActivityLog
    @AfterReturning(pointcut = "businessActivityPointcut(businessActivityLog)", returning = "result")
    public void logBusinessActivitySuccess(JoinPoint joinPoint, BusinessActivityLog businessActivityLog, Object result) {
        User actingUser = findUserArgument(joinPoint.getArgs());
        String userIdentifier = (actingUser != null) ? actingUser.getUsername() : "SYSTEM";

        log.info("[SUCCESS] Action '{}' performed by user '{}' completed successfully. Result: [{}]",
                businessActivityLog.action(),
                userIdentifier,
                result != null ? result.toString() : "void");
    }

   // Advice que executa APÓS um método anotado com @BusinessActivityLog lançar uma exceção
    @Around("servicePackagePointcut() && !@annotation(br.com.teamtacles.config.aop.BusinessActivityLog)")
    public Object logServiceMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.debug("==> Entering Service [{}::{}] | Arguments: {}", className, methodName, Arrays.toString(joinPoint.getArgs()));

        long startTime = System.currentTimeMillis();
        Object result;

        try {
            result = joinPoint.proceed();
            long timeTaken = System.currentTimeMillis() - startTime;
            log.debug("<== Exiting Service [{}::{}] | Result: {} | Duration: {}ms", className, methodName, result, timeTaken);
            return result;
        } catch (Throwable throwable) {
            long timeTaken = System.currentTimeMillis() - startTime;
            log.error("<== Exception in Service [{}::{}] | Exception: {} | Duration: {}ms", className, methodName, throwable.getMessage(), timeTaken);
            throw throwable;
        }
    }

    // ===================================================================================
    // MÉTODOS DE APOIO
    // ===================================================================================

    // Procura um argumento do tipo User entre os argumentos do método
    private User findUserArgument(Object[] args) {
        return Arrays.stream(args)
                .filter(User.class::isInstance)
                .map(User.class::cast)
                .findFirst()
                .orElse(null);
    }

    // Gera uma string com os detalhes dos argumentos, omitindo objetos do tipo User
    private String getArgumentDetails(Object[] args) {
        if (args == null || args.length == 0) {
            return "No arguments";
        }
        return Arrays.stream(args)
                .filter(arg -> !(arg instanceof User)) // No caso, não logamos o objeto User inteiro
                .map(arg -> arg != null ? arg.toString() : "null")
                .collect(Collectors.joining(", "));
    }
}