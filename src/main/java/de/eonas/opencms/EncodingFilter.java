package de.eonas.opencms;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class EncodingFilter implements Filter {

	// default to UTF-8
	@Nullable
    private String targetEncoding = "UTF-8";

	public void init(FilterConfig config) throws ServletException {
		// this.targetEncoding = config.getInitParameter("encoding");
	}

	public void destroy() {
		targetEncoding = null;
	}

	public void doFilter(ServletRequest srequest, ServletResponse sresponse, @NotNull FilterChain chain) throws java.io.IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) srequest;
		if (request.getCharacterEncoding() == null) {
			request.setCharacterEncoding(targetEncoding);
		}
		// move on to the next
		chain.doFilter(srequest, sresponse);
	}
}
