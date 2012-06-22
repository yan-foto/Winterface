package freenet.winterface.core;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * {@link Filter} for IP filtering.
 * <p>
 * If remote host IP is not included in allowed hosts of Winterface, the request
 * is not processed further and a {@link ServletException} is thrown.
 * </p>
 * 
 * @author pausb
 * @see Configuration
 */
public class IPFilter implements Filter {

	/** List of allowedHosts as read from filter config */
	private String[] allowedHosts;

	/** Filter parameter name containing allowed hosts */ 
	public final static String ALLOWED_HOSTS_PARAM = "allowedHosts";

	/** Log4j Logger */
	public final static Logger logger = Logger.getLogger(IPFilter.class);

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		String configAllowed = filterConfig.getInitParameter(ALLOWED_HOSTS_PARAM);
		logger.info("Filter initiated with following hosts: "+configAllowed);
		allowedHosts = configAllowed.split(",");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		String remoteHost = request.getRemoteHost();
		for (String allowed : allowedHosts) {
			if (remoteHost.equals(allowed)) {
				chain.doFilter(request, response);
				return;
			}
		}
		logger.debug("Blocked request from "+remoteHost);
		((HttpServletResponse)response).setStatus(HttpServletResponse.SC_FORBIDDEN);
		((HttpServletResponse)response).sendRedirect("/403");
	}

	@Override
	public void destroy() {
	}

}
