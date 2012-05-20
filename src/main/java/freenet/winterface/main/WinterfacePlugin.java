package freenet.winterface.main;

import org.apache.wicket.protocol.http.ContextParamWebApplicationFactory;
import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.protocol.http.WicketServlet;
import org.apache.wicket.util.time.Duration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginThreadless;
import freenet.pluginmanager.FredPluginVersioned;
import freenet.pluginmanager.PluginRespirator;
import freenet.winterface.web.WinterfaceApplication;

public class WinterfacePlugin implements FredPlugin, FredPluginThreadless,
		FredPluginVersioned {
	
	private Server server;

//	static final Logger logger = Logger.getLogger(WinterfacePlugin.class);

	@Override
	public void runPlugin(PluginRespirator pr) {
		initServer();
	}

	@Override
	public void terminate() {
		try {
			server.stop();
			server.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	public void initServer() {
		int timeout = (int) Duration.ONE_HOUR.getMilliseconds();

		server = new Server();
		SocketConnector connector = new SocketConnector();

		// Set some timeout options to make debugging easier.
		connector.setMaxIdleTime(timeout);
		connector.setSoLingerTime(-1);
		connector.setPort(8080);
		server.addConnector(connector);

		ServletContextHandler sch = new ServletContextHandler(ServletContextHandler.SESSIONS);
		ServletHolder sh = new ServletHolder(WicketServlet.class);
		sh.setInitParameter(ContextParamWebApplicationFactory.APP_CLASS_PARAM, WinterfaceApplication.class.getName());
		sh.setInitParameter(WicketFilter.FILTER_MAPPING_PARAM, "/*");
		sch.addServlet(sh, "/*");
		
		server.setHandler(sch);
		

		try {
			System.out
					.println(">>> STARTING EMBEDDED JETTY SERVER, PRESS ANY KEY TO STOP");
			server.start();
			System.in.read();
			System.out.println(">>> STOPPING EMBEDDED JETTY SERVER");
			terminate();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	public static void main(String[] args) {
		WinterfacePlugin p = new WinterfacePlugin();
		p.runPlugin(null);
	}

}
