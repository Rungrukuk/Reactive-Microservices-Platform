package platform.api_gateway.util;

public enum AuthResponseStatuses {
    /**
     * When user is authenticated and has permissions for the given resources
     */
    AUTHORIZED_USER,
    /**
     * When user is unauthenticated but has permissions for the given resources
     */
    AUTHORIZED_GUEST_USER,
    /**
     * When user has no access to the given resources, but authenticated
     */
    UNAUTHORIZED_USER,
    /**
     * When user has no access to the given resources, and unauthenticated with
     * valid session and access token
     */
    UNAUTHORIZED_GUEST_USER,
    /**
     * When user unauthenticated with invalid session and access token. Created new
     * session and/or access token
     */
    UNAUTHENTICATED_GUEST_USER,
    /**
     * When unexpected error occurs
     */
    UNEXPECTED_ERROR,
}
