package freenet.winterface.core;

import org.apache.log4j.Logger;

import freenet.node.Node;
import freenet.pluginmanager.PluginRespirator;

/**
 * A class to manage all Freenet related functionalities.
 * 
 * @author pausb
 * @see Node
 */
public class FreenetManager {

	/**
	 * Log4j logger
	 */
	private static final Logger logger = Logger.getLogger(FreenetManager.class);

	/**
	 * {@link PluginRespirator} for {@link WinterfacePlugin}
	 */
	private static PluginRespirator _pr;

	/**
	 * Directory containing files overwriting default templates
	 * (HTML/CSS/JS/...)
	 */
	// TODO do something!
	public static final String WinterFaceResources = "webinterface/";

	/**
	 * Does not constructs!
	 */
	private FreenetManager() {
		// Making it uninstantiable
	}

	/**
	 * Initializes {@link FreenetManager} and assigns a {@link PluginRespirator}
	 * to it.
	 * <p>
	 * This method is supposed to be called only once during the lifetime of
	 * {@link WinterfacePlugin}, otherwise it throws an
	 * {@link IllegalStateException}.
	 * </p>
	 * 
	 * @param pr
	 *            {@link PluginRespirator} of {@link WinterfacePlugin}
	 */
	public static void init(PluginRespirator pr) {
		if (_pr == null) {
			_pr = pr;
			logger.info("FreenetManager initialized.");
		} else {
			throw new IllegalStateException("Already initialized (by WinterfacePlugin) and cannot be changed again!");
		}
	}

	/**
	 * Returns {@link PluginRespirator} of {@link WinterfacePlugin}
	 * 
	 * @return {@link PluginRespirator} of {@link WinterfacePlugin}
	 */
	public PluginRespirator getPluginRespirator() {
		return _pr;
	}

	/**
	 * Returns Freenet running {@link Node}
	 * 
	 * @return freenet's {@link Node}
	 */
	public static Node getNode() {
		if (_pr != null) {
			return _pr.getNode();
		}
		throw new IllegalStateException("FreenetManager doesnt seem to be initialized!");
	}

}
