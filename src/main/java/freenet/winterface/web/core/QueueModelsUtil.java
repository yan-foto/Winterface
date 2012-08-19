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

@SuppressWarnings("serial")
public final class QueueModelsUtil {

	private final static String L10N_KEY_PREFIX = "QueuePage.";

	private static final Logger logger = Logger.getLogger(QueueModelsUtil.class);

	private QueueModelsUtil() {
		// not instantiable
	}

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

	public static LoadableDetachableModel<Integer> ofQueueSize(final IModel<List<RequestStatus>> parent) {
		return new LoadableDetachableModel<Integer>() {
			@Override
			protected Integer load() {
				return parent.getObject().size();
			}

			@Override
			protected void onDetach() {
				super.onDetach();
				parent.detach();
			}
		};
	}

	public static IModel<String> ofQueueLocalizedSize(IModel<QueueUtil> parent, int targetClass) {
		Map<String, Integer> substitution = Maps.newHashMap();
		substitution.put("size", parent.getObject().getList(targetClass).size());
		String result = Localizer.get().getString(L10N_KEY_PREFIX + QueueUtil.codeNameMap.get(targetClass), null, Model.ofMap(substitution));
		return Model.of(result);
	}

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
