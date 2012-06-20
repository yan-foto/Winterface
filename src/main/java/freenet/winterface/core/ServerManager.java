package freenet.winterface.core;

import org.apache.log4j.Logger;
import org.apache.wicket.protocol.http.ContextParamWebApplicationFactory;
import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.protocol.http.WicketServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
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
	public Server startServer(boolean devMode,final FreenetWrapper fw) {
		if (server == null) {
			server = new Server();
			SocketConnector connector = new SocketConnector();

			// Set some timeout options to make debugging easier.
			connector.setMaxIdleTime(Configuration.getIdleTimeout());
			connector.setSoLingerTime(-1);
			connector.setPort(Configuration.getPort());
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
			
			/*
			 * Add PluginRespirator to servlet context
			 * So it can be retrievable by our WebApplication
			 */
			sch.setAttribute(FREENET_ID, fw);
			
			server.setHandler(sch);

			try {
				logger.info("Starting Jetty Server on port " + Configuration.getPort());
				server.start();
			} catch (Exception e) {
				logger.error("Error by server startup!", e);
			}
		}
		return server;
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
