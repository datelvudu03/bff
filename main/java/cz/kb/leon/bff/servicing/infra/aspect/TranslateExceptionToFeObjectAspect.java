package cz.kb.leon.bff.servicing.infra.aspect;

import cz.kb.leon.bff.servicing.configuration.AppConstants;
import cz.kb.leon.bff.servicing.domain.exception.DomainException;
import cz.kb.leon.bff.servicing.domain.exception.DomainExceptionCode;
import cz.kb.leon.bff.servicing.infra.annotation.TranslateExceptionToFeObject;
import cz.kb.leon.exception.CommonExceptionCode;
import cz.kb.leon.exception.CommonRuntimeException;
import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.UserError;
import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.UserErrors;
import cz.kb.speed.messaging.api.exception.MessageProcessingException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
@Aspect
@Order(100)
public class TranslateExceptionToFeObjectAspect {

    @Pointcut("execution(public * *(..)) && @within(translateExceptionToFeObject)")
    public void translateExceptionToFeObjectPointCut(TranslateExceptionToFeObject translateExceptionToFeObject) {
    }

    @Around(value = "translateExceptionToFeObjectPointCut(translateExceptionToFeObject)", argNames = "joinPoint,translateExceptionToFeObject")
    public Object translateExceptionToFeObject(ProceedingJoinPoint joinPoint, TranslateExceptionToFeObject translateExceptionToFeObject) throws Throwable {
        try {
            // Proceed with the method execution
            return joinPoint.proceed();

        } catch (Throwable exception) {
            CommonRuntimeException commonRuntimeException = extractCommonRuntimeException(exception);

            if (commonRuntimeException == null) {
                // Just rethrow
                throw exception;
            }

            // Check if translation to fe object is enabled and response code is 400 (only 400 have special error object on fe api)
            if (translateExceptionToFeObject != null && translateExceptionToFeObject.enabled() && Response.Status.BAD_REQUEST.getStatusCode() == commonRuntimeException.getExceptionCode().getHttpStatus()) {

                Method interfaceMethod = getInterfaceMethod(joinPoint);

                String errorType = determineErrorType(interfaceMethod);

                if (errorType != null) {
                    return buildErrorResponse(commonRuntimeException, errorType);
                }
            }
            // Just rethrow
            throw exception;
        }
    }

    private CommonRuntimeException extractCommonRuntimeException(Throwable exception) {
        // Same functionality as in MessageProcessingExceptionUnwrapAspect (call has been made via commandBus)
        if (exception instanceof MessageProcessingException mpe) {
            Throwable cause = mpe.getCause();
            if (cause instanceof CommonRuntimeException commonRuntimeException) {
                return commonRuntimeException;
            } else if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException; // Rethrow non-CommonRuntimeExceptions
            }
            // Method call has not been made via command bus
        } else if (exception instanceof CommonRuntimeException commonRuntimeException) {
            return commonRuntimeException;
        }
        return null; // No CommonRuntimeException found
    }

    private Method getInterfaceMethod(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Class<?> declaringClass = method.getDeclaringClass();
        Class<?>[] interfaces = declaringClass.getInterfaces();

        for (Class<?> i : interfaces) {
            try {
                return i.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                // continue searching
            }
        }

        throw new NoSuchMethodException(String.format("Method '%s' with parameters %s not found in any implemented interface of %s",
                method.getName(), Arrays.toString(method.getParameterTypes()), declaringClass.getName()));
    }

    private String determineErrorType(Method interfaceMethod) {
        // GET and POST method have different error types, see https://wiki.kb.cz/display/OSP/Error+and+result+handling
        if (interfaceMethod.isAnnotationPresent(GET.class)) {
            return AppConstants.FE_ERROR_TYPE_GET;
        } else if (interfaceMethod.isAnnotationPresent(POST.class)) {
            return AppConstants.FE_ERROR_TYPE_POST;
        }
        return null;
    }

    private Response buildErrorResponse(CommonRuntimeException exception, String errorType) {
        UserErrors userErrors = new UserErrors();
        UserError userError = new UserError();
        userError.setType(errorType);

        if (exception.getExceptionCode() instanceof DomainExceptionCode domainExceptionCode) {
            userError.setCode(domainExceptionCode.name());
        } else if (exception.getExceptionCode() instanceof CommonExceptionCode commonExceptionCode) {
            userError.setCode(commonExceptionCode.name());
        } else {
            // Handle the case where the exception code is neither type
            userError.setCode("UNKNOWN_EXCEPTION_CODE");
        }

        if (exception instanceof DomainException domainException) {
            userError.setData(domainException.getExceptionParams());
        }

        userErrors.addErrorsItem(userError);

        return Response.status(Response.Status.BAD_REQUEST).entity(userErrors).build();
    }

}
