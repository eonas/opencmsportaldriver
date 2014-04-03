package de.eonas.opencms.parserimpl;

import junit.framework.TestCase;
import org.apache.pluto.driver.url.PortalURL;
import org.apache.pluto.driver.url.PortalURLParameter;
import org.apache.pluto.driver.url.PortalURLParser;
import org.jetbrains.annotations.NotNull;

import javax.portlet.PortletMode;
import javax.portlet.WindowState;
import java.io.IOException;

public class PortalURLParserImplTest extends TestCase {

    public void testEncode() throws IOException, ClassNotFoundException {
        PortalURLParser parser = PortalURLParserImpl.getParser();
        PortalURLParserImpl parserImpl = (PortalURLParserImpl) parser;

        RelativePortalURLImpl portalURL1 = new RelativePortalURLImpl();
        String[] values = {"val1", "val"};
        portalURL1.addPublicParameterCurrent("key", values);
        portalURL1.setResourceWindow("Window");

        String s = parser.toString(portalURL1);
        System.out.println(s.length());

        loopback(parser, parserImpl, portalURL1);

        RelativePortalURLImpl portalURL2 = new RelativePortalURLImpl();
        String[] values2 = {"val1", "val", "val3", "val4", "val5", "val6"};
        portalURL2.addPublicParameterCurrent("key", values2);
        portalURL2.setResourceWindow("Window");
        PortalURLParameter param = new PortalURLParameter("paramwindow", "paramkey", "paramvalue");
        PortalURLParameter param2 = new PortalURLParameter("paramwindow", "paramkey2", "paramvalue2");
        PortalURLParameter param3 = new PortalURLParameter("paramwindow2", "paramkey2", "paramvalue2");
        portalURL2.addParameter(param);
        portalURL2.addParameter(param2);
        portalURL2.addParameter(param3);
        WindowState windowState = new WindowState("state");
        portalURL2.setWindowState("windowId", windowState);
        PortletMode portletMode = new PortletMode("windowId");
        portalURL2.setPortletMode("windowId", portletMode);

        loopback(parser, parserImpl, portalURL2);
    }

    public void testSharedUrl() throws IOException, ClassNotFoundException {
        PortalURLParser parser = PortalURLParserImpl.getParser();
        PortalURLParserImpl parserImpl = (PortalURLParserImpl) parser;

        RelativePortalURLImpl portalURL1 = new RelativePortalURLImpl();
        portalURL1.setTransients("/cms/asdfasd", "/cms", parser, "noSession", "");
        portalURL1.setResourceWindow("window");
        PortalURLParameter param = new PortalURLParameter("window", "ln", "primefaces");
        PortalURLParameter param2 = new PortalURLParameter("window", "javax.faces.resource", "somefile.css");
        portalURL1.addParameter(param);
        portalURL1.addParameter(param2);

        loopback(parser, parserImpl, portalURL1);

        String url = parser.toString(portalURL1);
        PortalURL parsedUrl = parserImpl.getPortalURL("noSession", "", url, null);
        System.out.println(parsedUrl.toURL(true));
    }


    private void loopback(PortalURLParser parser, @NotNull PortalURLParserImpl parserImpl, @NotNull RelativePortalURLImpl portalURL1) throws IOException, ClassNotFoundException {
        portalURL1.setTransients("urlBase", "servletPath", parser, "httpSessionId", "");
        String text = parserImpl.serializePortalURL(portalURL1);
        byte[] bytes = text.getBytes("utf-8");
        RelativePortalURLImpl portalURL = parserImpl.deSerializePortalUrl(bytes);
        portalURL.setTransients("urlBase", "servletPath", parser, "httpSessionId", "");
        assertEquals(portalURL.toURL(true), portalURL1.toURL(true));
    }
}
