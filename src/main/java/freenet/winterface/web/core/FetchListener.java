package freenet.winterface.web.core;

import org.apache.log4j.Logger;
//import org.apache.wicket.atmosphere.EventBus;

import freenet.clients.http.FProxyFetchInProgress;
import freenet.clients.http.FProxyFetchListener;
import freenet.clients.http.FProxyFetchResult;
import freenet.clients.http.FProxyFetchWaiter;
import freenet.keys.FreenetURI;

/**
 * Listens for progress updates in process of fetching a {@link FreenetURI} and
 * posts the updates to {@link EventBus}
 * <p>
 * <strong>THIS FUNCTION IS NOT USED AT THE TIME. IT MAY BE UTILIZIED ONCE AGAIN
 * IF PUSHING PATTERN IS IMPLEMENTED</strong>
 * </p>
 * 
 * @author pausb
 * @see FetchTrackerManager
 */
public class FetchListener implements FProxyFetchListener {

	public final FreenetURI uri;
	/** Progress to monitor */
	final FProxyFetchInProgress progress;
	/** Manager responsible for the fetching */
	final FetchTrackerManager manager;
	
	private FProxyFetchResult latestResult;

	/** Log4j logger */
	private final static Logger logger = Logger.getLogger(FetchListener.class);

	/**
	 * Constructs
	 * 
	 * @param manager
	 *            responsible for the fetch process
	 * @param progress
	 *            to listen to its events
	 */
	public FetchListener(FetchTrackerManager manager, FProxyFetchInProgress progress) {
		this.uri = progress.uri;
		this.progress = progress;
		this.manager = manager;
		this.progress.addListener(this);
		updateLatestResult();
		logger.debug("Fetch listener registered for URI: " + progress.uri);
	}
	
	private void updateLatestResult() {
		FProxyFetchWaiter waiter = progress.getWaiter();
		if(latestResult!=null) {
			latestResult.close();
		}
		latestResult = waiter.getResultFast();
		waiter.close();
	}
	
	public FProxyFetchResult getLatest() {
		return latestResult;
	}

	@Override
	public void onEvent() {
		logger.debug("Received fetch event for URI: " + progress.uri);
		if (progress.finished()) {
			logger.debug("Fetching completed: " + progress.uri);
		}
		updateLatestResult();
	}

}
