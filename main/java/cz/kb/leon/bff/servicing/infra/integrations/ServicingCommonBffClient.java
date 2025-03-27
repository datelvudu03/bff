package cz.kb.leon.bff.servicing.infra.integrations;

import cz.kb.leon.bff.servicing.util.ObjectUtil;
import cz.kb.speed.exception.ServiceException;
import cz.kb.speed.rest.config.JaxRsRestClientProperties;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ServicingCommonBffClient {

    protected String evaluateTarget(Object... pathParts) {
        var target = getBaseUri() + Stream.of(pathParts).map(Objects::toString).collect(Collectors.joining("/", "/", "")).replaceFirst("^\\/{2,}", "/");
        getLog().info("Target end point: {}.", target);
        return target;
    }

    protected String getBaseUri() {
        return getJaxClientProperties().getBaseUri();
    }

    protected Response parseResponse(Response response, Supplier<?> mapperMethod) {
        return parseResponse(response, Response.Status.OK, mapperMethod);
    }

    protected Response parseResponse(Response response, Response.Status expectedResultStatus, Supplier<?> mapperMethod) {
        var responseStatus = response.getStatus();
        if (responseStatus == 200 || responseStatus == 201) {
            var responseDto = mapperMethod.get();

            return Response.status(expectedResultStatus)
                    .entity(responseDto)
                    .build();
        } else {
            return Response
                    .status(responseStatus)
                    .entity(response.readEntity(Object.class))
                    .build();
        }
    }

    protected Pair<Integer, Object> parseResponse(Response response, Response.Status acceptableStatus, Response.Status expectedResultStatus, Supplier<?> mapperMethod) {
        var responseStatus = response.getStatus();
        if (responseStatus == acceptableStatus.getStatusCode()) {
            return Pair.of(expectedResultStatus.getStatusCode(), mapperMethod.get());
        } else {
            return Pair.of(responseStatus, response.readEntity(Object.class));
        }
    }

    protected JaxRsRestClientProperties.Client getJaxClientProperties() {
        return getClientProperties().getClient().get(getClientName());
    }

    protected  <T> T getResponseEntity(Response response, Class<T> entityClass) {
        return response.readEntity(entityClass);
    }

    protected <T> T evaluateResponse(Response response, Class<T> responseType, String exceptionMessagePattern) {
        var responseStatus = response.getStatus();

        if (responseStatus == 200) {
            return getResponseEntity(response, responseType);
        } else {
            throw new ServiceException(ObjectUtil.evaluateMessage(exceptionMessagePattern, responseStatus));
        }
    }

    protected abstract String getClientName();

    protected abstract JaxRsRestClientProperties getClientProperties();

    protected abstract Logger getLog();

}
