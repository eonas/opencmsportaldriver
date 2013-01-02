package de.eonas.opencms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opencms.flex.CmsFlexRequest;

import javax.el.ELContext;
import javax.servlet.*;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.ErrorData;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.tagext.BodyContent;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Enumeration;

@SuppressWarnings("UnusedDeclaration")
public class PortletPageContext extends javax.servlet.jsp.PageContext {
	
	private static final Log LOG = LogFactory.getLog(PortletPageContext.class);

	private javax.servlet.jsp.PageContext delegate;
	private FlexRequestWithoutDispatcher request;
	
	public PortletPageContext ( @NotNull javax.servlet.jsp.PageContext delegate ) {
		this.delegate = delegate;
		this.request = new FlexRequestWithoutDispatcher ( (CmsFlexRequest) delegate.getRequest() );
	}

	public ServletRequest getRequest() {
		return request;
	}

	// delegates after this
	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object arg0) {
		return delegate.equals(arg0);
	}

	public Object findAttribute(String arg0) {
		return delegate.findAttribute(arg0);
	}

	public void forward(String arg0) throws ServletException, IOException {
		delegate.forward(arg0);
	}

	public Object getAttribute(String arg0, int arg1) {
		return delegate.getAttribute(arg0, arg1);
	}

	public Object getAttribute(String arg0) {
		return delegate.getAttribute(arg0);
	}

	@SuppressWarnings("rawtypes")
	public Enumeration getAttributeNamesInScope(int arg0) {
		return delegate.getAttributeNamesInScope(arg0);
	}

	public int getAttributesScope(String arg0) {
		return delegate.getAttributesScope(arg0);
	}

	public ErrorData getErrorData() {
		return delegate.getErrorData();
	}

	public Exception getException() {
		return delegate.getException();
	}

	public ExpressionEvaluator getExpressionEvaluator() {
		return delegate.getExpressionEvaluator();
	}

	public JspWriter getOut() {
		return delegate.getOut();
	}

	public Object getPage() {
		return delegate.getPage();
	}

	public ServletResponse getResponse() {
		return delegate.getResponse();
	}

	public ServletConfig getServletConfig() {
		return delegate.getServletConfig();
	}

	public ServletContext getServletContext() {
		return delegate.getServletContext();
	}

	public HttpSession getSession() {
		return delegate.getSession();
	}

	public VariableResolver getVariableResolver() {
		return delegate.getVariableResolver();
	}

	public void handlePageException(Exception arg0) throws ServletException, IOException {
		delegate.handlePageException(arg0);
	}

	public void handlePageException(Throwable arg0) throws ServletException, IOException {
		delegate.handlePageException(arg0);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public void include(String arg0, boolean arg1) throws ServletException, IOException {
		delegate.include(arg0, arg1);
	}

	public void include(String arg0) throws ServletException, IOException {
		delegate.include(arg0);
	}

	public void initialize(Servlet arg0, ServletRequest arg1, ServletResponse arg2, String arg3, boolean arg4, int arg5, boolean arg6) throws IOException,
			IllegalStateException, IllegalArgumentException {
		delegate.initialize(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
	}

	public JspWriter popBody() {
		return delegate.popBody();
	}

	public BodyContent pushBody() {
		return delegate.pushBody();
	}

	public JspWriter pushBody(Writer writer) {
		return delegate.pushBody(writer);
	}

	public void release() {
		delegate.release();
	}

	public void removeAttribute(String arg0, int arg1) {
		delegate.removeAttribute(arg0, arg1);
	}

	public void removeAttribute(String arg0) {
		delegate.removeAttribute(arg0);
	}

	public void setAttribute(String arg0, Object arg1, int arg2) {
		delegate.setAttribute(arg0, arg1, arg2);
	}

	public void setAttribute(String arg0, Object arg1) {
		delegate.setAttribute(arg0, arg1);
	}

	public String toString() {
		return delegate.toString();
	}

	@Nullable
    @SuppressWarnings("UnusedDeclaration")
    public javax.el.ELContext getELContext() {
		try {
			Class<? extends javax.servlet.jsp.PageContext> class1 = delegate.getClass();
			Method method = class1.getMethod("getELContext");
			return (ELContext) method.invoke(delegate);
		} catch (Exception e) {
			LOG.fatal("Unable to pass getELContext-Call to delegate", e);
			return null;
		}
	}
}
