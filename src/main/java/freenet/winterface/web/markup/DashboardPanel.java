package freenet.winterface.web.markup;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
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
	}

	/**
	 * Constructs.
	 * <p>
	 * {@link IModel} parameter can be an {@link IModel} (e.g. a
	 * {@link CompoundPropertyModel}) which provides data for {@link Component}s
	 * inside the panel.
	 * </p>
	 * 
	 * @param id
	 *            {@link Component} markup ID
	 * @param model
	 *            {@link IModel} to retrieve data from
	 */
	public DashboardPanel(String id, IModel<?> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new Label("name", new PropertyModel<String>(this, "name")));
	}

	/**
	 * Returns {@link Panel} name
	 * 
	 * @return panel title
	 */
	public abstract String getName();

}
