package freenet.winterface.web;

import java.net.MalformedURLException;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.atmosphere.AtmosphereBehavior;
import org.apache.wicket.atmosphere.EventBus;
import org.apache.wicket.atmosphere.Subscribe;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException;
import org.atmosphere.cpr.AtmosphereResourceEvent;

import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.HighLevelSimpleClientImpl;
import freenet.clients.http.FProxyFetchInProgress;
import freenet.clients.http.FProxyFetchResult;
import freenet.clients.http.FProxyFetchWaiter;
import freenet.keys.FreenetURI;
import freenet.winterface.core.Configuration;
import freenet.winterface.web.core.FetchTrackerManager;
import freenet.winterface.web.core.WinterfaceApplication;
import freenet.winterface.web.markup.FetchProgressPanel;

/**
 * {@link WinterPage} to fetch {@link FreenetURI}s and show the fetch progress
 * 
 * @author pausb
 * @see FetchTrackerManager
 * @see FetchProgressPanel
 */
@SuppressWarnings("serial")
public class FreenetURIPage extends WinterPage {

	/** {@link FreenetURI} to be fetched */
	private transient FreenetURI uri;
	/** Manages fetching monitor */
	private transient FetchTrackerManager trackerManager;
	/** Use this to get progress by first page load */
	private transient FProxyFetchInProgress progress;
	/** {@link Panel} which renders the progress */
	private FetchProgressPanel progressPanel;
	
	private IModel<Float> progressModel;

	private boolean connected;
	private final transient EventBus eventBus;

	/** Log4j logger */
	private static final Logger logger = Logger.getLogger(FreenetURIPage.class);
	
	public FreenetURIPage() {
		eventBus = ((WinterfaceApplication)getApplication()).getEventBus();
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		connected = false;
		add(new FetchAtmosphereBehavior(this));
		// Parse path and create FreenetURI
		String path = getRequest().getUrl().canonical().toString();
		path = path.replace("wicket/bookmarkable/freenet.winterface.web.FreenetURIPage/", "");
		try {
			uri = new FreenetURI(path);
		} catch (MalformedURLException e) {
			logger.error("Error while parsing FreenetURI", e);
			throw new AbortWithHttpErrorCodeException(404);
		}
		// Tracker Manager
		trackerManager = ((WinterfaceApplication) getApplication()).getTrackerManager();
		HighLevelSimpleClientImpl client = trackerManager.getClient();
		// Max size in HTTP header
		HttpServletRequest request = getHttpServletRequest();
		long maxLength = Configuration.getMaxLength();
		boolean restricted = (Configuration.isPublicGateway() && !isAllowedFullAccess());
		if (restricted) {
			String maxSize = request.getHeader("max-size");
			maxLength = (maxSize == null ? maxLength : Long.valueOf(maxSize));
		}
		FetchContext cntx = new FetchContext(client.getFetchContext(), FetchContext.IDENTICAL_MASK, true, null);
		cntx.maxOutputLength = maxLength;
		try {
			logger.debug("Attempting to fetch URI: " + uri);
			progress = trackerManager.getProgress(uri, maxLength, cntx).progress;
		} catch (FetchException e) {
			logger.error("Error while fetching Freenet URI", e);
		}
		progressModel = Model.of(getProgressPercent(progress));
		progressPanel = new FetchProgressPanel("content", progressModel);
		progressPanel.setOutputMarkupId(true);
		add(progressPanel);
	}

	@Subscribe
	public void receiveEvent(AjaxRequestTarget target, FProxyFetchInProgress progress) {
		if (!progress.uri.equals(uri) || !connected) {
			// Does not interest us
			return;
		}
		logger.debug("Pushing progress of URI "+progress.uri);
		progressModel.setObject(getProgressPercent(progress));
		progressPanel = (FetchProgressPanel) progressPanel.replaceWith(new FetchProgressPanel("content", progressModel));
		target.add(progressPanel);
	}

	/**
	 * Returns current progress in percent.
	 * <p>
	 * This method is used to generate a value for progress tag in
	 * {@link FetchProgressPanel}
	 * </p>
	 * 
	 * @param waiter
	 *            {@link FProxyFetchWaiter} to get current progress from
	 * @return 0.0 &lt; progress &lt; 1.0
	 */
	private float getProgressPercent(FProxyFetchInProgress progress) {
		FProxyFetchResult resultFast = progress.getWaiter().getResultFast();
		float result = -1.0f;
		if (resultFast.requiredBlocks > 0) {
			result = resultFast.fetchedBlocks / resultFast.requiredBlocks;
		}
		resultFast.close();
		return result;
	}

	private class FetchAtmosphereBehavior extends AtmosphereBehavior {

		final FreenetURIPage parent;

		private FetchAtmosphereBehavior(FreenetURIPage parent) {
			this.parent = parent;
		}

		@Override
		public void onResourceRequested() {
			super.onResourceRequested();
			logger.info("Connection established.");
			parent.connected = true;
			eventBus.post(progress);
		}

		@Override
		public void onDisconnect(AtmosphereResourceEvent arg0) {
			super.onDisconnect(arg0);
			logger.info("Connection disconnected.");
			parent.connected = false;
		}

	};

}
