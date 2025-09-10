package org.nuxeo.shaded.keycloak.adapters.tomcat;

import org.apache.catalina.Container;
import org.apache.catalina.Valve;
import org.keycloak.adapters.AdapterDeploymentContext;

/**
 * @implNote Copied from Keycloak Tomcat Adapter since it won't be upgraded to Jakarta as their support is dropped
 */
public class AuthenticatedActionsValve extends AbstractAuthenticatedActionsValve {

    public AuthenticatedActionsValve(AdapterDeploymentContext deploymentContext, Valve next, Container container) {
        super(deploymentContext, next, container);
    }

    @Override
    public boolean isAsyncSupported() {
        return true;
    }
}
