package freenet.winterface.web.core;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

import freenet.winterface.core.FreenetWrapper;
import freenet.winterface.core.ServerManager;
import freenet.winterface.web.Dashboard;

/**
 * {@link WebApplication} of Winterface.
 * 
 * @author pausb
 * 
 */
public class WinterfaceApplication extends WebApplication {
	
	private FreenetWrapper freenetWrapper;

	@Override
	protected void init() {
		super.init();
		// New resource path
		// TODO Add this for theming etc.
		// String resourcePath = FreenetManager.getNode().getNodeDir().getPath()
		// + FreenetManager.WinterFaceResources;
		// IResourceSettings resourceSettings = getResourceSettings();
		// resourceSettings.addResourceFolder(resourcePath);
		// logger.info("Added wicket resource path at " + resourcePath);
		// Gather all browser data
		getRequestCycleSettings().setGatherExtendedBrowserInfo(true);
		// Configuring custom mapper
		WinterMapper mapper = new WinterMapper(getRootRequestMapper());
		setRootRequestMapper(mapper);
		freenetWrapper = (FreenetWrapper) getServletContext().getAttribute(ServerManager.FREENET_ID);
		// Add Auto-Linking
		getMarkupSettings().setAutomaticLinking(true);
	}

	@Override
	public Class<? extends Page> getHomePage() {
		return Dashboard.class;
	}
	
	public FreenetWrapper getFreenetWrapper() {
		return freenetWrapper;
	}

}
