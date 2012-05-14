package freenet.winterface.main;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;

import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginThreadless;
import freenet.pluginmanager.FredPluginVersioned;
import freenet.pluginmanager.PluginRespirator;

public class WinterfacePlugin implements FredPlugin, FredPluginThreadless, FredPluginVersioned {
	
	static final Logger logger = Logger.getLogger(WinterfacePlugin.class); 

	@Override
	public void runPlugin(PluginRespirator pr) {
		initServer();
	}

	@Override
	public void terminate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void initServer() {
		Server server = new Server(8080);
		try {
			server.start();
		} catch (Exception e) {
			logger.error("Unable to start webserver", e);
		}
		
	}

}
