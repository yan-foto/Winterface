package freenet.winterface.core;

import org.apache.log4j.Logger;
import org.apache.wicket.protocol.http.ContextParamWebApplicationFactory;
import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.protocol.http.WicketServlet;
import org.apache.wicket.util.time.Duration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;


/**
 * A class to take care of {@link Server}
 * 
 * @author pausb
 * 
 */
public class ServerWrapper {

	/**
	 * Server Port
	 */
	private static int port = 8080;

	/**
	 * Idle time for a connection
	 */
	// FIXME change this for deployment mode
	private static int idle_timeout = (int) Duration.ONE_HOUR.getMilliseconds();

	/**
	 * An instance of running server
	 */
	private static Server server;

	/**
	 * Log4j logger
	 */
	private final static Logger logger = Logger.getLogger(ServerWrapper.class);

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
	public static Server startServer(boolean devMode) {
		if (server == null) {
			server = new Server();
			SocketConnector connector = new SocketConnector();

			// Set some timeout options to make debugging easier.
			connector.setMaxIdleTime(idle_timeout);
			connector.setSoLingerTime(-1);
			connector.setPort(port);
			server.addConnector(connector);

			ServletContextHandler sch = new ServletContextHandler(ServletContextHandler.SESSIONS);
			ServletHolder sh = new ServletHolder(WicketServlet.class);
			sh.setInitParameter(ContextParamWebApplicationFactory.APP_CLASS_PARAM, WinterfaceApplication.class.getName());
			sh.setInitParameter(WicketFilter.FILTER_MAPPING_PARAM, "/*");
			if (!devMode) {
				sh.setInitParameter("wicket.configuration", "deployment");
			}
			sch.addServlet(sh, "/*");

			// Static resources
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

			server.setHandler(sch);

			try {
				logger.info("Starting Jetty Server on port " + port);
				server.start();
			} catch (Exception e) {
				logger.error("Error by server startup!", e);
			}
		}
		return server;
	}

	/**
	 * Return current instance of {@link Server}
	 * <p>
	 * Note if {@link #startServer(boolean)} has never been invoked or server
	 * has been terminated, this method returns {@code null}
	 * </p>
	 * 
	 * @return current instance of {@link Server}
	 */
	public static Server getServer() {
		return server;
	}

	/**
	 * Terminates {@link Server} (if running)
	 */
	public static void terminateServer() {
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
