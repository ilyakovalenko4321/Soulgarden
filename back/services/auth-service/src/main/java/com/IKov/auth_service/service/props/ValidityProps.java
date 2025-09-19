package com.IKov.auth_service.service.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "auth.validity")
public class ValidityProps {

    private String access;
    private String refresh;

    public ValidityProps() {
    }

    public ValidityProps(String access, String refresh) {
        this.access = access;
        this.refresh = refresh;
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public String getRefresh() {
        return refresh;
    }

    public void setRefresh(String refresh) {
        this.refresh = refresh;
    }

}
