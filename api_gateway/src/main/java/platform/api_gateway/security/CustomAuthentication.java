package platform.api_gateway.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;

public class CustomAuthentication implements Authentication {

    private final Map<String, String> metadata;
    private final String userStatus;

    public String getUserStatus() {
        return userStatus;
    }

    public CustomAuthentication(Map<String, String> metadata, String userStatus) {
        this.metadata = metadata;
        this.userStatus = userStatus;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Map<String, String> getPrincipal() {
        return metadata;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    }

    @Override
    public String getName() {
        return metadata.getOrDefault("serviceToken", "");
    }
}
