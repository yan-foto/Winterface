package freenet.winterface.web.core;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import freenet.node.fcp.RequestStatus;
import freenet.support.SizeUtil;
import freenet.winterface.core.QueueUtil;
import freenet.winterface.core.RequestProgress;
import freenet.winterface.core.RequestStatusUtil;

/**
 * A {@link ListView} to render a {@link List} of {@link RequestStatus}(es).
 * <p>
 * Each item renders following information of {@link RequestStatus}:
 * <ul>
 * <li>Identifier</li>
 * <li>Priority</li>
 * <li>Size</li>
 * <li>MIME-Type</li>
 * <li>Progress</li>
 * <li>Last activity</li>
 * <li>Persistence mode</li>
 * <li>File name</li>
 * <li>Key (if known)</li>
 * </ul>
 * </p>
 * 
 * @author pausb
 * @see RequestStatusUtil
 */
@SuppressWarnings("serial")
public class RequestStatusView extends ListView<RequestStatus> {

	public RequestStatusView(String id, final IModel<QueueUtil> model, final int targetClass) {
		super(id);
		LoadableDetachableModel<List<RequestStatus>> listModel = QueueModelsUtil.ofQueue(model, targetClass);
		setDefaultModel(listModel);
	}

	@Override
	protected void populateItem(ListItem<RequestStatus> item) {
		RequestStatus req = item.getModelObject();
		final IModel<String> identifierModel = Model.of(req.getIdentifier());
		// checkbox
		Check<String> check = new Check<String>("select", identifierModel);
		item.add(check);
		// Identifier
		item.add(new Label("identifier", identifierModel));
		// Priority
		final String priority = RequestStatusUtil.getPriority(req);
		item.add(new Label("priority", Model.of(priority)));
		item.add(new AttributeModifier("class", Model.of("priority-" + req.getPriority())));
		// Size
		String size = SizeUtil.formatSize(RequestStatusUtil.getSize(req));
		item.add(new Label("size", Model.of(size)));
		// Mime
		String mime = RequestStatusUtil.getMIME(req);
		item.add(new Label("mime", Model.of(mime)));
		// Progress
		RequestProgress progressStatus = RequestStatusUtil.getProgress(req);
		Label progress = new Label("progress");
		if (progressStatus.specialFlag == RequestProgress.FLAG_PROG_COMP) {
			progress.setDefaultModel(Model.of(progressStatus.mainPercent + " %"));
		} else {
			progress.setDefaultModel(Model.of(progressStatus.localizeSpecialFlag()));
		}
		item.add(progress);
		// Last activity
		Label lastActivity = new Label("lastActivity", Model.of(RequestStatusUtil.getLastActivity(req)));
		item.add(lastActivity);
		// Persistence
		Label persistence = new Label("persistence", Model.of(RequestStatusUtil.getPersistence(req)));
		item.add(persistence);
		// File name
		Label fileName = new Label("fileName", Model.of(RequestStatusUtil.getFileName(req)));
		item.add(fileName);
		// Key
		Component keyLink;
		String[] link = RequestStatusUtil.getKeyLink(req);
		if (link[1].startsWith("/")) {
			keyLink = new ExternalLink("keyLink", Model.of(link[1]));
			((ExternalLink) keyLink).setBody(Model.of(link[0]));
		} else {
			keyLink = new Label("keyLink", Model.of(link[0]));
		}
		item.add(keyLink);
	}

}
