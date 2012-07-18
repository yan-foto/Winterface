package freenet.winterface.web.core;

import org.apache.log4j.Logger;
import org.apache.wicket.atmosphere.EventBus;

import freenet.clients.http.FProxyFetchInProgress;
import freenet.clients.http.FProxyFetchListener;
import freenet.keys.FreenetURI;

/**
 * Listens for progress updates in process of fetching a {@link FreenetURI} and
 * posts the updates to {@link EventBus}
 * 
 * @author pausb
 * @see FetchTrackerManager
 */
public class FetchListener implements FProxyFetchListener {

	/** Progress to monitor */
	private final FProxyFetchInProgress progress;
	/** Manager responsible for the fetching */
	private final FetchTrackerManager manager;

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
		this.progress = progress;
		this.manager = manager;
		this.progress.addListener(this);
		logger.debug("Fetch listener registered for URI: " + progress.uri);
	}

	@Override
	public void onEvent() {
		logger.debug("Received fetch event for URI: " + progress.uri);
		manager.eventBus.post(progress);
		if (progress.finished()) {
			logger.debug("Fetching completed: " + progress.uri);
			manager.removeListener(this);
		}
	}

}
