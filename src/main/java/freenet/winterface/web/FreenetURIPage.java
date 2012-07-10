package freenet.winterface.web;

import java.net.MalformedURLException;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.atmosphere.Subscribe;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException;

import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.HighLevelSimpleClientImpl;
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
	private transient FProxyFetchWaiter initialWaiter;
	/** {@link Panel} which renders the progress */
	private FetchProgressPanel progressPanel;

	/** Log4j logger */
	private static final Logger logger = Logger.getLogger(FreenetURIPage.class);

	/**
	 * Constructs
	 */
	public FreenetURIPage() {
		super();
		// Fix for Atmosphere module
		// Session.get().bind();
		// Parse path and create FreenetURI
		String path = getRequest().getUrl().canonical().toString();
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
			initialWaiter = trackerManager.getProgress(uri, maxLength, cntx);
		} catch (FetchException e) {
			logger.error("Error while fetching Freenet URI", e);
		}
		progressPanel = new FetchProgressPanel("content", Model.of(getProgressPercent(initialWaiter)));
		add(progressPanel);
	}

	@Subscribe
	public void receiveEvent(AjaxRequestTarget target, FProxyFetchWaiter waiter) {
		logger.info("received update for " + waiter.progress.uri);
	}

	@Override
	protected void onDetach() {
		initialWaiter.close();
		super.onDetach();
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
	private float getProgressPercent(FProxyFetchWaiter waiter) {
		FProxyFetchResult resultFast = waiter.getResultFast();
		float result;
		if (resultFast.requiredBlocks < 0) {
			result = -1;
		} else {
			result = resultFast.fetchedBlocks / resultFast.requiredBlocks;
		}
		resultFast.close();
		return result;
	}

}
