package freenet.winterface.web;

import java.io.IOException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import freenet.clients.http.ConnectionsToadlet.PeerAdditionReturnCodes;
import freenet.node.DarknetPeerNode.FRIEND_TRUST;
import freenet.node.DarknetPeerNode.FRIEND_VISIBILITY;
import freenet.winterface.core.FreenetWrapper;
import freenet.winterface.core.PeerUtils;
import freenet.winterface.web.core.WinterfaceApplication;

@SuppressWarnings("serial")
public class AddFriendPage extends WinterPage {
	
	private final static String L10N_STATUS_PREFIX = "DarknetConnectionsToadlet.peerAdditionCode.";

	public AddFriendPage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		// Form body
		Form<Void> form = new Form<Void>("addFriendForm");
		// Feedback panel
		FeedbackPanel feedback = new FeedbackPanel("feedback");
		form.add(feedback);
		// Node reference
		final TextArea<String> directRef = new TextArea<String>("noderefInsert", Model.of(""));
		final TextField<String> urlRef = new TextField<String>("noderefURL", Model.of(""));
		final FileUploadField browseRef = new FileUploadField("noderefBrowse");
		form.add(directRef, urlRef, browseRef);
		// Peer trust
		final RadioGroup<FRIEND_TRUST> trust = new RadioGroup<FRIEND_TRUST>("trustGroup", Model.of(FRIEND_TRUST.LOW));
		trust.add(new Radio<FRIEND_TRUST>("trustHigh", Model.of(FRIEND_TRUST.HIGH)));
		trust.add(new Radio<FRIEND_TRUST>("trustNormal", Model.of(FRIEND_TRUST.NORMAL)));
		trust.add(new Radio<FRIEND_TRUST>("trustLow", Model.of(FRIEND_TRUST.LOW)));
		trust.setRequired(true);
		form.add(trust);
		// Peer Visibility
		final RadioGroup<FRIEND_VISIBILITY> visibility = new RadioGroup<FRIEND_VISIBILITY>("visibilityGroup", Model.of(FRIEND_VISIBILITY.YES));
		visibility.add(new Radio<FRIEND_VISIBILITY>("visible", Model.of(FRIEND_VISIBILITY.YES)));
		visibility.add(new Radio<FRIEND_VISIBILITY>("onlyName", Model.of(FRIEND_VISIBILITY.NAME_ONLY)));
		visibility.add(new Radio<FRIEND_VISIBILITY>("invisible", Model.of(FRIEND_VISIBILITY.NO)));
		form.add(visibility);
		visibility.setRequired(true);
		// Description
		final TextField<String> desc = new TextField<String>("desc", Model.of(""));
		form.add(desc);
		// Submit Button
		AjaxFallbackButton submit = new AjaxFallbackButton("submit", form) {
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				String refs = null;
				FRIEND_TRUST formTrust = trust.getModelObject();
				FRIEND_VISIBILITY formVisibility = visibility.getModelObject();
				String formDirectRef = directRef.getModelObject();
				String formURLRef = urlRef.getModelObject();
				FileUpload formFileRef = browseRef.getFileUpload();
				String formDesc = desc.getModelObject();
				if (formDirectRef != null) {
					refs = PeerUtils.buildRefsFromString(formDirectRef);
				} else if (formURLRef != null) {
					try {
						refs = PeerUtils.buildRefsFromUrl(formURLRef);
					} catch (IOException e) {
						error("Node ref cannot be read from given URL path");
					}
				} else if (formFileRef != null) {
					refs = PeerUtils.buildRefsFromFile(formFileRef);
				} else {
					error("No node ref where given neither directly nor as URL or file!");
				}
				FreenetWrapper freenetWrapper = ((WinterfaceApplication) getApplication()).getFreenetWrapper();
				// Split multiple refs (if applicable)
				PeerAdditionReturnCodes returnCode = null;
				if (refs != null) {
					String[] splitRefs = PeerUtils.splitRefs(refs);
					for (String ref : splitRefs) {
						String[] splitRef = PeerUtils.splitRef(ref);
						returnCode = PeerUtils.addNewDarknetNode(freenetWrapper.getNode(), splitRef, formDesc, formTrust, formVisibility);
						// Localize response message
						String returnCodeKey = L10N_STATUS_PREFIX + returnCode.toString();
						getApplication().getResourceSettings().getLocalizer().getString(returnCodeKey, this, "");
						info(returnCode.toString());
					}
				}
				if (target != null) {
					target.add(form);
				}
			}
		};
		form.add(submit);
		add(form);
	}

}
