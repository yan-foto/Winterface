package freenet.winterface.web.markup;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import freenet.keys.FreenetURI;
import freenet.winterface.web.FreenetURIPage;
import freenet.winterface.web.core.FreenetURIHandler;

/**
 * Simple {@link DashboardPanel} to browse to a given {@link FreenetURI}
 * 
 * @author pausb
 * @see FreenetURIPage
 * @see FreenetURIHandler
 */
// TODO add advanced options
@SuppressWarnings("serial")
public class VisitURIPanel extends DashboardPanel {

	private final static String L10N_TITLE = "WelcomeToadlet.fetchKeyLabel";
	private final static String L10N_INVALID_KEY = "FProxyToadlet.invalidKeyWithReason";

	/**
	 * Constructs
	 * @param id of element to replace {@link Component} with
	 */
	public VisitURIPanel(String id) {
		super(id);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		// Feedback panel
		final FeedbackPanel feedback = new FeedbackPanel("feedback");
		feedback.setOutputMarkupId(true);
		add(feedback);
		// Actual form
		Form<Void> visitForm = new Form<Void>("visitForm");
		final TextField<String> keyToFetch = new TextField<String>("keyToFetch", Model.of(""));
		AjaxFallbackButton fetchSubmit = new AjaxFallbackButton("fetchSubmit", visitForm) {
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				String userInput = keyToFetch.getModelObject();
				FreenetURI uri = null;
				try {
					uri = new FreenetURI(userInput);
				} catch (MalformedURLException e) {
					Map<String, String> substitution = new HashMap<String, String>();
					substitution.put("reason", e.toString());
					form.error(getLocalizer().getString(L10N_INVALID_KEY, this, Model.ofMap(substitution)));
					if (target != null) {
						target.add(feedback);
					}
					return;
				}
				// No errors found, move on
				PageParameters params = new PageParameters();
				params.set(0, uri.toString());
				setResponsePage(FreenetURIPage.class, params);
			}
		};
		visitForm.add(keyToFetch, fetchSubmit);
		add(visitForm);
	};

	@Override
	public String getName() {
		return getLocalizer().getString(L10N_TITLE, this);
	}

}
