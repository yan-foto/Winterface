package freenet.winterface.web.core;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.cookies.CookieUtils;

import freenet.winterface.core.Configuration;
import freenet.winterface.core.FreenetWrapper;
import freenet.winterface.core.ServerManager;
import freenet.winterface.web.AddFriendPage;
import freenet.winterface.web.AlertsPage;
import freenet.winterface.web.Dashboard;
import freenet.winterface.web.ErrorPage;

/**
 * {@link WebApplication} of Winterface.
 * 
 * @author pausb
 * 
 */
public class WinterfaceApplication extends WebApplication {

	/** FreenetWrapper to interact with node */
	private FreenetWrapper freenetWrapper;
	/** User configuration */
	private Configuration config;

	/**
	 * Tracks {@link FreenetURI}s being fetched
	 */
	private FetchTrackerManager trackerManager;

	private CookieUtils cookieUtils;

	@Override
	protected void init() {
		super.init();
		// Gather all browser data
		getRequestCycleSettings().setGatherExtendedBrowserInfo(true);
		// Configuring custom mapper
		WinterMapper mapper = new WinterMapper(getRootRequestMapper());
		setRootRequestMapper(mapper);
		// Retrieve FreenetWrapper
		freenetWrapper = (FreenetWrapper) getServletContext().getAttribute(ServerManager.FREENET_ID);
		// Setup manager for FProxyFetchTracker
		trackerManager = new FetchTrackerManager(freenetWrapper, this);
		config = (Configuration) getServletContext().getAttribute(ServerManager.CONFIG_ID);
		// Instantiate cookie utils
		cookieUtils = new CookieUtils();
		// Add Auto-Linking
		getMarkupSettings().setAutomaticLinking(true);
		
		initPageMounts();
	}

	@Override
	public Class<? extends Page> getHomePage() {
		return Dashboard.class;
	}

	private void initPageMounts() {
		// Setup error pages
		mountPage("/error", ErrorPage.class);
		// Custom mountings
		mountPage("/addfriend", AddFriendPage.class);
		mountPage("/messages", AlertsPage.class);
	}

	/**
	 * Returns {@link FreenetWrapper}, which contains Freenet related objects
	 * 
	 * @return {@link FreenetWrapper}
	 */
	public FreenetWrapper getFreenetWrapper() {
		return freenetWrapper;
	}

	/**
	 * @return user {@link Configuration}
	 */
	public Configuration getConfiguration() {
		return config;
	}

	/**
	 * Return {@link FetchTrackerManager} responsible to track progress of
	 * {@link FreenetURI}s being fetched
	 * 
	 * @return {@link FetchTrackerManager}
	 */
	public FetchTrackerManager getTrackerManager() {
		return trackerManager;
	}

	public CookieUtils getCookieUtils() {
		return cookieUtils;
	}

}
