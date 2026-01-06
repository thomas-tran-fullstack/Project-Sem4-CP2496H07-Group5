package controllers;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;

@WebFilter("/*")
public class EncodingFilter implements Filter {
    private String encoding = "UTF-8";
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String enc = filterConfig.getInitParameter("encoding");
        if (enc != null && !enc.isEmpty()) encoding = enc;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        request.setCharacterEncoding(encoding);
        response.setCharacterEncoding(encoding);
        response.setContentType("text/html; charset=" + encoding);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
