package de.eonas.opencms;

import java.io.IOException;

import javax.portlet.PortletException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.pluto.container.PortletContainer;
import org.apache.pluto.container.PortletContainerException;
import org.apache.pluto.driver.AttributeKeys;
import org.apache.pluto.driver.core.PortalRequestContext;
import org.apache.pluto.driver.core.PortletWindowImpl;
import org.apache.pluto.driver.services.portal.PortletWindowConfig;
import org.apache.pluto.driver.url.PortalURL;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The controller filter used to drive static portlet pages (see <a
 * href="http://portals.apache.org/pluto/faq.html#simple-embed">
 * http://portals.apache.org/pluto/faq.html#simple-embed</a> in Pluto FAQ).
 * 
 * @version 1.0
 * @since March 28, 2006
 */
@SuppressWarnings("UnusedDeclaration")
public class PortalDriverFilter implements Filter {

	/**
	 * Internal Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(PortalDriverFilter.class);

	/**
	 * The Portal Driver sServlet Context
	 */
	@Nullable
    private ServletContext servletContext;

	/**
	 * The portlet container to which we will forward all portlet requests.
	 */
	@Nullable
    protected PortletContainer container;

	@Nullable
    public ServletContext getServletContext() {
		return servletContext;
	}

	/**
	 * Initialize the Portal Driver. This method retrieves the portlet container
	 * instance from the servlet context scope.
	 * 
	 * @see org.apache.pluto.container.PortletContainer
	 */
	public void init(@NotNull FilterConfig filterConfig) throws ServletException {
		servletContext = filterConfig.getServletContext();
		container = (PortletContainer) servletContext.getAttribute(AttributeKeys.PORTLET_CONTAINER);
	}

	/**
	 * Release the container and the context.
	 */
	public void destroy() {
		container = null;
		servletContext = null;
	}

	/**
	 * Intercept requests in order to perform any actions.
	 * 
	 * @param request
	 *            the incoming ServletRequest.
	 * @param response
	 *            the incoming ServletResponse.
	 * @throws javax.servlet.ServletException
	 *             if an internal error occurs.
	 * @throws java.io.IOException
	 *             if an error occurs writing to the response.
	 */
	public void doFilter(@NotNull ServletRequest request, ServletResponse response, @NotNull FilterChain filterChain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			HttpServletRequest req = (HttpServletRequest) request;
			// Since we must support a 2.3 environment, we can't use
			// filter dispatchers. B/C of this, we make sure we haven't
			// allready processed this request. No infinite loops for us!!!!
			if (PortalRequestContext.getContext(req) == null) {
				boolean actionRequestProcessed = doPortletPrepare(req, (HttpServletResponse) response);

				if (actionRequestProcessed) {
					return;
				}

			}

			String path = req.getServletPath();
			int idx = path.indexOf(".jsp");
			if (!path.endsWith(".jsp") && idx > 0) {
				String realPath = path.substring(0, idx + ".jsp".length());
				if (realPath.startsWith(req.getContextPath())) {
					realPath = realPath.substring(req.getContextPath().length());
				}
				LOG.info("Forwarding to realPath: " + realPath);
				request.getRequestDispatcher(realPath).forward(request, response);
				return;
			}
		}
		filterChain.doFilter(request, response);
	}

	/**
	 * Perform Portlet Preparation
	 * 
	 * @throws java.io.IOException
	 *             if an io exception occurs
	 * @throws javax.servlet.ServletException
	 *             if a servlet exception occurs
	 * @return A boolean flag indicating whether or not an action request was
	 *         processed. A value of true indicates than an action request was
	 *         processed while a value of false indicates that an action request
	 *         was NOT processed.
	 */
	public boolean doPortletPrepare(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		boolean requestProcessed = false;

		PortalRequestContext portalRequestContext = new PortalRequestContext(getServletContext(), request, response);

		PortalURL portalURL = portalRequestContext.getRequestedPortalURL();
		String actionWindowId = portalURL.getActionWindow();
		String resourceWindowId = portalURL.getResourceWindow();

		PortletWindowConfig resourceWindowConfig = null;
		PortletWindowConfig actionWindowConfig = null;

		if (resourceWindowId != null) {
			resourceWindowConfig = PortletWindowConfig.fromId(resourceWindowId);
		} else if (actionWindowId != null) {
			actionWindowConfig = PortletWindowConfig.fromId(actionWindowId);
		}

		// Action window config will only exist if there is an action request.
		if (actionWindowConfig != null) {
			PortletWindowImpl portletWindow = new PortletWindowImpl(container, actionWindowConfig, portalURL);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Processing action request for window: " + portletWindow.getId().getStringId());
			}
			try {
				container.doAction(portletWindow, request, response);
			} catch (PortletContainerException ex) {
				throw new ServletException(ex);
			} catch (PortletException ex) {
				throw new ServletException(ex);
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("Action request processed.\n\n");
			}

			requestProcessed = true;
		} else if (resourceWindowConfig != null) {
			PortletWindowImpl portletWindow = new PortletWindowImpl(container, resourceWindowConfig, portalURL);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Processing resource Serving request for window: " + portletWindow.getId().getStringId());
			}
			try {
				container.doServeResource(portletWindow, request, response);
			} catch (PortletContainerException ex) {
				LOG.error(ex.getMessage(), ex);
				throw new ServletException(ex);
			} catch (PortletException ex) {
				LOG.error(ex.getMessage(), ex);
				throw new ServletException(ex);
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("Resource serving request processed.\n\n");
			}
			requestProcessed = true;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Render Path: " + portalURL.getRenderPath());
			LOG.debug("Servlet Path: " + portalURL.getServletPath());
		}

		return requestProcessed;
	}
}
