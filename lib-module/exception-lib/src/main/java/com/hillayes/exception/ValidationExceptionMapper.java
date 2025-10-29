package com.hillayes.exception;

import com.hillayes.commons.correlation.Correlation;
import com.hillayes.exception.common.CommonErrorCodes;
import com.hillayes.onestop.api.ErrorSeverity;
import com.hillayes.onestop.api.ServiceError;
import com.hillayes.onestop.api.ServiceErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    // the ErrorCode used to report all validation errors
    private final static ErrorCode ERROR_CODE = CommonErrorCodes.INVALID_REQUEST_CONTENT;
    private final static ErrorSeverity ERROR_SEVERITY = ERROR_CODE.getSeverity().getApiSeverity();

    @Override
    public Response toResponse(ConstraintViolationException aException) {
        String correlationId = Correlation.getCorrelationId().orElseGet(() -> UUID.randomUUID().toString());

        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(format(correlationId, aException))
            .build();
    }

    private ServiceErrorResponse format(String aCorrelationId, ConstraintViolationException aException) {
        ServiceErrorResponse result = new ServiceErrorResponse();
        result.setCorrelationId(aCorrelationId);

        Set<ConstraintViolation<?>> violations = aException.getConstraintViolations();
        if (violations == null) {
            result.addErrorsItem(new ServiceError()
                .severity(ERROR_SEVERITY)
                .messageId(ERROR_CODE.getId())
                .message(aException.getMessage())
            );
        } else {
            violations.forEach(violation -> {
                String name = getLeafNode(violation.getPropertyPath()).getName();
                result.addErrorsItem(new ServiceError()
                    .severity(ERROR_SEVERITY)
                    .messageId(ERROR_CODE.getId())
                    .message(name + " " + violation.getMessage())
                    .putContextAttributesItem("field-name", name)
                    .putContextAttributesItem("field-value", String.valueOf(violation.getInvalidValue()))
                );
            });
        }

        result.severity(ERROR_SEVERITY);
        return result;
    }

    private Path.Node getLeafNode(Path path) {
        Path.Node result = null;
        for (Path.Node node : path) result = node;
        return result;
    }
}
