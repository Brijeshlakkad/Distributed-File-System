package rmi.enums;

/**
 * https://cloud.google.com/endpoints/docs/frameworks/java/exceptions
 *
 * @author Brijesh Lakkad
 * @version 1.0
 */
public enum ResponseStatus {
    Ok(200),
    BadRequestException(400),
    UnauthorizedException(401),
    ForbiddenException(403),
    NotFoundException(404),
    ConflictException(409),
    InternalServerErrorException(500),
    ServiceUnavailableException(503);

    public Integer d_jsonValue;

    /**
     * Parameterised constructor to set the (json) value of the enum member.
     *
     * @param p_jsonValue
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
