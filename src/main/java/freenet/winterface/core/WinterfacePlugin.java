package freenet.winterface.core;

import java.net.URL;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;

import freenet.config.SubConfig;
import freenet.l10n.BaseL10n.LANGUAGE;
import freenet.node.Node;
import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginConfigurable;
import freenet.pluginmanager.FredPluginThreadless;
import freenet.pluginmanager.FredPluginVersioned;
import freenet.pluginmanager.PluginRespirator;

/**
 * Winterface {@link FredPlugin}
 * <p>
 * Replaces FProxy with Apache Wicket and Jetty web server.
 * </p>
 * 
 * @author pasub
 * 
 */
public class WinterfacePlugin implements FredPlugin, FredPluginThreadless, FredPluginVersioned, FredPluginConfigurable {

	/**
	 * {@link URL} at which {@link WinterfacePlugin} resides
	 */
	private URL plugin_path;

	/**
	 * An instance of {@link ServerManager} for all {@link Server} related
	 * functionalities
	 */
	private ServerManager serverManager;

	/** Current version */
	private final static String VERSION = "0.1";

	/**
	 * True if in development mode. Change to {@code false} to switch to
	 * deployment mode
	 */
	private final static boolean DEV_MODE = true;

	/**
	 * Log4j logger
	 */
	private static final Logger logger = Logger.getLogger(WinterfacePlugin.class);
	
	@Override
	public void runPlugin(PluginRespirator pr) {
		// Load path
		plugin_path = this.getClass().getClassLoader().getResource(".");
		// Register logger and so on
		logger.debug("Loaded WinterFacePlugin on path " + plugin_path);
		// initServer();
		serverManager = new ServerManager();
		serverManager.startServer(DEV_MODE, new FreenetWrapper(pr));
	}

	@Override
	public void terminate() {
		serverManager.terminateServer();
	}

	@Override
	public String getVersion() {
		return VERSION;
	}

	/**
	 * Just for test cases if {@link Node} is not needed
	 * 
	 * @param args
	 *            start arguments
	 */
	public static void main(String[] args) {
		WinterfacePlugin p = new WinterfacePlugin();
		p.runPlugin(null);
	}

	@Override
	public String getString(String arg0) {
		// Part of FredPluginL10n (which is not yet implemented)
		// So we just ignore the translation for now
		return arg0;
	}

	@Override
	public void setLanguage(LANGUAGE arg0) {
		// Part of FredPluginL10n (which is not yet implemented)
		// Cannot throw exception, otherwise the plug-in wont start!
		// throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void setupConfig(SubConfig subconfig) {
		Configuration.initialize(subconfig);
	}

}
