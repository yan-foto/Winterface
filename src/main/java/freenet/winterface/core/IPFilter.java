package freenet.winterface.core;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
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

	/** List of urls not to block **/
	private final static List<String> whiteUrls = Arrays.asList("/error", "/static");

	/** Log4j Logger */
	private final static Logger logger = Logger.getLogger(IPFilter.class);

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		String configAllowed = filterConfig.getInitParameter(ALLOWED_HOSTS_PARAM);
		logger.info("Filter initiated with following hosts: " + configAllowed);
		allowedHosts = configAllowed.split(",");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		String path = ((HttpServletRequest) request).getServletPath();
		String remoteAddr = request.getRemoteAddr();
		boolean unblock = false;
		for (String allowed : allowedHosts) {
			try {
				unblock |= IPUtils.quietMatches(allowed, remoteAddr);
			} catch (UnknownHostException e){
				logger.error("Error while matching allowed hosts and remoter address.",e);
				unblock = false;
			}
		}
		if (unblock || whiteUrls.contains(path)) {
			chain.doFilter(request, response);
			return;
		}
		logger.debug("Blocked request from " + remoteAddr + " on path " + path);
		((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
	}

	@Override
	public void destroy() {
	}

}
