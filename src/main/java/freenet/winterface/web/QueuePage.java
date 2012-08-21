package freenet.winterface.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import freenet.client.async.DatabaseDisabledException;
import freenet.node.fcp.FCPServer;
import freenet.node.fcp.MessageInvalidException;
import freenet.node.fcp.RequestStatus;
import freenet.winterface.core.QueueHelper;
import freenet.winterface.web.core.QueueModelsUtil;
import freenet.winterface.web.core.RequestStatusView;

/**
 * Displays a list of all global {@link RequestStatus}
 * <p>
 * Lists are divided in different categories (see {@link QueueHelper#codeNameMap})
 * and user can change priority and delete items from lists.
 * </p>
 * 
 * @author pausb
 * @see FCPServer
 * @see QueueHelper
 * @see RequestStatusView
 */
// TODO there are missing features such as restart item
// TODO add no items in queue message
@SuppressWarnings("serial")
public class QueuePage extends WinterPage {

	/** An integer representing desired queues to show */
	private final int targetClass;

	/**
	 * Parameter containing target class. It can contain both string and
	 * integers (see {@link #extractTargetClass()})
	 */
	private final static String QUEUE_PARAM = "queue";

	// L10N
	private final static String L10N_PRIO_PREFIX = "QueueToadlet.priority";
	private final static String L10N_REMOVE_FAILED = "QueueToadlet.failedToRemoveRequest";

	/** Log4j logger */
	private final static Logger logger = Logger.getLogger(QueuePage.class);

	/**
	 * Constructs.
	 * 
	 * @param params
	 *            page parameters
	 * @see #extractTargetClass()
	 */
	public QueuePage(PageParameters params) {
		super(params);
		targetClass = extractTargetClass();
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		// Shared models
		final LoadableDetachableModel<QueueHelper> utilModel = QueueModelsUtil.ofQueueUtil(targetClass);
		final LoadableDetachableModel<Integer> sizeModel = QueueModelsUtil.ofAllQueuesSize(utilModel);

		// Navigation and tools
		final WebMarkupContainer navLinksContainer = new WebMarkupContainer("navLinksContainer", sizeModel) {
			@Override
			public boolean isVisible() {
				boolean isVisible = ((Integer) getDefaultModelObject()) != 0;
				return isVisible;
			}
		};
		navLinksContainer.setOutputMarkupId(true);

		// Links to each queue list
		RefreshingView<Integer> linksRepeater = new RefreshingView<Integer>("linksRepeater") {
			@Override
			protected Iterator<IModel<Integer>> getItemModels() {
				return QueueModelsUtil.ofAllQueues(utilModel);
			}

			@Override
			protected void populateItem(Item<Integer> item) {
				Integer targetClass = item.getModelObject();
				ExternalLink navLink = new ExternalLink("navLink", "#" + QueueHelper.codeNameMap.get(targetClass));
				navLink.setBody(QueueModelsUtil.ofQueueLocalizedSize(utilModel, targetClass));
				item.add(navLink);
			}
		};
		navLinksContainer.add(linksRepeater);

		// All queues
		final WebMarkupContainer queuesContainer = new WebMarkupContainer("queuesContainer", sizeModel) {
			@Override
			public boolean isVisible() {
				boolean isVisible = ((Integer) getDefaultModelObject()) != 0;
				return isVisible;
			}
		};
		queuesContainer.setOutputMarkupId(true);

		// From to get selected check boxes
		Form<Void> editForm = new Form<Void>("editForm");

		// Feedback panel
		FeedbackPanel feedback = new FeedbackPanel("feedback");
		editForm.add(feedback);

		// Group to collect identifier of all selected items
		final CheckGroup<String> selectedGroup = new CheckGroup<String>("selectedGroup", new ArrayList<String>());
		editForm.add(selectedGroup);
		// Remove button
		AjaxFallbackButton removeItems = new AjaxFallbackButton("removeItems", editForm) {
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				Collection<String> modelObject = selectedGroup.getModelObject();
				FCPServer fcp = utilModel.getObject().getFCPServer();
				for (String id : modelObject) {
					try {
						fcp.removeGlobalRequestBlocking(id);
						logger.debug("Successfully removed item with identifier: " + id);
					} catch (MessageInvalidException e) {
						String message = localize(L10N_REMOVE_FAILED);
						message += "(" + id + ")";
						form.error(message);
						logger.debug(message);
					} catch (DatabaseDisabledException e) {
						// Cannot happen. QueueUtil has been instantiated
						// already -> Database is active
					}
				}
				// Make sure that model is up-to-date
				utilModel.detach();
				if (target != null) {
					target.add(navLinksContainer, queuesContainer);
				}
			}
		};
		editForm.add(removeItems);
		// Change priority
		List<Short> priorities = Arrays.asList((short) 0, (short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6);
		IChoiceRenderer<Short> priorityRenderer = new IChoiceRenderer<Short>() {
			@Override
			public Object getDisplayValue(Short object) {
				return localize(L10N_PRIO_PREFIX + object);
			}

			@Override
			public String getIdValue(Short object, int index) {
				return String.valueOf(object);
			}
		};
		final DropDownChoice<Short> prioritySelect = new DropDownChoice<Short>("prioritySelect", Model.of((short) 0), priorities, priorityRenderer);
		editForm.add(prioritySelect);

		AjaxFallbackButton changePriority = new AjaxFallbackButton("changePriority", editForm) {
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				Collection<String> modelObject = selectedGroup.getModelObject();
				FCPServer fcp = utilModel.getObject().getFCPServer();
				for (String id : modelObject) {
					try {
						fcp.modifyGlobalRequestBlocking(id, null, prioritySelect.getModelObject());
						logger.debug("Successfully change priority of item with identifier: " + id);
					} catch (DatabaseDisabledException e) {
						// Cannot happen.
					}
				}
				utilModel.detach();
				if (target != null) {
					target.add(navLinksContainer, queuesContainer);
				}
			}
		};
		editForm.add(changePriority);

		queuesContainer.add(editForm);
		// All queues
		RefreshingView<Integer> queuesRepeater = new RefreshingView<Integer>("queuesRepeater") {
			@Override
			protected Iterator<IModel<Integer>> getItemModels() {
				return QueueModelsUtil.ofAllQueues(utilModel);
			}

			@Override
			protected void populateItem(Item<Integer> item) {
				int targetClass = item.getModelObject();
				String itemTitle = QueueHelper.codeNameMap.get(targetClass);
				// Outer container
				WebMarkupContainer queueContainer = new WebMarkupContainer("queueContainer");
				item.add(queueContainer);
				// Title and anchor
				Label title = new Label("queueTitle", QueueModelsUtil.ofQueueLocalizedSize(utilModel, targetClass));
				Label queueAnchor = new Label("queueAnchor");
				queueAnchor.setDefaultModel(Model.of("<a name=\"" + itemTitle + "\"></a>")).setEscapeModelStrings(false);
				queueAnchor.setRenderBodyOnly(true);
				queueContainer.add(title, queueAnchor);
				// Queue itself
				RequestStatusView queue = new RequestStatusView("queue", utilModel, item.getModelObject());
				queue.setReuseItems(true);
				queueContainer.add(queue);
			}
		};
		selectedGroup.add(queuesRepeater);
		add(navLinksContainer, queuesContainer);
	}

	/**
	 * Creates a target class from page parameter
	 * <p>
	 * Page parameter can be a string (downlaods/uploads) or an integer
	 * representing a target class. If none all queues are fetched.
	 * </p>
	 * 
	 * @return generated target class
	 * @see #QUEUE_PARAM
	 * @see QueueHelper
	 */
	private int extractTargetClass() {
		int targetClass = 0;
		StringValue desiredQueue = getPageParameters().get(QUEUE_PARAM);
		String stringParam = desiredQueue.toString();
		if ("downloads".equalsIgnoreCase(stringParam)) {
			targetClass = QueueHelper.DL_ALL;
		} else if ("uploads".equalsIgnoreCase(stringParam)) {
			targetClass = QueueHelper.UP_ALL;
		} else if (stringParam != null) {
			String[] allClasses = stringParam.split(",");
			for (String cl : allClasses) {
				Integer code = QueueHelper.codeNameMap.inverse().get(cl);
				targetClass = (code == null) ? targetClass : (targetClass |= code);
			}
		}
		// None of the above conditions are fulfilled: either parameter is
		// invalid or not available at all
		if (targetClass == 0) {
			targetClass = QueueHelper.DL_ALL | QueueHelper.UP_ALL;
		}
		return targetClass;
	}

}
