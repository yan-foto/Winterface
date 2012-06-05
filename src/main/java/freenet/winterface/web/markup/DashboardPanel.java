package freenet.winterface.web.markup;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;


@SuppressWarnings("serial")
public abstract class DashboardPanel extends Panel {

	public DashboardPanel(String id) {
		super(id);
		add(new Label("name", new PropertyModel<String>(this, "name")));
	}
	
	public abstract String getName();

}
