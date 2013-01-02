package de.eonas.opencms;

import org.jetbrains.annotations.NotNull;
import org.opencms.flex.CmsFlexRequest;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

public class FlexRequestWithoutDispatcher implements HttpServletRequest {
	private CmsFlexRequest request;
	private HttpServletRequest parent;

	public FlexRequestWithoutDispatcher(@NotNull CmsFlexRequest request) {
		this.request = request;
		this.parent = (HttpServletRequest) request.getRequest();
	}

	public RequestDispatcher getRequestDispatcher(String target) {
		return parent.getRequestDispatcher(target);
	}

	// delegates after this
	public Map<String, Object> addAttributeMap(Map<String, Object> arg0) {
		return request.addAttributeMap(arg0);
	}

	public Map<String, String[]> addParameterMap(Map<String, String[]> arg0) {
		return request.addParameterMap(arg0);
	}

	public boolean equals(Object obj) {
		return request.equals(obj);
	}

	public Object getAttribute(String name) {
		return request.getAttribute(name);
	}

	public Map<String, Object> getAttributeMap() {
		return request.getAttributeMap();
	}

	public Enumeration<String> getAttributeNames() {
		return request.getAttributeNames();
	}

	public String getAuthType() {
		return request.getAuthType();
	}

	public String getCharacterEncoding() {
		return request.getCharacterEncoding();
	}

	public int getContentLength() {
		return request.getContentLength();
	}

	public String getContentType() {
		return request.getContentType();
	}

	public String getContextPath() {
		return request.getContextPath();
	}

	public Cookie[] getCookies() {
		return request.getCookies();
	}

	public long getDateHeader(String name) {
		return request.getDateHeader(name);
	}

	public String getElementRootPath() {
		return request.getElementRootPath();
	}

	public String getElementUri() {
		return request.getElementUri();
	}

	public String getHeader(String name) {
		return request.getHeader(name);
	}

	@SuppressWarnings("rawtypes")
	public Enumeration getHeaderNames() {
		return request.getHeaderNames();
	}

	@SuppressWarnings("rawtypes")
	public Enumeration getHeaders(String name) {
		return request.getHeaders(name);
	}

	public ServletInputStream getInputStream() throws IOException {
		return request.getInputStream();
	}

	public int getIntHeader(String name) {
		return request.getIntHeader(name);
	}

	public String getLocalAddr() {
		return request.getLocalAddr();
	}

	public String getLocalName() {
		return request.getLocalName();
	}

	public int getLocalPort() {
		return request.getLocalPort();
	}

	public Locale getLocale() {
		return request.getLocale();
	}

	@SuppressWarnings("rawtypes")
	public Enumeration getLocales() {
		return request.getLocales();
	}

	public String getMethod() {
		return request.getMethod();
	}

	public String getParameter(String name) {
		return request.getParameter(name);
	}

	public Map<String, String[]> getParameterMap() {
		return request.getParameterMap();
	}

	public Enumeration<String> getParameterNames() {
		return request.getParameterNames();
	}

	public String[] getParameterValues(String name) {
		return request.getParameterValues(name);
	}

	public String getPathInfo() {
		return request.getPathInfo();
	}

	public String getPathTranslated() {
		return request.getPathTranslated();
	}

	public String getProtocol() {
		return request.getProtocol();
	}

	public String getQueryString() {
		return request.getQueryString();
	}

	public BufferedReader getReader() throws IOException {
		return request.getReader();
	}

	public String getRealPath(String path) {
		return request.getRealPath(path);
	}

	public String getRemoteAddr() {
		return request.getRemoteAddr();
	}

	public String getRemoteHost() {
		return request.getRemoteHost();
	}

	public int getRemotePort() {
		return request.getRemotePort();
	}

	public String getRemoteUser() {
		return request.getRemoteUser();
	}

	public ServletRequest getRequest() {
		return request.getRequest();
	}

	public String getRequestURI() {
		return request.getRequestURI();
	}

	public StringBuffer getRequestURL() {
		return request.getRequestURL();
	}

	public String getRequestedSessionId() {
		return request.getRequestedSessionId();
	}

	public String getScheme() {
		return request.getScheme();
	}

	public String getServerName() {
		return request.getServerName();
	}

	public int getServerPort() {
		return request.getServerPort();
	}

	public String getServletPath() {
		return request.getServletPath();
	}

	public HttpSession getSession() {
		return request.getSession();
	}

	public HttpSession getSession(boolean create) {
		return request.getSession(create);
	}

	public Principal getUserPrincipal() {
		return request.getUserPrincipal();
	}

	public int hashCode() {
		return request.hashCode();
	}

	public boolean isDoRecompile() {
		return request.isDoRecompile();
	}

	public boolean isOnline() {
		return request.isOnline();
	}

	public boolean isRequestedSessionIdFromCookie() {
		return request.isRequestedSessionIdFromCookie();
	}

	public boolean isRequestedSessionIdFromURL() {
		return request.isRequestedSessionIdFromURL();
	}

	public boolean isRequestedSessionIdFromUrl() {
		return request.isRequestedSessionIdFromUrl();
	}

	public boolean isRequestedSessionIdValid() {
		return request.isRequestedSessionIdValid();
	}

	public boolean isSecure() {
		return request.isSecure();
	}

	public boolean isUserInRole(String role) {
		return request.isUserInRole(role);
	}

	public void removeAttribute(String name) {
		request.removeAttribute(name);
	}

	public void setAttribute(String name, Object value) {
		request.setAttribute(name, value);
	}

	public void setAttributeMap(Map<String, Object> map) {
		request.setAttributeMap(map);
	}

	public void setCharacterEncoding(String enc) throws UnsupportedEncodingException {
		request.setCharacterEncoding(enc);
	}

	public void setParameterMap(Map<String, String[]> map) {
		request.setParameterMap(map);
	}

	public String toString() {
		return request.toString();
	}

}
