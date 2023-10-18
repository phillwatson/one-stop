package com.hillayes.exception;

import com.hillayes.commons.correlation.Correlation;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Provider
@Slf4j
public class MensaExceptionMapper implements ExceptionMapper<MensaException> {
    @Override
    public Response toResponse(MensaException aException) {
        if (aException.getErrorCode().getSeverity() == ErrorCode.Severity.error) {
            log.error(aException.getMessage(), aException);
        } else {
            log.info(aException.getMessage(), aException);
        }

        ErrorCode errorCode = aException.getErrorCode();
        String correlationId = Correlation.getCorrelationId().orElse(UUID.randomUUID().toString());

        Response.Status status = Response.Status.fromStatusCode(errorCode.getStatusCode());
        return Response
            .status(status)
            .entity(format(correlationId, aException))
            .build();
    }

    private ServiceError format(String aCorrelationId, MensaException aException) {
        ServiceError result = new ServiceError();
        result.setCorrelationId(aCorrelationId);
        result.setSeverity(aException.getErrorCode().getSeverity());
        result.setMessageId(aException.getErrorCode().getId());
        result.setMessage(aException.getMessage());
        result.setContextAttributes(getContextParams(aException));

        return result;
    }

    private Map<String,String> getContextParams(MensaException aException) {
        return aException.getContext().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString() ));
    }
}
