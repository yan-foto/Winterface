package freenet.winterface.web.core;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;

import com.db4o.ObjectContainer;

import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.HighLevelSimpleClient;
import freenet.client.HighLevelSimpleClientImpl;
import freenet.clients.http.FProxyFetchInProgress;
import freenet.clients.http.FProxyFetchInProgress.REFILTER_POLICY;
import freenet.clients.http.FProxyFetchTracker;
import freenet.clients.http.FProxyFetchWaiter;
import freenet.keys.FreenetURI;
import freenet.node.NodeClientCore;
import freenet.node.RequestClient;
import freenet.node.RequestStarter;
import freenet.winterface.core.FreenetWrapper;

/**
 * Manages all {@link FreenetURI} fetches.
 * 
 * @author pausb
 * @see FProxyFetchTracker
 */
public class FetchTrackerManager implements RequestClient {

	/** Used to get {@link FetchContext} from */
	private HighLevelSimpleClientImpl client;
	/** Actual FProxy fetch tracker */
	private FProxyFetchTracker tracker;
	
	/** Default filtering policy*/
	private final static REFILTER_POLICY DEFAULT_FILTER_POLICY = FProxyFetchInProgress.REFILTER_POLICY.RE_FILTER;
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
	}

	public FProxyFetchWaiter getWaiterFor(String path,FetchContext fctx) throws MalformedURLException, FetchException {
		FreenetURI uri = new FreenetURI(path);
		return tracker.makeFetcher(uri, fctx.maxOutputLength, fctx,DEFAULT_FILTER_POLICY);
	}
	
	public FProxyFetchInProgress getProgressFor(String path, FetchContext fctx) {
		try {
		FreenetURI uri = new FreenetURI(path);
		return tracker.getFetchInProgress(uri, fctx.maxOutputLength, fctx);
		} catch (MalformedURLException e) {
			logger.debug("Progress required for a malformed URI", e);
		}
		return null;
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
