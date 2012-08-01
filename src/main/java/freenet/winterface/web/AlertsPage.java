package freenet.winterface.web;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import freenet.node.useralerts.UserAlert;
import freenet.winterface.core.AlertsUtil;
import freenet.winterface.web.core.AjaxFallbackCssButton;
import freenet.winterface.web.core.AjaxFallbackCssButton.ButtonIcon;

/**
 * {@link WinterPage} showing all valid {@link UserAlert}(s).
 * 
 * @author pausb
 * 
 */
// FIXME Some alerts are not directly deleted in AJAX mode (LDM returns a not
// empty list)
@SuppressWarnings("serial")
public class AlertsPage extends WinterPage {

	/** Current priority class of alerts to show */
	private Integer priorityClass;
	/** {@link PageParameters} name determining priority class to filter */
	private final String PRIORITY_PARAM = "priority";
	/** L10N key */
	private final String NO_MESSAGE_KEY = "UserAlertsToadlet.noMessages";

	/**
	 * {@link LoadableDetachableModel} for alert list. Always up to date
	 * according to {@link #priorityClass}
	 */
	private final LoadableDetachableModel<List<UserAlert>> alertsModel = new LoadableDetachableModel<List<UserAlert>>() {

		@Override
		protected List<UserAlert> load() {
			Integer priorityClass = AlertsPage.this.priorityClass;
			List<UserAlert> result = AlertsUtil.getFilteredAlerts(priorityClass);
			return result;
		}

	};

	/**
	 * Constructs
	 * 
	 * @param params
	 *            parameters
	 * @see #PRIORITY_PARAM
	 */
	public AlertsPage(PageParameters params) {
		super(params);
		priorityClass = params.get(PRIORITY_PARAM).toOptionalInteger();
		if (priorityClass == null) {
			// Get all messages starting from warning
			priorityClass = new Integer(UserAlert.WARNING);
		}
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		// No messages label
		String noMessageText = getApplication().getResourceSettings().getLocalizer().getString(NO_MESSAGE_KEY, this);
		final Label noMessage = new Label("info", Model.of(noMessageText)) {
			@Override
			public boolean isVisible() {
				return alertsModel.getObject().isEmpty();
			}
		};
		noMessage.setOutputMarkupPlaceholderTag(true);
		add(noMessage);
		// Top container needed for refreshing list by ajax
		final WebMarkupContainer alertsOverview = new WebMarkupContainer("alertsOverview");
		alertsOverview.setOutputMarkupId(true);
		PropertyListView<UserAlert> alertsList = new PropertyListView<UserAlert>("alerts", alertsModel) {

			@Override
			protected void populateItem(final ListItem<UserAlert> item) {
				item.setOutputMarkupId(true);
				Label title = new Label("Title");
				Label text = new Label("Text");
				IModel<String> dismissText = Model.of(item.getModelObject().dismissButtonText());
				AjaxFallbackCssButton dismiss = new AjaxFallbackCssButton("dismiss", dismissText, ButtonIcon.DELETE) {
					@Override
					public void onClick(AjaxRequestTarget target) {
						UserAlert ua = item.getModelObject();
						if (ua.userCanDismiss()) {
							item.remove();
							AlertsUtil.dismissAlert(ua.hashCode());
						}
						// Refresh if call is ajax
						if (target != null) {
							target.appendJavaScript("jQuery('#" + item.getMarkupId() + "').slideUp();");
						}
					}
				};
				dismiss.setIconVisible(true);
				dismiss.setVisible(item.getModelObject().userCanDismiss());
				item.add(title, text, dismiss);
			}
		};
		alertsOverview.add(alertsList);
		// Change priority form
		Form<Void> priorityForm = new Form<Void>("priorityForm");
		final RadioGroup<Integer> priority = new RadioGroup<Integer>("priorityGroup", Model.of(priorityClass));
		Radio<Integer> minor = new Radio<Integer>("minor", Model.of(new Integer(UserAlert.MINOR)));
		Radio<Integer> warning = new Radio<Integer>("warning", Model.of(new Integer(UserAlert.WARNING)));
		Radio<Integer> error = new Radio<Integer>("error", Model.of(new Integer(UserAlert.ERROR)));
		Radio<Integer> critical = new Radio<Integer>("critical", Model.of(new Integer(UserAlert.CRITICAL_ERROR)));
		priority.add(minor, warning, error, critical);
		AjaxFallbackButton submit = new AjaxFallbackButton("submit", priorityForm) {
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				priorityClass = priority.getModelObject();
				if (target != null) {
					noMessage.modelChanged();
					target.add(noMessage, alertsOverview);
				} else {
					setResponsePage(AlertsPage.class, new PageParameters().add(PRIORITY_PARAM, priorityClass));
				}
			}
		};
		priorityForm.add(priority, submit);

		add(alertsOverview, priorityForm);
	}

}
