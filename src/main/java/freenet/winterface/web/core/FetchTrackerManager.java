package freenet.winterface.web.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.atmosphere.EventBus;

import com.db4o.ObjectContainer;

import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.HighLevelSimpleClient;
import freenet.client.HighLevelSimpleClientImpl;
import freenet.clients.http.FProxyFetchInProgress;
import freenet.clients.http.FProxyFetchTracker;
import freenet.clients.http.FProxyFetchWaiter;
import freenet.keys.FreenetURI;
import freenet.node.NodeClientCore;
import freenet.node.RequestClient;
import freenet.node.RequestStarter;
import freenet.winterface.core.FreenetWrapper;

/**
 * Manages all {@link FreenetURI} fetches.
 * <p>
 * It uses the current progress mechanism of FProxy and adapts it to the pushing
 * approach appropriate for Wicket + Atmosphere.
 * </p>
 * 
 * @author pausb
 * @see FProxyFetchTracker
 */
public class FetchTrackerManager implements RequestClient {

	/** {@link EventBus} to push progress updates to */
	public final EventBus eventBus;

	/** Used to get {@link FetchContext} from */
	private HighLevelSimpleClientImpl client;
	/** Actual FProxy fetch tracker */
	private FProxyFetchTracker tracker;
	/** A list of listeners to monitor progress of each fetch */
	private List<FetchListener> listeners;

	/**
	 * Constructs.
	 * 
	 * @param wrapper
	 *            to access Freenet
	 * @param application
	 *            to access {@link WinterfaceApplication}
	 */
	public FetchTrackerManager(FreenetWrapper wrapper, WinterfaceApplication application) {
		NodeClientCore core = wrapper.getNode().clientCore;
		this.client = new HighLevelSimpleClientImpl(core, core.tempBucketFactory, core.random, RequestStarter.INTERACTIVE_PRIORITY_CLASS, true, true);
		this.tracker = new FProxyFetchTracker(core.clientContext, client.getFetchContext(), this);
		this.eventBus = application.getEventBus();
		this.listeners = new ArrayList<FetchListener>();
	}

	/**
	 * Gets the progress of a fetch process.
	 * <p>
	 * Moreover it adds listeners for each fetch process (see
	 * {@link FetchListener}).
	 * </p>
	 * 
	 * @param uri
	 *            key to fetch
	 * @param maxSize
	 *            max size of data to fetch
	 * @param fctx
	 *            fetch context
	 * @return {@link FProxyFetchWaiter} of the fetch process.
	 * @throws FetchException
	 */
	public FProxyFetchWaiter getProgress(FreenetURI uri, long maxSize, FetchContext fctx) throws FetchException {
		// FIXME Maybe add filter policy to the Configuration
		FProxyFetchWaiter waiter = tracker.makeFetcher(uri, maxSize, fctx, FProxyFetchInProgress.REFILTER_POLICY.ACCEPT_OLD);
		FetchListener listener = new FetchListener(this, waiter.progress);
		listeners.add(listener);
		return waiter;
	}

	/**
	 * Returns the actual {@link FProxyFetchTracker}
	 * 
	 * @return FProxy fetch tracker
	 */
	public FProxyFetchTracker getTracker() {
		return tracker;
	}

	/**
	 * Returns {@link HighLevelSimpleClientImpl} which is responsible for
	 * fetching.
	 * 
	 * @return FProxy {@link HighLevelSimpleClient}
	 */
	public HighLevelSimpleClientImpl getClient() {
		return client;
	}

	/**
	 * Removes a {@link FetchListener} from list of listeners
	 * 
	 * @param listener
	 *            to remove
	 */
	void removeListener(FetchListener listener) {
		listeners.remove(listener);
	}

	@Override
	public boolean persistent() {
		return false;
	}

	@Override
	public boolean realTimeFlag() {
		return true;
	}

	@Override
	public void removeFrom(ObjectContainer container) {
		// Do nothing
	}

}
