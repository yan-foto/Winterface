package freenet.winterface.web;

import java.net.MalformedURLException;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.RequestHandlerStack;
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.time.Duration;

import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.HighLevelSimpleClientImpl;
import freenet.clients.http.FProxyFetchResult;
import freenet.keys.FreenetURI;
import freenet.winterface.core.Configuration;
import freenet.winterface.web.core.FetchTrackerManager;
import freenet.winterface.web.core.FreenetURIHandler;
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

	/** FreenetURI to be fetched */
	private final String path;

	/** Interval to refresh progress */
	private static final Duration RELOAD_DURATION = Duration.milliseconds(3000);

	/** Log4j logger */
	private static final Logger logger = Logger.getLogger(FreenetURIPage.class);

	public FreenetURIPage(PageParameters params) {
		super(params);
		path = parametersToPath(params);
		// Tracker Manager
		FetchTrackerManager trackerManager = trackerManager();
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
		// Try to init the progress (if not available already)
		try {
			logger.debug("Attempting to init fetch for path " + path);
			trackerManager.initProgress(path, maxLength, cntx);
		} catch (FetchException e) {
			logger.error("Error while fetching Freenet URI", e);
		} catch (MalformedURLException e) {
			logger.error("Error while parsing FreenetURI", e);
			throw new AbortWithHttpErrorCodeException(404);
		}
	}

	private FetchTrackerManager trackerManager() {
		FetchTrackerManager trackerManager = ((WinterfaceApplication) getApplication()).getTrackerManager();
		return trackerManager;
	}

	private String parametersToPath(PageParameters params) {
		int segmentCount = params.getIndexedCount();
		String[] segments = new String[segmentCount];
		for (int i = 0; i < segmentCount; i++) {
			segments[i] = params.get(i).toString();
		}
		String path = segments[0];
		for (int i = 1; i < segments.length; i++) {
			path += ("/" + segments[i]);
		}
		return path;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		FProxyFetchResult latestResult = getLatestResult();
		if (!latestResult.isFinished()) {
			final FetchProgressPanel resultsPanel = new FetchProgressPanel("progress", new CompoundPropertyModel<FProxyFetchResult>(new ResultModel(path)));
			resultsPanel.setOutputMarkupId(true);
			add(resultsPanel);

			// Add timer to refresh progress
			AbstractAjaxTimerBehavior refreshBehavior = new AbstractAjaxTimerBehavior(RELOAD_DURATION) {

				@Override
				protected void onTimer(AjaxRequestTarget target) {
					if (getLatestResult().isFinished()) {
						logger.debug(String.format("Fetching %s finished. Relaoading the page", path));
						setResponsePage(FreenetURIPage.class, FreenetURIPage.this.getPageParameters());
						return;
					}
					target.add(resultsPanel);
				}
			};
			add(refreshBehavior);
		} else {
			/*
			 * Data is available. Get a new FreenetURIHandler to write it to
			 * response
			 */
			throw new RequestHandlerStack.ReplaceHandlerException(new FreenetURIHandler(latestResult), false);
		}
	}

	private FProxyFetchResult getLatestResult() {
		FProxyFetchResult latestResult = null;
		try {
			latestResult = trackerManager().getResult(path);
		} catch (MalformedURLException e) {
			// Should never happen, since FetchTrackerManager#initProgress has
			// already been called in constructor
			e.printStackTrace();
		}
		return latestResult;
	}

	@Override
	public boolean isVersioned() {
		// Provide no versioning for this page
		return false;
	}

//	@Override
//	public void onEvent() {
//		FProxyFetchWaiter waiter = progress.getWaiter();
//		FProxyFetchResult result = waiter.getResultFast();
//		if (result.isFinished()) {
//			logger.debug("Fetching finished for URI " + progress.uri);
//			progress.removeListener(this);
//			return;
//		} else {
//			logger.debug("New fetch results for URI " + progress.uri);
//		}
//		// Close everything we don't need anymore
//		if (waiter != null) {
//			waiter.close();
//		}
//		if (latestResult != null) {
//			latestResult.close();
//		}
//		// Update the latest result
//		latestResult = result;
//	}

	private class ResultModel extends LoadableDetachableModel<FProxyFetchResult> {

		private String path;
		
		public ResultModel(String path) {
			super();
			this.path = path;
		}
		
		@Override
		protected FProxyFetchResult load() {
			FetchTrackerManager trackerManager = FreenetURIPage.this.trackerManager();
			try {
				return trackerManager.getResult(path);
			} catch (MalformedURLException e) {
				// Should never happen
				e.printStackTrace();
			}
			return null;
		}

	}

}
