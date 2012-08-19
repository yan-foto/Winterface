package freenet.winterface.web.markup;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.IValueMap;

import freenet.node.useralerts.UserAlert;
import freenet.winterface.core.AlertsUtil;
import freenet.winterface.web.AlertsPage;
import freenet.winterface.web.core.AjaxFallbackCssButton;
import freenet.winterface.web.core.AjaxFallbackCssButton.ButtonIcon;

/**
 * An expandable/collapsable {@link Panel} which shows <i>all<i>
 * {@link UserAlert}(s). Alerts can be dismissed.
 * 
 * @author pausb
 * @see AlertsUtil
 * @see AlertsPage
 */
// TODO messages counts are not updated! use RefreshingView instead of
// RepeatingView
@SuppressWarnings("serial")
public class AlertsPanel extends Panel {

	/** Interval (in seconds) to refresh the content */
	private final static int REFRESH_INTERVAL = 5;
	/** Separator used between priority links */
	private final static String SEPARATOR = " | ";
	/** Log4j logger */
	private final static Logger logger = Logger.getLogger(AlertsPanel.class);

	/**
	 * Constructs
	 * 
	 * @param id
	 *            of tag to replace {@link Component} at
	 */
	public AlertsPanel(String id) {
		super(id);
	}

	@Override
	protected void onInitialize() {
		// Set Model
		final LoadableDetachableModel<List<UserAlert>> itemsModel = new LoadableDetachableModel<List<UserAlert>>() {
			@Override
			protected List<UserAlert> load() {
				return AlertsUtil.getFilteredAlerts(UserAlert.MINOR);
			}
		};
		super.onInitialize();
		// Container to ease AJAX refresh
		final WebMarkupContainer outerContainer = new WebMarkupContainer("outerContainer");
		outerContainer.setOutputMarkupId(true);
		outerContainer.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(REFRESH_INTERVAL)));
		// No messages label
		final WebMarkupContainer noMessageContainer = new WebMarkupContainer("noMessageContainer") {
			@Override
			public boolean isVisible() {
				return itemsModel.getObject().size() == 0;
			}
		};
		noMessageContainer.setOutputMarkupPlaceholderTag(true);
		outerContainer.add(noMessageContainer);
		// Summary of alerts count
		final RepeatingView repeatingContainer = new RepeatingView("linkContainer");
		int[] count = AlertsUtil.countAlerts(itemsModel.getObject());
		for (int i = 0; i < count.length; i++) {
			String priorityTitle = AlertsUtil.getLocalizedTitle(i);
			WebMarkupContainer linkContainer = new WebMarkupContainer(repeatingContainer.newChildId());
			PageParameters params = new PageParameters();
			params.add(AlertsPage.PRIORITY_PARAM, i);
			BookmarkablePageLink<Void> priorityLink = new BookmarkablePageLink<Void>("priorityLink", AlertsPage.class, params);
			priorityLink.add(new AttributeModifier("class", Model.of("priority-" + i)));
			priorityLink.add(new AttributeModifier("title", Model.of(priorityTitle)));
			priorityLink.setBody(Model.of(count[i]));
			linkContainer.add(priorityLink);
			String separatorContent = (i == count.length - 1) ? "" : SEPARATOR;
			Label separator = new Label("separator", Model.of(separatorContent));
			linkContainer.add(separator);
			repeatingContainer.add(linkContainer);
		}
		outerContainer.add(repeatingContainer);
		// Collapsable container
		final WebMarkupContainer alertsContainer = new WebMarkupContainer("alertsContainer", Model.of(false)) {
			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				// Add a inline CSS to modify visibility
				IValueMap attributes = tag.getAttributes();
				String displayValue = (Boolean) getDefaultModelObject() ? "block" : "none";
				attributes.put("style", "display:" + displayValue + ";");
			}
		};
		alertsContainer.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(5)));
		alertsContainer.setOutputMarkupId(true);
		alertsContainer.setOutputMarkupPlaceholderTag(true);
		PropertyListView<UserAlert> alerts = new PropertyListView<UserAlert>("alert", new CompoundPropertyModel<List<UserAlert>>(itemsModel)) {
			@Override
			protected void populateItem(final ListItem<UserAlert> item) {
				item.setOutputMarkupId(true);
				AttributeModifier priorityClass = new AttributeModifier("class", Model.of("-" + item.getModelObject().getPriorityClass()));
				item.add(priorityClass);
				Label shortText = new Label("shortText");
				item.add(shortText);
				AjaxFallbackCssButton dismiss = new AjaxFallbackCssButton("dismiss", null, ButtonIcon.CROSS) {
					@Override
					public void onClick(AjaxRequestTarget target) {
						final UserAlert modelObject = item.getModelObject();
						logger.debug("Removing alert: " + modelObject.getTitle());
						if (modelObject.userCanDismiss()) {
							AlertsUtil.dismissAlert(modelObject.hashCode());
							if (target != null) {
								target.prependJavaScript("jQuery('#" + item.getMarkupId() + "').slideUp();");
							}
							itemsModel.detach();
							repeatingContainer.detachModels();
						}
					}
				};
				// Hide dismiss button if item is not dismissible
				dismiss.setVisible(item.getModelObject().userCanDismiss());
				item.add(dismiss);
			}
		};
		alertsContainer.add(alerts);
		outerContainer.add(alertsContainer);
		// Link to toggle view of alerts list
		AjaxFallbackCssButton toggleLink = new AjaxFallbackCssButton("toggleLink", null, ButtonIcon.BULLET_ARROW_TOP) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				boolean newVisibility = !(Boolean) alertsContainer.getDefaultModelObject();
				alertsContainer.setDefaultModelObject(newVisibility);
				alertsContainer.modelChanged();
				ButtonIcon newIcon = newVisibility ? ButtonIcon.BULLET_ARROW_BOTTOM : ButtonIcon.BULLET_ARROW_TOP;
				replaceIcon(newIcon);
				if (target != null) {
					target.add(outerContainer);
				}
			}
		};
		outerContainer.add(toggleLink);
		add(outerContainer);
	}
}
