package freenet.winterface.web.core;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wicket.Localizer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.google.common.collect.Maps;

import freenet.client.async.DatabaseDisabledException;
import freenet.node.fcp.RequestStatus;
import freenet.winterface.core.QueueUtil;
import freenet.winterface.web.QueuePage;

/**
 * A Util class to generate mostly used {@link IModel}s for {@link QueuePage}
 * 
 * @author pausb
 * @see QueueUtil
 */
@SuppressWarnings("serial")
public final class QueueModelsUtil {

	// L10N
	private final static String L10N_KEY_PREFIX = "QueuePage.";

	/** Log4j Logger */
	private static final Logger logger = Logger.getLogger(QueueModelsUtil.class);

	/**
	 * Cannot be instantiated
	 */
	private QueueModelsUtil() {
		// not instantiable
	}

	/**
	 * @param targetClass
	 *            target class to initialize {@link QueueUtil}
	 * @return a {@link LoadableDetachableModel} used to access
	 *         {@link QueueUtil}
	 */
	public static LoadableDetachableModel<QueueUtil> ofQueueUtil(final int targetClass) {
		return new LoadableDetachableModel<QueueUtil>() {
			@Override
			protected QueueUtil load() {
				try {
					logger.debug("Accessing QueueUtil");
					QueueUtil result = new QueueUtil(targetClass);
					logger.debug("Queue has " + result.getQueueSize() + " items");
					return result;
				} catch (DatabaseDisabledException e) {
					// TODO forward to error page
					logger.error("Database seems to be disabled", e);
				}
				return null;
			}
		};
	}

	/**
	 * @param parent
	 *            {@link IModel} to access parent {@link QueueUtil}
	 * @param targetClass
	 *            class of queue to access
	 * @return a {@link LoadableDetachableModel} to access queue with given
	 *         target class
	 */
	public static LoadableDetachableModel<List<RequestStatus>> ofQueue(final IModel<QueueUtil> parent, final int targetClass) {
		return new LoadableDetachableModel<List<RequestStatus>>() {
			@Override
			protected List<RequestStatus> load() {
				QueueUtil queueUtil = parent.getObject();
				return queueUtil.get(targetClass);
			}

			@Override
			protected void onDetach() {
				super.onDetach();
				parent.detach();
			}
		};
	}

	/**
	 * @param parent
	 *            {@link IModel} to access parent {@link QueueUtil}
	 * @param targetClass
	 *            class of desired queue
	 * @return localized string of queue size
	 */
	public static IModel<String> ofQueueLocalizedSize(IModel<QueueUtil> parent, int targetClass) {
		Map<String, Integer> substitution = Maps.newHashMap();
		substitution.put("size", parent.getObject().getList(targetClass).size());
		String result = Localizer.get().getString(L10N_KEY_PREFIX + QueueUtil.codeNameMap.get(targetClass), null, Model.ofMap(substitution));
		return Model.of(result);
	}

	/**
	 * @param parent
	 *            {@link IModel} to access parent {@link QueueUtil}
	 * @return an {@link Iterator} containing {@link IModel} of all available
	 *         queues in paret {@link QueueUtil}
	 */
	public static Iterator<IModel<Integer>> ofAllQueues(final IModel<QueueUtil> parent) {
		return new Iterator<IModel<Integer>>() {
			private QueueUtil queueUtil = parent.getObject();
			private Iterator<Integer> delegate = queueUtil.requests.keySet().iterator();

			@Override
			public boolean hasNext() {
				return delegate.hasNext();
			}

			@Override
			public IModel<Integer> next() {
				return Model.of(delegate.next());
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * @param parent
	 *            {@link IModel} to access parent {@link QueueUtil}
	 * @return a {@link LoadableDetachableModel} to access total size of items
	 *         in parent {@link QueueUtil}
	 */
	public static LoadableDetachableModel<Integer> ofAllQueuesSize(final IModel<QueueUtil> parent) {
		return new LoadableDetachableModel<Integer>() {
			@Override
			protected Integer load() {
				return parent.getObject().getQueueSize();
			}

			@Override
			protected void onDetach() {
				super.onDetach();
				parent.detach();
			}
		};
	}
}
