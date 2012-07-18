package freenet.winterface.web.core;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import freenet.node.useralerts.UserAlert;
import freenet.winterface.web.core.AjaxFallbackCssButton.buttonIcon;
import freenet.winterface.web.markup.AlertsPanel;

@SuppressWarnings("serial")
public class AlertsView extends PropertyListView<UserAlert> {

	private boolean isSHortView;

	public AlertsView(String id, IModel<List<UserAlert>> model, boolean isShortView) {
		super(id, model);
		this.isSHortView = isShortView;
	}

	@Override
	protected void populateItem(final ListItem<UserAlert> item) {
		// Box Title
		// Model to add corresponding type CSS
		IModel<String> typeCssModel = new Model<String>() {
			@Override
			public String getObject() {
				switch(item.getModelObject().getPriorityClass()) {
				case UserAlert.CRITICAL_ERROR:
					return "critical-error";
				case UserAlert.ERROR:
					return "error";
				case UserAlert.MINOR:
					return "minor";
				case UserAlert.WARNING:
					return "warning";
				}
				return "";
			}
		};
		Label label = new Label("title");
		label.add((new AttributeAppender("class", typeCssModel," ")));
		item.add(label);
		// Alert Content
		Label content = new Label("text");
		item.add(content);
		// If dismissible
		WebMarkupContainer hideContainer = new WebMarkupContainer("dismiss");
		AjaxFallbackCssButton hide = new AjaxFallbackCssButton("hide",Model.of(item.getModelObject().dismissButtonText())) {
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				// hide item
				int hashCode = item.getModelObject().hashCode();
				((WinterfaceApplication)getApplication()).getFreenetWrapper().getNode().clientCore.alerts.dismissAlert(hashCode);
				// Refresh the container
				AlertsPanel panel = findParent(AlertsPanel.class);
				Component container = panel.get("alerts-container");
				if(target!=null){
					target.add(container);
				}
				
			}
		};
		hide.setIcon(buttonIcon.CANCEL);
		hideContainer.add(hide);
		hideContainer.setVisible(item.getModelObject().userCanDismiss());
		item.add(hideContainer);
	}

}
