package uk.ac.ox.oucs.oauth.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.Authentication;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import uk.ac.ox.oucs.oauth.service.OAuthHttpService;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;

/**
 * Last filter applied for the OAuth protocol
 * <p>
 * Gets the current user from the principal and log the user in.
 * </p>
 *
 * @author Colin Hebert
 */
public class OAuthPostFilter implements Filter {
    private OAuthHttpService oAuthHttpService;
    private SessionManager sessionManager;
    private UserDirectoryService userDirectoryService;
    private UsageSessionService usageSessionService;
    //private AuthenticationManager authenticationManager;
    private final static Log log = LogFactory.getLog(OAuthPostFilter.class);


    public void init(FilterConfig filterConfig) throws ServletException {
        oAuthHttpService = (OAuthHttpService) ComponentManager.getInstance().get(OAuthHttpService.class.getCanonicalName());
        sessionManager = (SessionManager) ComponentManager.getInstance().get(SessionManager.class.getCanonicalName());
        userDirectoryService = (UserDirectoryService) ComponentManager.getInstance().get(UserDirectoryService.class.getCanonicalName());
        usageSessionService = (UsageSessionService) ComponentManager.getInstance().get(UsageSessionService.class.getCanonicalName());
        //authenticationManager = (AuthenticationManager) ComponentManager.getInstance().get(AuthenticationManager.class.getCanonicalName());
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        //Only apply filter if there is an OAuth implementation and a valid OAuth request
        if (oAuthHttpService == null || !oAuthHttpService.isEnabled() || !oAuthHttpService.isValidOAuthRequest(req, res)) {
            chain.doFilter(req, response);
            return;
        }

        Principal principal = req.getUserPrincipal();
        //Do not log the user in if there is already an opened session
        if (principal != null && sessionManager.getCurrentSessionUserId() == null) {
            try {
                //Force the authentication/login with the user Eid
                final String eid = userDirectoryService.getUserEid(principal.getName());
                final String uid = principal.getName();

                // TODO This is a hack and we should go through the AuthenticationManager API.
                Authentication authentication = new Authentication() {

                    @Override
                    public String getUid() {
                        return uid;
                    }

                    @Override
                    public String getEid() {
                        return eid;
                    }
                };

                //Authentication authentication = authenticationManager.authenticate(new ExternalTrustedEvidence() {
                //    public String getIdentifier() {
                //        return eid;
                //    }
                //});
                usageSessionService.login(authentication, req);
            } catch (UserNotDefinedException e) {
                log.warn("Failed to find user \"" + principal.getName() + "\". This shouldn't happen", e);
            }
        }
        chain.doFilter(req, res);
    }

    public void destroy() {
    }
}
