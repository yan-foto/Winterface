package freenet.winterface.web;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

import freenet.winterface.web.core.WinterMapper;

/**
 * {@link WebApplication} of Winterface.
 * 
 * @author pausb
 * 
 */
public class WinterfaceApplication extends WebApplication {

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
		WinterMapper.setDelegate(getRootRequestMapper());
		setRootRequestMapper(WinterMapper.getInstance());
	}

	@Override
	public Class<? extends Page> getHomePage() {
		return Dashboard.class;
	}

}
