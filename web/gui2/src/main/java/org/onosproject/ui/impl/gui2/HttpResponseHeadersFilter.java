package org.onosproject.ui.impl.gui2;
 
import java.io.IOException;
 
import javax.servlet.Filter;  
import javax.servlet.FilterConfig;  
import javax.servlet.FilterChain; 
import javax.servlet.ServletException; 
import javax.servlet.ServletRequest; 
import javax.servlet.ServletResponse; 
import javax.servlet.http.HttpServletResponse;
 
 
public class HttpResponseHeadersFilter implements Filter {
	
    @Override
    public void init(FilterConfig filterconfig){}

    @Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
	    HttpServletResponse resp = (HttpServletResponse) response;
        resp.setHeader("Content-Security-Policy", "frame-ancestors 'none'");
		chain.doFilter(request, resp);
	}

    @Override
    public void destroy(){}

}