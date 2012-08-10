package freenet.winterface.web;

import java.net.MalformedURLException;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.RequestHandlerStack;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.time.Duration;

import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.HighLevelSimpleClientImpl;
import freenet.client.async.USKManager;
import freenet.clients.http.FProxyFetchInProgress;
import freenet.clients.http.FProxyFetchResult;
import freenet.clients.http.FProxyFetchTracker;
import freenet.clients.http.FProxyFetchWaiter;
import freenet.keys.FreenetURI;
import freenet.keys.USK;
import freenet.winterface.core.Configuration;
import freenet.winterface.core.RequestsUtil;
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

	/** Interval to refresh progress */
	private final static Duration RELOAD_DURATION = Duration.milliseconds(1000);

	/** Log4j logger */
	private final static Logger logger = Logger.getLogger(FreenetURIPage.class);

	/**
	 * Constructs.
	 * 
	 * @param params
	 *            contains FreenetURI to fetch
	 */
	public FreenetURIPage(PageParameters params) {
		super(params);
		path = parametersToPath(params);
		// Make sure user browser data is already collected
		// Its best to call this in instructor since it makes
		// a redirection a data gathering page
		getSession().getClientInfo();

		FetchContext cntx = createFetchContext();
		boolean canSendProgress = canSendProgress();

		FProxyFetchWaiter waiter = null;

		// Initiates the fetching progress if it doesn't already exist
		try {
			waiter = getWaiter();
			if (canSendProgress) {
				// If page has a newer version we restart the
				restartIfOutdated(waiter);
			} else {
				// Requested file is an image or css file. A simple redirect is
				// not enough. Result is fetched in a recursive manner
				waiter = enterRecursionIfNeeded(waiter, cntx);
			}
		} catch (FetchException e) {
			// Happens if fetching cannot be started
			logger.error("Error while fetching Freenet URI", e);
			// TODO redirect to error page
		} catch (MalformedURLException e) {
			logger.error("Error while parsing FreenetURI", e);
			throw new AbortWithHttpErrorCodeException(404);
		} finally {
			if (waiter != null) {
				waiter.close();
			}
		}
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		FProxyFetchResult latestResult = getLatestResult();
		if (!latestResult.isFinished()) {
			// Result model
			LoadableDetachableModel<FProxyFetchResult> resultModel = new LoadableDetachableModel<FProxyFetchResult>() {
				@Override
				protected FProxyFetchResult load() {
					return FreenetURIPage.this.getLatestResult();
				}

				@Override
				protected void onDetach() {
					if (isAttached()) {
						// Request is finished, FProxyFetchResult is not needed
						// anymore
						getObject().close();
					}
				}

			};
			final FetchProgressPanel resultsPanel = new FetchProgressPanel("progress", new CompoundPropertyModel<FProxyFetchResult>(resultModel));
			resultsPanel.setOutputMarkupId(true);
			add(resultsPanel);

			// Add timer to refresh progress
			// Its important to add path in case of meta-refresh. It just
			// overrides the page versioning which causes event loss.
			// TODO remove this. Page versioning is not in URL with the new
			// WinterMapper
			String refreshURL = "/" + path;
			AjaxFallbackTimerBehavior refreshBehavior = new AjaxFallbackTimerBehavior(RELOAD_DURATION, refreshURL) {
				@Override
				protected void onTimer(AjaxRequestTarget target) {
					if (FreenetURIPage.this.getLatestResult().isFinished()) {
						logger.debug(String.format("Fetching %s finished. Relaoading the page", path));
						// Restart the page so that data is processed
						// If data is not available the error message is shown
						FreenetURIPage.this.restartPage();
						return;
					}
					target.add(resultsPanel);
				}
			};
			add(refreshBehavior);
		} else {
			// Fetching is finished and may contain data xor fetch exception
			if (latestResult.failed != null) {
				logger.debug("Fetching has failed. Redirecting to error page");
				// Redirect to error page
				throw new RestartResponseAtInterceptPageException(new FetchErrorPage(latestResult, path));
			} else if (latestResult.hasData()) {
				// TODO check for RSS data as in evilHorribleHack of FProxy
				logger.debug("Passing data to " + FreenetURIHandler.class.getName());
				throw new RequestHandlerStack.ReplaceHandlerException(new FreenetURIHandler(latestResult), false);
			}
		}
	}

	/**
	 * Fetches {@link FetchTrackerManager} from {@link WinterfaceApplication}
	 * 
	 * @return {@link FetchTrackerManager}
	 */
	private FetchTrackerManager trackerManager() {
		return ((WinterfaceApplication) getApplication()).getTrackerManager();
	}

	/**
	 * @return {@link FetchContext} with respect to current {@link WebRequest}
	 */
	private FetchContext createFetchContext() {
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
		int maxRetries = getPageParameters().get(RequestsUtil.PARAM_MAX_RETRIES).toInt(-2);
		boolean restricted = (config.isPublicGateway() && !isAllowedFullAccess());
		if (restricted) {
			String maxSize = request.getHeader(RequestsUtil.HEADER_MAX_SIZE);
			maxLength = (maxSize == null ? maxLength : Long.valueOf(maxSize));
			maxRetries = -2;
		}
		FetchContext result = new FetchContext(client.getFetchContext(maxLength), FetchContext.IDENTICAL_MASK, true, null);
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
	private FProxyFetchResult getLatestResult() {
		FProxyFetchInProgress progress = getProgress();
		if (progress == null) {
			// this *shouldn't* happen since we initiated the progress in
			// constructor. If anything went wrong, we simply restart the page
			restartPage();
		}
		return progress.getWaiter().getResultFast();
	}

	/**
	 * Depending on {@link WebClientInfo} of the current {@link Request}, it is
	 * decided if a progress page should be shown or not.
	 * <p>
	 * Returns {@code true} If client is browsing through a browser and requests
	 * data of MIME-type <tt>text/html</tt> and <tt>forcedownload</tt> parameter
	 * is not set.
	 * </p>
	 * 
	 * @return {@code false} if no progress page can be shown
	 */
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

	/**
	 * Creates {@link FProxyFetchWaiter} for current path.
	 * <p>
	 * If no progress already exist, a new on is created. Otherwise a waiter is
	 * created from the existing progress.
	 * </p>
	 * 
	 * @return waiter containing result
	 * @throws MalformedURLException
	 *             if current path is not a valid {@link FreenetURI}
	 * @throws FetchException
	 *             if fetch progress cannot be started
	 * @see #createFetchContext()
	 * @see FProxyFetchTracker#makeFetcher(FreenetURI, long, FetchContext,
	 *      freenet.clients.http.FProxyFetchInProgress.REFILTER_POLICY)
	 */
	private FProxyFetchWaiter getWaiter() throws MalformedURLException, FetchException {
		return trackerManager().getWaiterFor(path, createFetchContext());
	}

	/**
	 * @return progress for current path
	 * @see #createFetchContext()
	 * @see FProxyFetchTracker#getFetchInProgress(FreenetURI, long,
	 *      FetchContext)
	 */
	private FProxyFetchInProgress getProgress() {
		return trackerManager().getProgressFor(path, createFetchContext());
	}

	/**
	 * Follows new {@link FreenetURI}s for a given URI.
	 * <p>
	 * If fetched data is an image or a CSS file, sending a simple redirect is
	 * not enough. Instead we first follow the new URIs and then try to render
	 * the latest version.
	 * </p>
	 * 
	 * @param waiter
	 *            containing initial result
	 * @param cntx
	 *            context for potential deeper fetches
	 * @return {@link FProxyFetchWaiter} containing up-to-date result
	 *         (<b>NOTE</b>: this can also be {@code null})
	 * @throws MalformedURLException
	 *             if any of new URIs are malformed (no {@link FreenetURI})
	 * @throws FetchException
	 *             if fetching progress for new URIs cannot be started
	 * @see #MAX_RECURSION
	 */
	private FProxyFetchWaiter enterRecursionIfNeeded(FProxyFetchWaiter waiter, FetchContext cntx) throws MalformedURLException, FetchException {
		logger.trace("Checking if fetch recursion is needed for " + path);
		HttpServletRequest request = getHttpServletRequest();
		String accept = request.getHeader(RequestsUtil.HEADER_ACCEPT);
		short recursion = RequestsUtil.MAX_RECURSION;
		boolean shouldEnter = (accept != null && (accept.startsWith("text/css") || accept.startsWith("image/")));
		if (shouldEnter) {
			while (recursion > 0) {
				FreenetURI currentURI = waiter.getProgress().uri;
				logger.trace("Recursion number " + (RequestsUtil.MAX_RECURSION - recursion) + " for " + currentURI);
				FProxyFetchResult result = waiter.getResult(true);
				FetchException fe = result.failed;
				if (fe != null && fe.newURI != null) {
					waiter.close();
					final String newPath = fe.newURI.toString();
					logger.debug("New URI found " + newPath + " for " + currentURI);
					waiter = trackerManager().getWaiterFor(newPath, cntx);
					FProxyFetchResult fetchResult = waiter.getResult(true);
					if (fetchResult.isFinished() && fetchResult.failed != null) {
						break;
					}
				} else if (result.isFinished() && fe == null) {
					// No exceptions after finish = no newer versions
					break;
				}
				recursion--;
			}
		} else {
			logger.debug("No recursion needed for" + waiter.getProgress().uri);
		}
		return waiter;
	}

	/**
	 * Redirects the page to the newer version of {@link USK} URI.
	 * <p>
	 * This method is called in case the result if fetched from
	 * {@link FProxyFetchTracker}, since it can be cached.
	 * </p>
	 * 
	 * @param waiter
	 *            containing the {@link FProxyFetchResult}
	 * @see #restartPage(String)
	 */
	private void restartIfOutdated(FProxyFetchWaiter waiter) {
		USKManager uskManager = getFreenetNode().clientCore.uskManager;
		FProxyFetchResult result = waiter.getResult();
		FreenetURI uri = waiter.getProgress().uri;
		if (result.hasData()) {
			try {
				if (uri.isUSK() && !result.hasWaited() && result.getFetchCount() > 1 && uskManager.lookupKnownGood(USK.create(uri)) > uri.getSuggestedEdition()) {
					// A newer version is found
					// Clean cache
					waiter.getProgress().requestImmediateCancel();
					waiter.close();
					// Start fetching again
					logger.debug("A newer version of USK key is available. Restarting request.");
					restartPage();
				}
			} catch (MalformedURLException e) {
				// Cannot happen
				logger.error("Error while checking if key is up-to-date", e);
			}
		}
	}

	/**
	 * Restarts the page
	 */
	private void restartPage() {
		restartPage(null);
	}

	/**
	 * Redirects the {@link Response} to the given path
	 * 
	 * @param newPath
	 *            path to redirect to
	 */
	private void restartPage(String newPath) {
		PageParameters params;
		if (newPath != null) {
			params = new PageParameters();
			params.set(0, newPath);
		} else {
			params = FreenetURIPage.this.getPageParameters();
		}
		setResponsePage(FreenetURIPage.class, params);
	}

	@Override
	public boolean isVersioned() {
		// Provide no versioning for this page
		return false;
	}

}
