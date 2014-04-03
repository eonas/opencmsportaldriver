package de.eonas.opencms;

import de.eonas.opencms.responses.PortletResourceResponseContextImpl;
import org.apache.pluto.container.PortletActionResponseContext;
import org.apache.pluto.container.PortletContainer;
import org.apache.pluto.container.PortletEventResponseContext;
import org.apache.pluto.container.PortletRenderResponseContext;
import org.apache.pluto.container.PortletResourceResponseContext;
import org.apache.pluto.container.PortletWindow;
import org.apache.pluto.driver.services.container.PortletActionResponseContextImpl;
import org.apache.pluto.driver.services.container.PortletEventResponseContextImpl;
import org.apache.pluto.driver.services.container.PortletRenderResponseContextImpl;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("UnusedDeclaration")
public class PortletRequestContextService extends org.apache.pluto.driver.services.container.PortletRequestContextServiceImpl {

    @NotNull
    public PortletActionResponseContext getPortletActionResponseContext(PortletContainer container,
                                                                        HttpServletRequest containerRequest,
                                                                        HttpServletResponse containerResponse,
                                                                        PortletWindow window) {
        return new PortletActionResponseContextImpl(container, containerRequest, containerResponse, window);
    }

    @NotNull
    public PortletEventResponseContext getPortletEventResponseContext(PortletContainer container,
                                                                      HttpServletRequest containerRequest,
                                                                      HttpServletResponse containerResponse, PortletWindow window) {
        return new PortletEventResponseContextImpl(container, containerRequest, containerResponse, window);
    }

    @NotNull
    public PortletRenderResponseContext getPortletRenderResponseContext(PortletContainer container,
                                                                        HttpServletRequest containerRequest,
                                                                        HttpServletResponse containerResponse,
                                                                        PortletWindow window) {
        return new PortletRenderResponseContextImpl(container, containerRequest, containerResponse, window);
    }

    @NotNull
    public PortletResourceResponseContext getPortletResourceResponseContext(PortletContainer container,
                                                                            HttpServletRequest containerRequest,
                                                                            HttpServletResponse containerResponse,
                                                                            PortletWindow window) {
        return new PortletResourceResponseContextImpl(container, containerRequest, containerResponse, window);
    }
}
