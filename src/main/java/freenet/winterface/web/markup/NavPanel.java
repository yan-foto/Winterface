package freenet.winterface.web.nav;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * {@link Panel} that renders the navigation menu.
 * <p>
 * The navigation menu is built in a hierarchical manner, so that each submenu
 * itself is an instance of {@link NavPanel}
 * </p>
 * 
 * @author pausb
 * @see NavCallbackInterface
 * @see PageNavItem
 */
@SuppressWarnings("serial")
public final class NavPanel extends Panel {

	/**
	 * Denotes the depth level
	 */
	private int level;

	/**
	 * Constructs.
	 * 
	 * @param id
	 *            wicket:id of desired {@link Component}
	 * @param callbackModel
	 *            {@link IModel} used to generate navigation content
	 */
	public NavPanel(String id, IModel<AbstractNavItem> callbackModel) {
		this(id, callbackModel, 0);
	}

	/**
	 * Constructs
	 * 
	 * @param id
	 *            wicket:id of desired {@link Component}
	 * @param callbackModel
	 *            {@link IModel} used to generate navigation content
	 * @param level
	 *            depth level
	 */
	public NavPanel(String id, IModel<AbstractNavItem> callbackModel, int level) {
		super(id);
		this.level = level;
		initPanel(callbackModel);
	}

	/**
	 * Initializes the navigation panel
	 * 
	 * @param callbackModel
	 *            {@link IModel} used to generate content
	 */
	private void initPanel(final IModel<AbstractNavItem> callbackModel) {
		// Use field called "name" to get name of menu
		// TODO support i18n
		final IModel<String> nameModel = new PropertyModel<String>(callbackModel, "Name");

		// Model to add "active" class to menu if currently in respective page
		LoadableDetachableModel<String> classModel = new LoadableDetachableModel<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			protected String load() {
				return callbackModel.getObject().isActive(getPage()) ? "active" : null;
			}

			@Override
			protected void onDetach() {
				super.onDetach();
				callbackModel.detach();
			}
		};

		// Menu link to page
		Link<AbstractNavItem> link = new Link<AbstractNavItem>("nav-link", callbackModel) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick() {
				callbackModel.getObject().onClick(getPage());
			}

			@Override
			protected void onBeforeRender() {
				super.onBeforeRender();
				boolean visible = (nameModel.getObject() == null) ? false : true;
				setVisible(visible);
			}
		};

		// Adding link
		link.add(new Label("nav-name", nameModel));
		link.add(new AttributeAppender("class", classModel));
		link.add(new AttributeAppender("class", Model.of(" level-" + this.level)));
		add(link);

		// Model used to generate children (if any)
		LoadableDetachableModel<List<AbstractNavItem>> childModel = new LoadableDetachableModel<List<AbstractNavItem>>() {

			private static final long serialVersionUID = 1L;

			@Override
			protected List<AbstractNavItem> load() {
				return callbackModel.getObject().getChilds(getPage());
			}
		};

		// Add Children
		ListView<AbstractNavItem> childList = new ListView<AbstractNavItem>("nav-children", childModel) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<AbstractNavItem> item) {
				item.add(new NavPanel("nav-child", item.getModel(), level + 1));
			}

		};

		add(childList);
	}
}
