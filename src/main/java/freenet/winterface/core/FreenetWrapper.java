package freenet.winterface.core;

import freenet.clients.http.bookmark.BookmarkManager;
import freenet.node.Node;
import freenet.pluginmanager.PluginRespirator;

/**
 * Manages all Freenet related functionalities.
 * 
 * @author pausb
 * @see Node
 */
public class FreenetWrapper {

	/** {@link PluginRespirator} for {@link WinterfacePlugin} */
	private final PluginRespirator pr;

	/** {@link BookmarkManager} to manipulate bookmark items */
	private final BookmarkManager bookmarkManager;

	/**
	 * Directory containing files overwriting default templates
	 * (HTML/CSS/JS/...)
	 */
	// TODO this is not implemented yet.
	public static final String WinterFaceResources = "webinterface/";

	/**
	 * Constructs {@link FreenetManager} and assigns a {@link PluginRespirator}
	 * to it.
	 * 
	 * @param pr
	 *            {@link PluginRespirator} of {@link WinterfacePlugin}
	 */
	public FreenetWrapper(PluginRespirator pr) {
		this.pr = pr;
		// TODO NodeClientCore will provide an instance of BookmarkManager
		// Make changes respectively
		this.bookmarkManager = new BookmarkManager(pr.getNode().clientCore);
	}

	/**
	 * Returns {@link PluginRespirator} of {@link WinterfacePlugin}
	 * 
	 * @return {@link PluginRespirator} of {@link WinterfacePlugin}
	 */
	public PluginRespirator getPluginRespirator() {
		return pr;
	}

	/**
	 * Returns Freenet running {@link Node}
	 * 
	 * @return freenet's {@link Node}
	 */
	public Node getNode() {
		return pr.getNode();
	}

	/**
	 * Returns Freenet {@link BookmarkManager}
	 * 
	 * @return {@link BookmarkManager}
	 */
	public BookmarkManager getBookmarkManager() {
		return bookmarkManager;
	}

}
