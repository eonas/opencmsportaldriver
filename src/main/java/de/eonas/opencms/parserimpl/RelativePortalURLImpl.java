/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.eonas.opencms.parserimpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pluto.driver.AttributeKeys;
import org.apache.pluto.driver.config.DriverConfiguration;
import org.apache.pluto.driver.services.portal.PageConfig;
import org.apache.pluto.driver.url.PortalURL;
import org.apache.pluto.driver.url.PortalURLParameter;
import org.apache.pluto.driver.url.PortalURLParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.portlet.PortletMode;
import javax.portlet.WindowState;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The portal URL.
 *
 * @since 1.0
 */
public class RelativePortalURLImpl implements PortalURL, Serializable {

    private static final Log LOG = LogFactory.getLog(RelativePortalURLImpl.class);

    private String renderPath;
    private String actionWindow;
    private String resourceWindow;
    private String cacheLevel;
    private String resourceID;

    private Map<String, String[]> publicParameterCurrent = new HashMap<String, String[]>();
    @NotNull
    private Map<String, String[]> publicParameterNew = new HashMap<String, String[]>();
    @NotNull
    private Map<String, String[]> privateRenderParameters = new HashMap<String, String[]>();

    transient private String urlBase;
    transient private String servletPath;
    transient private String contextPath;
    transient private String httpSessionId;
    transient private PortalURLParser urlParser;
    transient private Map<String, WindowState> windowStates = new HashMap<String, WindowState>();
    transient private Map<String, PortletMode> portletModes = new HashMap<String, PortletMode>();
    transient private Map<String, PortalURLParameter> parameters = new HashMap<String, PortalURLParameter>();

    /**
     * Internal private constructor used by method <code>clone()</code>.
     *
     * @see #clone()
     */
    public RelativePortalURLImpl() {
        // Do nothing.
    }

    public void setTransients(String urlBase, String servletPath, PortalURLParser urlParser, String httpSessionId, String contextPath) {
        this.urlBase = urlBase;
        this.servletPath = servletPath;
        this.urlParser = urlParser;
        this.httpSessionId = httpSessionId;
        this.contextPath = contextPath;
    }

    // Public Methods ----------------------------------------------------------

    public void setRenderPath(String renderPath) {
        this.renderPath = renderPath;
    }

    public String getRenderPath() {
        return renderPath;
    }

    public void addParameter(@NotNull PortalURLParameter param) {
        parameters.put(param.getWindowId() + param.getName(), param);
    }

    public Collection<PortalURLParameter> getParameters() {
        return parameters.values();
    }

    public void setActionWindow(String actionWindow) {
        this.actionWindow = actionWindow;
    }

    public String getActionWindow() {
        return actionWindow;
    }

    public Map<String, PortletMode> getPortletModes() {
        return Collections.unmodifiableMap(portletModes);
    }

    public PortletMode getPortletMode(String windowId) {
        PortletMode mode = portletModes.get(windowId);
        if (mode == null) {
            mode = PortletMode.VIEW;
        }
        return mode;
    }

    public void setPortletMode(String windowId, PortletMode portletMode) {
        portletModes.put(windowId, portletMode);
    }

    public Map<String, WindowState> getWindowStates() {
        return Collections.unmodifiableMap(windowStates);
    }

    /**
     * Returns the window state of the specified window.
     *
     * @param windowId the window ID.
     * @return the window state. Default to NORMAL.
     */
    public WindowState getWindowState(String windowId) {
        WindowState state = windowStates.get(windowId);
        if (state == null) {
            state = WindowState.NORMAL;
        }
        return state;
    }

    /**
     * Sets the window state of the specified window.
     *
     * @param windowId    the window ID.
     * @param windowState the window state.
     */
    public void setWindowState(String windowId, WindowState windowState) {
        this.windowStates.put(windowId, windowState);
    }

    /**
     * Clear parameters of the specified window.
     *
     * @param windowId the window ID.
     */
    public void clearParameters(String windowId) {
        for (Iterator<Map.Entry<String, PortalURLParameter>> it = parameters.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, PortalURLParameter> entry = it.next();
            PortalURLParameter param = entry.getValue();
            if (param.getWindowId() != null) {
                if (param.getWindowId().equals(windowId)) {
                    it.remove();
                }
            }
        }
    }

    public void setCacheability(String cacheLevel) {
        this.cacheLevel = cacheLevel;
    }

    public String getCacheability() {
        return cacheLevel;
    }

    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }

    public String getResourceID() {
        return resourceID;
    }

    /**
     * Converts to a string representing the portal URL.
     *
     * @return a string representing the portal URL.
     * @see PortalURLParserImpl#toString(org.apache.pluto.driver.url.PortalURL)
     * @deprecated use toURL(boolean absolute) instead
     */
    public String toString() {
        return toURL(false);
    }

    /**
     * Converts to a string representing the portal URL.
     *
     * @return a string representing the portal URL.
     * @see PortalURLParserImpl#toString(org.apache.pluto.driver.url.PortalURL)
     */
    public String toURL(boolean absolute) {
        return urlParser.toString(this);
    }

    /**
     * Returns the server URI (protocol, name, port).
     *
     * @return the server URI portion of the portal URL.
     * @deprecated
     */
    @Nullable
    @Deprecated
    public String getServerURI() {
        return null;
    }

    /**
     * Returns the servlet path (context path + servlet name).
     *
     * @return the servlet path.
     */
    public String getServletPath() {
        return servletPath;
    }

    /**
     * Clone a copy of itself.
     *
     * @return a copy of itself.
     */
    @NotNull
    @Override
    public synchronized PortalURL clone() {
        RelativePortalURLImpl portalURL = new RelativePortalURLImpl();
        portalURL.urlBase = this.urlBase;
        portalURL.servletPath = this.servletPath;
        portalURL.contextPath = this.contextPath;
        portalURL.parameters = new HashMap<String, PortalURLParameter>(parameters);
        portalURL.privateRenderParameters = new HashMap<String, String[]>(privateRenderParameters);
        portalURL.portletModes = new HashMap<String, PortletMode>(portletModes);
        portalURL.windowStates = new HashMap<String, WindowState>(windowStates);
        portalURL.cacheLevel = cacheLevel;
        portalURL.resourceID = resourceID;
        portalURL.renderPath = renderPath;
        portalURL.actionWindow = actionWindow;
        portalURL.urlParser = urlParser;
        portalURL.resourceWindow = resourceWindow;
        portalURL.publicParameterCurrent = publicParameterCurrent;
        portalURL.httpSessionId = httpSessionId;
        return portalURL;
    }
//JSR-286 methods

    public void addPublicRenderParametersNew(@NotNull Map<String, String[]> parameters) {
        for (String key : parameters.keySet()) {
            if (publicParameterNew.containsKey(key)) {
                publicParameterNew.remove(key);
            }
            String[] values = parameters.get(key);
            publicParameterNew.put(key, values);
        }
    }


    public void addPublicParameterCurrent(String name, String[] values) {
        publicParameterCurrent.put(name, values);
    }

    public void addPublicParameterActionResourceParameter(String parameterName, String value) {
        //add at the first position
        if (publicParameterCurrent.containsKey(parameterName)) {
            String[] tmp = publicParameterCurrent.get(parameterName);

            String[] values = new String[tmp.length + 1];
            values[0] = value;
            System.arraycopy(tmp, 0, values, 1, tmp.length);
            publicParameterCurrent.remove(parameterName);
            publicParameterCurrent.put(parameterName, values.clone());
        } else
            publicParameterCurrent.put(parameterName, new String[]{value});
    }

    @NotNull
    public Map<String, String[]> getPublicParameters() {
        Map<String, String[]> tmp = new HashMap<String, String[]>();

        for (String paramName : publicParameterCurrent.keySet()) {
            if (!publicParameterNew.containsKey(paramName)) {
                String[] paramValue = publicParameterCurrent.get(paramName);
                tmp.put(paramName, paramValue);
            }
        }
        for (String paramName : publicParameterNew.keySet()) {
            String[] paramValue = publicParameterNew.get(paramName);
            if (paramValue[0] != null) {
                tmp.put(paramName, paramValue);
            }
        }
        return tmp;
    }

    @NotNull
    public Map<String, String[]> getNewPublicParameters() {
        return publicParameterNew;
    }

    @NotNull
    public Map<String, String[]> getPrivateRenderParameters() {
        return privateRenderParameters;
    }


    public PageConfig getPageConfig(@NotNull ServletContext servletContext) {
        String requestedPageId = getRenderPath();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Requested Page: " + requestedPageId);
        }
        return ((DriverConfiguration) servletContext.getAttribute(
                AttributeKeys.DRIVER_CONFIG)).getPageConfig(requestedPageId);
    }

    public String getResourceWindow() {
        return resourceWindow;
    }

    public void setResourceWindow(String resourceWindow) {
        this.resourceWindow = resourceWindow;
    }

    public synchronized void merge(@NotNull PortalURL url, @NotNull String windowId) {
        actionWindow = url.getActionWindow();
        resourceWindow = url.getResourceWindow();
        setPortletMode(windowId, url.getPortletMode(windowId));
        setWindowState(windowId, url.getWindowState(windowId));
        setCacheability(url.getCacheability());
        setResourceID(url.getResourceID());
        clearParameters(windowId);
        for (PortalURLParameter param : url.getParameters()) {
            if (windowId.equals(param.getWindowId())) {
                addParameter(new PortalURLParameter(param.getWindowId(), param.getName(), param.getValues()));
            }
        }
        Map<String, String[]> newPublicParameters = url.getNewPublicParameters();
        for (Map.Entry<String, String[]> entry : newPublicParameters.entrySet()) {
            if (entry.getValue()[0] == null) {
                publicParameterCurrent.remove(entry.getKey());
            } else {
                publicParameterCurrent.put(entry.getKey(), entry.getValue());
            }
        }
    }

    static final MapObjectStreamTool<PortalURLParameter> parameterStreamTool = new MapObjectStreamTool<PortalURLParameter>() {
        @NotNull
        @Override
        protected PortalURLParameter readObject(@NotNull StringReader stream) throws IOException, ClassNotFoundException {
            String name = readString(stream);
            String[] values = readArrayOfString(stream);
            String windowId = readString(stream);
            return new PortalURLParameter(windowId, name, values);
        }

        @Override
        protected void writeObject(@NotNull StringWriter stream, @NotNull PortalURLParameter url) throws IOException {
            final String name = url.getName();
            final String[] arrayOfValues = url.getValues();
            final String windowId = url.getWindowId();

            writeString(stream, name);
            writeArrayOfString(stream, arrayOfValues);
            writeString(stream, windowId);
        }
    };

    static final MapObjectStreamTool<String[]> arrayStreamTool = new MapObjectStreamTool<String[]>() {
        @NotNull
        @Override
        protected String[] readObject(StringReader stream) throws IOException, ClassNotFoundException {
            return readArrayOfString(stream);
        }

        @Override
        protected void writeObject(StringWriter out, String[] o) throws IOException {
            writeArrayOfString(out, o);
        }
    };

    static final MapObjectStreamTool<WindowState> windowStreamTool = new MapObjectStreamTool<WindowState>() {
        @NotNull
        @Override
        protected WindowState readObject(@NotNull StringReader stream) throws IOException, ClassNotFoundException {
            String name = readString(stream);
            return new WindowState(name);
        }

        @Override
        protected void writeObject(@NotNull StringWriter stream, @NotNull WindowState state) throws IOException {
            final String name = state.toString();
            writeString(stream, name);
        }
    };

    static final MapObjectStreamTool<PortletMode> portletModeStreamTool = new MapObjectStreamTool<PortletMode>() {
        @NotNull
        @Override
        protected PortletMode readObject(@NotNull StringReader stream) throws IOException, ClassNotFoundException {
            String name = readString(stream);
            return new PortletMode(name);
        }

        @Override
        protected void writeObject(@NotNull StringWriter stream, @NotNull PortletMode state) throws IOException {
            final String name = state.toString();
            writeString(stream, name);
        }
    };


    public String getHttpSessionId() {
        return httpSessionId;
    }

    public String getUrlBase() {
        return urlBase;
    }

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }

    @NotNull
    public String toDebugString() {
        return "RelativePortalURLImpl [\n" +
                " renderPath=" + renderPath + "\n" +
                " actionWindow=" + actionWindow + "\n" +
                " resourceWindow=" + resourceWindow + "\n" +
                " cacheLevel=" + cacheLevel + "\n" +
                " resourceID=" + resourceID + "\n" +
                " publicParameterCurrent=" + publicParameterCurrent + "\n" +
                " publicParameterNew=" + publicParameterNew + "\n" +
                " privateRenderParameters=" + privateRenderParameters + "\n" +
                " urlBase=" + urlBase + "\n" +
                " servletPath=" + servletPath + "\n" +
                " httpSessionId=" + httpSessionId + "\n" +
                " urlParser=" + urlParser + "\n" +
                " windowStates=" + windowStates + "\n" +
                " portletModes=" + portletModes + "\n" +
                " parameters=" + toDebugString(parameters) + "]";
    }

    @Nullable
    private String toDebugString(@Nullable Map<String, PortalURLParameter> params) {
        if (params == null) return null;

        StringBuilder b = new StringBuilder();

        for (Map.Entry<String, PortalURLParameter> e : params.entrySet()) {
            PortalURLParameter p = e.getValue();
            b.append("(key " + e.getKey() + " = " + Arrays.asList(p.getValues()) + "( " + p.getWindowId() + ")");
        }
        return b.toString();
    }

    public void writeToStream(StringWriter out) throws IOException {
        MapObjectStreamTool.writeString(out, renderPath);
        MapObjectStreamTool.writeString(out, actionWindow);
        MapObjectStreamTool.writeString(out, resourceWindow);
        MapObjectStreamTool.writeString(out, cacheLevel);
        MapObjectStreamTool.writeString(out, resourceID);
        arrayStreamTool.writeMap(out, publicParameterCurrent);
        arrayStreamTool.writeMap(out, publicParameterNew);
        arrayStreamTool.writeMap(out, privateRenderParameters);
        windowStreamTool.writeMap(out, windowStates);
        portletModeStreamTool.writeMap(out, portletModes);
        parameterStreamTool.writeMap(out, parameters);
    }

    public void readFromStream(StringReader in) throws IOException, ClassNotFoundException {
        renderPath = MapObjectStreamTool.readString(in);
        actionWindow = MapObjectStreamTool.readString(in);
        resourceWindow = MapObjectStreamTool.readString(in);
        cacheLevel = MapObjectStreamTool.readString(in);
        resourceID = MapObjectStreamTool.readString(in);
        publicParameterCurrent = arrayStreamTool.readMap(in);
        publicParameterNew = arrayStreamTool.readMap(in);
        privateRenderParameters = arrayStreamTool.readMap(in);
        windowStates = windowStreamTool.readMap(in);
        portletModes = portletModeStreamTool.readMap(in);
        parameters = parameterStreamTool.readMap(in);
    }

    public void convertToSharedResource() {
        urlBase = contextPath + "/" + PortalURLParserImpl.SHARED;
        renderPath = null;
        actionWindow = null;
        cacheLevel = null;
        resourceID = null;
        publicParameterCurrent = new HashMap<String, String[]>();
        publicParameterNew = new HashMap<String, String[]>();
        privateRenderParameters = new HashMap<String, String[]>();
        windowStates = new HashMap<String, WindowState>();
        portletModes = new HashMap<String, PortletMode>();
        /* do not remove parameters, but rewrite to shared domain */
        Set<PortalURLParameter> parameterSet = new HashSet<PortalURLParameter>();
        for ( Iterator<Map.Entry<String, PortalURLParameter>> iterator = parameters.entrySet().iterator(); iterator.hasNext(); )
        {
            Map.Entry<String, PortalURLParameter> param = iterator.next();
            PortalURLParameter value = param.getValue();
            if ( value.getWindowId().equals(resourceWindow) ) {
                PortalURLParameter newParam = new PortalURLParameter(PortalURLParserImpl.SHARED, value.getName(), value.getValues());
                parameterSet.add(newParam);
            }
            iterator.remove();
        }
        for ( PortalURLParameter p: parameterSet )
        {
            addParameter(p);
        }

        resourceWindow = PortalURLParserImpl.SHARED;
    }

    public String getContextPath() {
        return contextPath;
    }
}

