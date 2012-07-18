package freenet.winterface.web.markup;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.time.Duration;

import freenet.node.useralerts.UserAlert;
import freenet.node.useralerts.UserAlertManager;
import freenet.winterface.web.core.AlertsView;
import freenet.winterface.web.core.WinterfaceApplication;

@SuppressWarnings("serial")
public class AlertsPanel extends Panel {

	private transient UserAlertManager userManager;
	private boolean isShortView = true;
	private LoadableDetachableModel<List<UserAlert>> itemsModel;

	public AlertsPanel(String id) {
		super(id);
		// get AlertManager
		userManager = ((WinterfaceApplication) getApplication()).getFreenetWrapper().getNode().clientCore.alerts;
		// Set Model
		itemsModel = new LoadableDetachableModel<List<UserAlert>>() {

			@Override
			protected List<UserAlert> load() {
				return Arrays.asList(userManager.getAlerts());
			}
		};
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		WebMarkupContainer container = new WebMarkupContainer("alerts-container");
		container.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(5)));
		container.setOutputMarkupId(true);
		add(container);
		container.add(new AlertsView("alerts", itemsModel, isShortView));

	}

	public void setShortView(boolean shortView) {
		this.isShortView = shortView;
	}

}
