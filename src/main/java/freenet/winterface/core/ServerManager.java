package freenet.winterface.core;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.wicket.Application;
import org.apache.wicket.protocol.http.ContextParamWebApplicationFactory;
import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.protocol.http.WicketServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import freenet.winterface.web.core.WinterfaceApplication;

/**
 * Takes care of {@link Server}
 * <p>
 * Responsible for:
 * <ul>
 * <li>Starting the server</li>
 * <li>Configuring the server</li>
 * <li>Terminating the server</li>
 * </ul>
 * </p>
 * 
 * @author pausb
 * 
 */
public class ServerManager {

	/**
	 * An instance of running server
	 */
	private Server server;

	/**
	 * Log4j logger
	 */
	private final static Logger logger = Logger.getLogger(ServerManager.class);

	public static final String FREENET_ID = "plugin-respirator";

	public static final String CONFIG_ID = "winterface-configuration";

	/**
	 * Starts {@link Server} in the desired mode.
	 * <p>
	 * Mode can be;
	 * <ul>
	 * <li>{@code true} if in development mode</li>
	 * <li>{@code false} if in deployment mode</li>
	 * <ul>
	 * Starting in development mode also makes Wicket to start in development
	 * mode
	 * </p>
	 * 
	 * @param devMode
	 *            {@code false} to start in deployment mode
	 * @return running instance of {@link Server}
	 */
	public Server startServer(boolean devMode, final Configuration config, final FreenetWrapper fw) {
		if (server == null) {
			server = new Server();

			// Bind
			String[] hosts = config.getBindToHosts().split(",");
			for (String host : hosts) {
				SocketConnector connector = new SocketConnector();
				connector.setMaxIdleTime(config.getIdleTimeout());
				connector.setSoLingerTime(-1);
				connector.setHost(host);
				connector.setPort(config.getPort());
				server.addConnector(connector);
			}

			ServletContextHandler sch = new ServletContextHandler(ServletContextHandler.SESSIONS);
			initIPFilter(sch, config);
			initErrorHandlers(sch);
			initWicketServlet(devMode, sch);
			initStaticResources(sch);

			/*
			 * Add PluginRespirator/Configuration to servlet context So it can
			 * be retrievable by our WebApplication
			 */
			sch.setAttribute(FREENET_ID, fw);
			sch.setAttribute(CONFIG_ID, config);

			server.setHandler(sch);

			try {
				logger.info("Starting Jetty Server on port " + config.getPort());
				server.start();
				server.join();
			} catch (Exception e) {
				logger.error("Error by server startup!", e);
			}
		}
		return server;
	}

	/**
	 * Initializes and configures {@link IPFilter}
	 * 
	 * @param sch
	 *            parent {@link ServletContextHandler}
	 */
	private void initIPFilter(ServletContextHandler sch, Configuration config) {
		FilterHolder fh = new FilterHolder(IPFilter.class);
		fh.setInitParameter(IPFilter.ALLOWED_HOSTS_PARAM, config.getAllowedHosts());
		sch.addFilter(fh, "/*", EnumSet.of(DispatcherType.REQUEST));
	}

	/**
	 * Initializes and configures {@link ErrorHandler}s.
	 * <p>
	 * Currently implemented handlers for:
	 * <ul>
	 * <li>404 Not found ({@link HttpServletResponse#SC_NOT_FOUND})</li>
	 * <li>403 Forbidden ({@link HttpServletResponse#SC_FORBIDDEN})</li>
	 * </ul>
	 * </p>
	 * 
	 * @param sch
	 *            parent {@link ServletContextHandler}
	 */
	private void initErrorHandlers(ServletContextHandler sch) {
		ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
		errorHandler.addErrorPage(HttpServletResponse.SC_NOT_FOUND, "/error");
		errorHandler.addErrorPage(HttpServletResponse.SC_FORBIDDEN, "/error");
		sch.setErrorHandler(errorHandler);
	}

	/**
	 * Initializes and configures {@link WicketServlet}
	 * 
	 * @param devMode
	 *            {@code true} to start Wicket in development mode (see
	 * @param sch
	 */
	private void initWicketServlet(boolean devMode, ServletContextHandler sch) {
		ServletHolder sh = new ServletHolder(WicketServlet.class);
		sh.setInitParameter(ContextParamWebApplicationFactory.APP_CLASS_PARAM, WinterfaceApplication.class.getName());
		sh.setInitParameter(WicketFilter.FILTER_MAPPING_PARAM, "/*");
		if (!devMode) {
			sh.setInitParameter("wicket." + Application.CONFIGURATION, "deployment");
		}

		sch.addServlet(sh, "/*");
	}

	/**
	 * Creates and configures a new {@link Servlet} responsible for resources in
	 * {@code static} folder.
	 * 
	 * @param sch
	 *            parent {@link ServletContextHandler}
	 */
	private void initStaticResources(ServletContextHandler sch) {
		String staticPath = WinterfacePlugin.class.getClassLoader().getResource("static/").toExternalForm();
		ServletHolder resourceServlet = new ServletHolder(DefaultServlet.class);
		resourceServlet.setInitParameter("dirAllowed", "true");
		resourceServlet.setInitParameter("resourceBase", staticPath);
		resourceServlet.setInitParameter("pathInfoOnly", "true");
		// if(DEV_MODE) {
		// resourceServlet.setInitParameter("maxCacheSize", "0");
		// }
		sch.addServlet(resourceServlet, "/static/*");
		logger.debug("Set Jetty to load static resources from " + staticPath);
	}

	/**
	 * Terminates {@link Server} (if running)
	 */
	public void terminateServer() {
		if (server != null) {
			try {
				server.stop();
				server.join();
			} catch (InterruptedException e) {
				logger.error("Error by server shutdown!", e);
			} catch (Exception e) {
				logger.error("Error by server shutdown!", e);
			}
		}
	}
}
