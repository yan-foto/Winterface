package freenet.winterface.web.markup;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import freenet.clients.http.bookmark.BookmarkCategory;
import freenet.winterface.web.core.BookmarkItemView;

/**
 * A {@link Panel} which recursively renders bookmark categories.
 * 
 * @author pausb
 * @see BookmarkItemView
 */
@SuppressWarnings("serial")
public class BookmarkCategoryPanel extends Panel {

	/** Path from root up to this category */
	private final String parentBookmarkPath;

	/**
	 * Constructs
	 * 
	 * @param id
	 *            id of HTML tag to replace this panel with
	 * @param model
	 *            data model of this panel
	 */
	public BookmarkCategoryPanel(String id, IModel<BookmarkCategory> model, String parentPath) {
		super(id, model);
		this.parentBookmarkPath = parentPath;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		// Feedback row
		WebMarkupContainer feedback = new WebMarkupContainer("feedback");
		feedback.setOutputMarkupId(true);
		add(feedback);
		// Add category name and BookmarkItem(s)
		add(new Label("visibleName"));
		add(new BookmarkItemView("items", getItemPath()));
		BookmarkCategory category = (BookmarkCategory) getDefaultModelObject();
		// Check for sub categories
		Component subCats = null;
		if (category.getAllSubCategories().size() == 0) {
			subCats = new WebMarkupContainer("allSubCategories");
			subCats.setVisible(false);
		} else {
			subCats = new PropertyListView<BookmarkCategory>("allSubCategories") {
				@Override
				protected void populateItem(ListItem<BookmarkCategory> item) {
					item.add(new BookmarkCategoryPanel("content", item.getModel(), getItemPath()));
				}
			}.setReuseItems(true);

		}
		add(subCats);
	}

	public String getItemPath() {
		String categoryName = ((BookmarkCategory)getDefaultModelObject()).getName();
		return parentBookmarkPath + categoryName;
	}

}
