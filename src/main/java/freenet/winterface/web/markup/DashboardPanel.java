package freenet.winterface.web.markup;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

/**
 * Super class for all {@link Panel}s which are to be shown as dashboard panels
 * on the main page.
 * <p>
 * All subclasses will inherit markup of this Panel and will look consistent.
 * </p>
 * 
 * @author pausb
 */
@SuppressWarnings("serial")
public abstract class DashboardPanel extends Panel {

	/**
	 * Constructs
	 * 
	 * @param id
	 *            {@link Component} markup ID
	 */
	public DashboardPanel(String id) {
		super(id);
		add(new Label("name", new PropertyModel<String>(this, "name")));
	}

	/**
	 * Returns {@link Panel} name
	 * 
	 * @return panel title
	 */
	public abstract String getName();

}
