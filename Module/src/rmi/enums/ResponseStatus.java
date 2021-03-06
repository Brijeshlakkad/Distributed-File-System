package rmi.enums;

/**
 * https://cloud.google.com/endpoints/docs/frameworks/java/exceptions
 *
 * @author Brijesh Lakkad
 * @version 1.0
 */
public enum ResponseStatus {
    /**
     * Successful invocation of the request.
     */
    Ok(200),
    /**
     * When the request had invalid data. Usually used when <code>InvocationTargetException</code> thrown.
     */
    BadRequestException(400),
    /**
     * When the request was not authorized, meaning that invocation didn't have access to an object. Usually when
     * <code>IllegalAccessException</code> thrown.
     */
    UnauthorizedException(401),
    /**
     * When the request mentioning the method was not found.
     */
    NotFoundException(404),
    /**
     * When the method invocation terminated because the input stream closed or the stream was corrupted.
     */
    InternalServerErrorException(500);

    public Integer d_jsonValue;

    /**
     * Parameterised constructor to set the (json) value of the enum member.
     *
     * @param p_jsonValue Value to be set for the enum member.
     */
    private ResponseStatus(Integer p_jsonValue) {
        this.d_jsonValue = p_jsonValue;
    }

    /**
     * Gets the value of the enum
     *
     * @return Value of the enum
     */
    public Integer getJsonValue() {
        return d_jsonValue;
    }
}
