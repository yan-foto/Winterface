package freenet.winterface.web.core;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.db4o.ObjectContainer;

import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.HighLevelSimpleClient;
import freenet.client.HighLevelSimpleClientImpl;
import freenet.clients.http.FProxyFetchInProgress;
import freenet.clients.http.FProxyFetchResult;
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

	/** Used to get {@link FetchContext} from */
	private HighLevelSimpleClientImpl client;
	/** Actual FProxy fetch tracker */
	private FProxyFetchTracker tracker;
	/** Contains running {@link FProxyFetchInProgress}s */
	private Map<String, FreenetURI> keysInProgress;
	/** A list of {@link FetchListener}s */
	private Set<FetchListener> listeners;

	/** Log4j logger */
	private final static Logger logger = Logger.getLogger(FetchTrackerManager.class);

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
		listeners = new HashSet<FetchListener>();
		keysInProgress = new HashMap<String, FreenetURI>();
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
	 * @throws MalformedURLException
	 */
	public void initProgress(String path, long maxSize, FetchContext fctx) throws FetchException, MalformedURLException {
		FreenetURI uri = keysInProgress.get(path);
		if (uri == null) {
			uri = new FreenetURI(path);
			keysInProgress.put(path, uri);
		}
		// FIXME Maybe add filter policy to the Configuration
		FProxyFetchWaiter waiter = tracker.makeFetcher(uri, maxSize, fctx, FProxyFetchInProgress.REFILTER_POLICY.RE_FILTER);
		FetchListener fetchListener = listenerFor(uri, waiter.getProgress(), fctx);
		if (fetchListener == null) {
			logger.debug(String.format("No existing listeners found for URI %s. Registering new one.", uri));
			fetchListener = new FetchListener(this, waiter.getProgress());
			addListener(fetchListener);
		}
	}

	/**
	 * Returns {@link FProxyFetchResult} for given {@link FreenetURI} (in
	 * {@link String} format).
	 * <p>
	 * This will return {@code null} if
	 * {@link #initProgress(String, long, FetchContext)} is not first called.
	 * <p>
	 * 
	 * @param path
	 *            A {@link FreenetURI} in {@link String} format
	 * @return corresponding {@link FProxyFetchResult}
	 * @throws MalformedURLException
	 */
	public FProxyFetchResult getResult(String path) throws MalformedURLException {
		FreenetURI uri = new FreenetURI(path);
		return listenerFor(uri).getLatest();
	}

	/**
	 * Returns {@link FetchListener} corresponding to given parameters.
	 * 
	 * @param uri
	 *            {@link FreenetURI} to be fetched
	 * @param progress
	 *            parent {@link FProxyFetchInProgress} of {@link FetchListener}
	 * @param fctx
	 *            {@link FetchContext} of {@link FProxyFetchInProgress}
	 * @return {@link FetchListener}
	 */
	public FetchListener listenerFor(FreenetURI uri, FProxyFetchInProgress progress, FetchContext fctx) {
		for (FetchListener listener : listeners) {
			if (listener.progress.fetchContextEquivalent(fctx) && URIisEquivalent(uri, listener.uri)) {
				return listener;
			}
		}
		return null;
	}

	public FetchListener listenerFor(FreenetURI uri) {
		for (FetchListener listener : listeners) {
			if (URIisEquivalent(uri, listener.uri)) {
				return listener;
			}
		}
		return null;
	}

	/**
	 * Does a {@link String#equals(Object)} on values of given
	 * {@link FreenetURI}s
	 * 
	 * @param one
	 *            {@link FreenetURI}
	 * @param other
	 *            {@link FreenetURI}
	 * @return {@code true} if String value of given {@link FreenetURI}s is
	 *         equal
	 */
	public boolean URIisEquivalent(FreenetURI one, FreenetURI other) {
		String oneString = one.toString();
		String otherString = other.toString();
		return oneString.equals(otherString);
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
	 * Adds {@link FetchListener} to list of listeners
	 * 
	 * @param listener
	 *            {@link FetchListener} to add
	 */
	void addListener(FetchListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes {@link FetchListener} from list of listeners
	 * 
	 * @param listener
	 *            {@link FetchListener} to remove
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
