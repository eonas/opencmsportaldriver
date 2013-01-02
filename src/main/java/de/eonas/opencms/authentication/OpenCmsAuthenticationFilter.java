package de.eonas.opencms.authentication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsUser;
import org.opencms.main.CmsSessionInfo;
import org.opencms.main.CmsSessionManager;
import org.opencms.main.OpenCms;
import org.opencms.security.I_CmsAuthorizationHandler;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

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
				CmsObject cmsobject = authmgr.initCmsObject(r);
				LOG.trace("cmsobject: " + cmsobject);
				CmsSessionInfo sesinfo = sesmgr.getSessionInfo(r);
				LOG.trace("cmssession: " + sesinfo);

				CmsUser cmsuser = null;
				try {
					if (sesinfo != null)
						cmsuser = cmsobject.readUser(sesinfo.getUserId());
				} catch (org.opencms.main.CmsException e) {
					LOG.info("Could not read CMS object", e);
				}

				request = new OpenCmsAuthenticatedRequest(r, cmsobject, sesinfo, cmsuser, guestuser);

				filterChain.doFilter(request, response);

				if (cmsuser == null && sesinfo != null && cmsobject.readUser(sesinfo.getUserId()) != null ) {
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
}
