package de.eonas.opencms.responses;

import org.apache.pluto.container.PortletContainer;
import org.apache.pluto.container.PortletWindow;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("UnusedDeclaration")
public class PortletResourceResponseContextImpl extends org.apache.pluto.driver.services.container.PortletResourceResponseContextImpl {
    public PortletResourceResponseContextImpl(PortletContainer container, HttpServletRequest containerRequest, HttpServletResponse containerResponse, PortletWindow window) {
        super(container, containerRequest, containerResponse, window);
    }

    @Override
    public void addProperty(String key, String value) {
        if (!isClosed()) {
            HttpServletResponse servletResponse = getServletResponse();
            if (isNull(key)) return;
            if (isNull(value)) return;

            servletResponse.addHeader(key, value);
        }
    }

    @SuppressWarnings("SimplifiableIfStatement")
    boolean isNull(String s) {
        if (s == null) return true;
        return s.length() == 0;
    }
}
