package de.eonas.opencms.authentication;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsUser;
import org.opencms.main.CmsSessionInfo;
import org.opencms.main.CmsSessionManager;
import org.opencms.main.OpenCms;
import org.opencms.security.I_CmsAuthorizationHandler;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * OpenCms MUST be initialised already.
 */
@SuppressWarnings("UnusedDeclaration")
public class OpenCmsAuthenticationFilter implements Filter {

    private static final Log LOG = LogFactory.getLog(OpenCmsAuthenticationFilter.class);

    @Nullable
    private CmsSessionManager sesmgr = null;
    @Nullable
    private I_CmsAuthorizationHandler authmgr = null;
    private String guestuser = "guest";

    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            sesmgr = OpenCms.getSessionManager();
            authmgr = OpenCms.getAuthorizationHandler();
            guestuser = OpenCms.getDefaultUsers().getUserGuest();
            LOG.debug("Initialised authentication filter");
        } catch (Throwable t) {
            LOG.error("Could not initialise OpenCMS authentication: ", t);
            throw new ServletException(t);
        }
    }

    public void destroy() {
        authmgr = null;
    }

    public void doFilter(ServletRequest request, ServletResponse response, @NotNull FilterChain filterChain) throws IOException, ServletException {
        try {

            // we've already wrapped this request
            if (request instanceof OpenCmsAuthenticatedRequest)
                return;

            // add opencms authentication info to httpservletrequests
            if ((request instanceof HttpServletRequest) && authmgr != null && sesmgr != null) {

                HttpServletRequest r = (HttpServletRequest) request;
                LOG.trace(r.getRequestURI());

                // this registers an authenticated session if not previously
                // available
                CmsObject cmsObject = authmgr.initCmsObject(r);
                LOG.trace("cmsObject: " + cmsObject);
                CmsSessionInfo sesinfo = sesmgr.getSessionInfo(r);
                LOG.trace("cmssession: " + sesinfo);
                String uri = cmsObject.getRequestContext().getUri();
                List<Locale> availableLocales = null;
                if (uri != null) {
                    try {
                        CmsProperty localeProperty = cmsObject.readPropertyObject(uri, "locale", true);
                        String localeString = localeProperty.getValue();
                        availableLocales = extractLocales(localeString);
                    } catch (org.opencms.security.CmsPermissionViolationException ex) {
                        LOG.info("Unable to read target locale due to permission restrictions");
                    } catch (org.opencms.file.CmsVfsResourceNotFoundException ex) {
                        LOG.debug("Page access not a page outside of the CMS, no language detection.");
                    }
                }

                CmsUser cmsuser = null;
                try {
                    if (sesinfo != null)
                        cmsuser = cmsObject.readUser(sesinfo.getUserId());
                } catch (org.opencms.main.CmsException e) {
                    LOG.info("Could not read CMS object", e);
                }

                request = new OpenCmsAuthenticatedRequest(r, cmsObject, sesinfo, cmsuser, guestuser, availableLocales);

                filterChain.doFilter(request, response);

                if (cmsuser == null && sesinfo != null && cmsObject.readUser(sesinfo.getUserId()) != null) {
                    LOG.trace("Freshly authenticated session");
                }
            } else {
                filterChain.doFilter(request, response);
            }

        } catch (Throwable t) {
            LOG.error("Authentication filter failed.", t);
            throw new ServletException(t);
        }
    }

    private List<Locale> extractLocales(String localeString) {
        if (localeString == null) return null;
        List<Locale> list = new ArrayList<Locale>();

        String[] localeStringList = localeString.split(",");
        for (String s : localeStringList) {
            if (s != null) {
                Locale loc = LocaleUtils.toLocale(s);
                list.add(loc);
            }
        }
        return list;
    }
}
