package freenet.winterface.web;

import java.net.MalformedURLException;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.RequestHandlerStack;
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.time.Duration;

import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.HighLevelSimpleClientImpl;
import freenet.clients.http.FProxyFetchResult;
import freenet.keys.FreenetURI;
import freenet.winterface.core.Configuration;
import freenet.winterface.web.core.AjaxFallbackTimerBehavior;
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
	/** Model to retrieve latest results from */
	private final LoadableDetachableModel<FProxyFetchResult> resultModel;

	/** Interval to refresh progress */
	private final static Duration RELOAD_DURATION = Duration.milliseconds(3000);
	/** Parameter key for max retries */
	private final static String PARAM_MAX_RETRIES = "max-retries";
	/** Header key for max size */
	private final static String HEADER_MAX_SIZE = "max-size";

	/** Log4j logger */
	private final static Logger logger = Logger.getLogger(FreenetURIPage.class);

	public FreenetURIPage(PageParameters params) {
		super(params);
		path = parametersToPath(params);
		// Make sure user browser data is already collected
		// Its best to call this in instructor since it makes
		// a redirection a data gathering page
		getSession().getClientInfo();

		FetchContext cntx = generateFetchContext();
		// Result model
		resultModel = new LoadableDetachableModel<FProxyFetchResult>() {
			@Override
			protected FProxyFetchResult load() {
				return FreenetURIPage.this.getLatestResult();
			}
		};
		try {
			if (canSendProgress()) {
				// Try to init the progress (if not available already)
				logger.debug("Attempting to init fetch for path " + path);
				trackerManager().initProgress(path, cntx);
			} else {
				// block and wait until data is available then return data
				logger.debug("Attempting to wait forever to feth data for path " + path);
				FProxyFetchResult result = trackerManager().getWaitForeverResult(path, cntx);
				throw new RequestHandlerStack.ReplaceHandlerException(new FreenetURIHandler(result), false);
			}
		} catch (FetchException e) {
			logger.error("Error while fetching Freenet URI", e);
		} catch (MalformedURLException e) {
			logger.error("Error while parsing FreenetURI", e);
			throw new AbortWithHttpErrorCodeException(404);
		}
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		FProxyFetchResult latestResult = getLatestResult();
		if (!latestResult.isFinished()) {
			final FetchProgressPanel resultsPanel = new FetchProgressPanel("progress", new CompoundPropertyModel<FProxyFetchResult>(resultModel));
			resultsPanel.setOutputMarkupId(true);
			add(resultsPanel);

			// Add timer to refresh progress
			// Its important to add path in case of meta-refresh. It just
			// overrides the page versioning which causes event loss.
			String refreshURL = "/" + path;
			AjaxFallbackTimerBehavior refreshBehavior = new AjaxFallbackTimerBehavior(RELOAD_DURATION, refreshURL) {
				@Override
				protected void onTimer(AjaxRequestTarget target) {
					if (FreenetURIPage.this.getLatestResult().isFinished()) {
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
			// TODO FProxyToadlet#L814 follows redirects for CSS and PNG. DO IT!
			logger.debug("Passing data to " + FreenetURIHandler.class.getName());
			throw new RequestHandlerStack.ReplaceHandlerException(new FreenetURIHandler(latestResult), false);
		}
	}

	/**
	 * Fetches {@link FetchTrackerManager} from {@link WinterfaceApplication}
	 * 
	 * @return {@link FetchTrackerManager}
	 */
	private FetchTrackerManager trackerManager() {
		FetchTrackerManager trackerManager = ((WinterfaceApplication) getApplication()).getTrackerManager();
		return trackerManager;
	}

	private FetchContext generateFetchContext() {
		// Tracker Manager
		FetchTrackerManager trackerManager = trackerManager();
		HighLevelSimpleClientImpl client = trackerManager.getClient();
		// Configuration
		Configuration config = ((WinterfaceApplication) getApplication()).getConfiguration();
		// Max size in HTTP header
		HttpServletRequest request = getHttpServletRequest();
		long maxLength = config.getMaxLength();
		// max-retries
		// Less than -1 = use default.
		// 0 = one try only, don't retry
		// 1 = two tries
		// 2 = three tries
		// 3 or more = GO INTO COOLDOWN EVERY 3 TRIES! TAKES *MUCH* LONGER!!!
		// STRONGLY NOT RECOMMENDED!!!
		int maxRetries = getPageParameters().get(PARAM_MAX_RETRIES).toInt(-2);
		boolean restricted = (config.isPublicGateway() && !isAllowedFullAccess());
		if (restricted) {
			String maxSize = request.getHeader(HEADER_MAX_SIZE);
			maxLength = (maxSize == null ? maxLength : Long.valueOf(maxSize));
			maxRetries = -2;
		}
		FetchContext result = new FetchContext(client.getFetchContext(), FetchContext.IDENTICAL_MASK, true, null);
		result.maxOutputLength = maxLength;
		if (maxRetries >= -1) {
			result.maxNonSplitfileRetries = maxRetries;
			result.maxSplitfileBlockRetries = maxRetries;
		}
		return result;
	}

	/**
	 * Implodes the {@link PageParameters} to a {@link String}, which is later
	 * used to generate a {@link FreenetURI}
	 * 
	 * @param params
	 *            indexed {@link PageParameters}
	 * @return A {@link FreenetURI} in {@link String} format
	 */
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

	/**
	 * Returns latest {@link FProxyFetchResult} for the current
	 * {@link FreenetURI}
	 * 
	 * @return latest fetch results
	 */
	// TODO Let FetchTrackerManager manage fetches with regard to page ID
	private FProxyFetchResult getLatestResult() {
		FProxyFetchResult latestResult = null;
		try {
			latestResult = trackerManager().getResult(path);
		} catch (MalformedURLException e) {
			// Should never happen, since FetchTrackerManager#initProgress has
			// already been called in constructor
			logger.error(e);
		}
		return latestResult;
	}

	private boolean canSendProgress() {
		WebClientInfo clientInfo = (WebClientInfo) getSession().getClientInfo();
		// If client is a browser
		String ua = clientInfo.getUserAgent();
		boolean isBrowser = (ua == null) ? false : ((ua.contains("Mozilla/") || (ua.contains("Opera/"))));
		// What does browser expect
		String accept = getHttpServletRequest().getHeader("accept");
		boolean htmlRequest = (accept != null) ? (accept.indexOf("text/html") > -1) : true;
		// Force download is set
		StringValue force = getPageParameters().get("forcedownload");
		return isBrowser && htmlRequest && (force.isNull());
	}

	@Override
	public boolean isVersioned() {
		// Provide no versioning for this page
		return false;
	}

}
