package freenet.winterface.web.markup;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import freenet.clients.http.bookmark.Bookmark;
import freenet.clients.http.bookmark.BookmarkCategory;
import freenet.clients.http.bookmark.BookmarkManager;

/**
 * A simple {@link Panel} to show/edit {@link Bookmark}s.
 * 
 * @author pausb
 * @see BookmarkManager
 */
@SuppressWarnings("serial")
public class BookmarksPanel extends DashboardPanel {

	/** {@link IModel} containing main bookmarks category */
	final LoadableDetachableModel<List<BookmarkCategory>> bmModel;

	/**
	 * Constructs
	 * 
	 * @param id
	 *            of HTML tag to replace this panel with
	 */
	public BookmarksPanel(String id) {
		super(id);
		// A loadable detachable model, so its content is not serialized
		bmModel = new LoadableDetachableModel<List<BookmarkCategory>>() {
			@Override
			protected List<BookmarkCategory> load() {
				ArrayList<BookmarkCategory> result = new ArrayList<BookmarkCategory>();
				result.add(BookmarkManager.MAIN_CATEGORY);
				return result;
			}
		};
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		// Make use of chaining models
		PropertyListView<BookmarkCategory> cats = new PropertyListView<BookmarkCategory>("categories", bmModel) {
			@Override
			protected void populateItem(ListItem<BookmarkCategory> item) {
				item.add(new BookmarkCategoryPanel("content", item.getModel(), ""));
			}
		};
		cats.setReuseItems(true);

		// A container to mix listView with Ajax
		WebMarkupContainer container = new WebMarkupContainer("bookmarks-container");
		container.setOutputMarkupId(true);

		container.add(cats);
		add(container);
	}

	@Override
	public String getName() {
		return "Bookmarks";
	}
}
