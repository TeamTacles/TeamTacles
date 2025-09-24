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

    /**
     * Pointcut para interceptar todos os métodos na camada de Controller.
     */
    @Pointcut("within(br.com.teamtacles.*.controller..*)")
    public void controllerPackagePointcut() {}

    /**
     * Pointcut para interceptar todos os métodos públicos na camada de Service.
     */
    @Pointcut("execution(public * br.com.teamtacles.*.service..*(..))")
    public void servicePackagePointcut() {}

    /**
     * Pointcut que captura a execução de qualquer método anotado com @BusinessActivityLog.
     * @param businessActivityLog A instância da anotação, permitindo acesso ao seu valor 'action'.
     */
    @Pointcut("@annotation(businessActivityLog)")
    public void businessActivityPointcut(BusinessActivityLog businessActivityLog) {}


    // ===================================================================================
    // ADVICES
    // ===================================================================================

    /**
     * Loga a entrada de requisições HTTP na camada de Controller
     */
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

    /**
     * Advice que executa ANTES de um método anotado com @BusinessActivityLog.
     * Gera o log de [ACTION] com o usuário e os detalhes da operação.
     */
    @Before("businessActivityPointcut(businessActivityLog)")
    public void logBusinessActivityStart(JoinPoint joinPoint, BusinessActivityLog businessActivityLog) {
        User actingUser = findUserArgument(joinPoint.getArgs());
        String userIdentifier = (actingUser != null) ? actingUser.getUsername() : "SYSTEM";

        log.info("[ACTION] User '{}' is attempting to perform: '{}'. Details: [{}]",
                userIdentifier,
                businessActivityLog.action(),
                getArgumentDetails(joinPoint.getArgs()));
    }

    /**
     * Advice que executa APÓS o retorno sucesso de um método anotado com @BusinessActivityLog.
     * Gera o log de [SUCCESS] com o usuário e o resultado da operação.
     */
    @AfterReturning(pointcut = "businessActivityPointcut(businessActivityLog)", returning = "result")
    public void logBusinessActivitySuccess(JoinPoint joinPoint, BusinessActivityLog businessActivityLog, Object result) {
        User actingUser = findUserArgument(joinPoint.getArgs());
        String userIdentifier = (actingUser != null) ? actingUser.getUsername() : "SYSTEM";

        log.info("[SUCCESS] Action '{}' performed by user '{}' completed successfully. Result: [{}]",
                businessActivityLog.action(),
                userIdentifier,
                result != null ? result.toString() : "void");
    }

    /**
     * Log genérico de DEBUG para todos os métodos de serviço.
     * Ignora os métodos que já têm um log de negócio para evitar duplicidade.
     *
     */
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
    // MÉTODOS DE APOIO (HELPERS)
    // ===================================================================================

    /**
     * Procura por um argumento do tipo User na lista de argumentos de um método.
     * @return O objeto User encontrado, ou null se não houver.
     */
    private User findUserArgument(Object[] args) {
        return Arrays.stream(args)
                .filter(User.class::isInstance)
                .map(User.class::cast)
                .findFirst()
                .orElse(null);
    }

    /**
     * Formata os argumentos de um método para uma string legível, ignorando o objeto User.
     * @return Uma string com os detalhes dos argumentos.
     */
    private String getArgumentDetails(Object[] args) {
        if (args == null || args.length == 0) {
            return "No arguments";
        }
        return Arrays.stream(args)
                .filter(arg -> !(arg instanceof User)) // Não logamos o objeto User inteiro
                .map(arg -> arg != null ? arg.toString() : "null")
                .collect(Collectors.joining(", "));
    }
}