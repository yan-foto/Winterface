package freenet.winterface.web.markup;

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;

import freenet.clients.http.bookmark.Bookmark;
import freenet.clients.http.bookmark.BookmarkCategory;
import freenet.clients.http.bookmark.BookmarkManager;
import freenet.winterface.web.core.BookmarkItemModel;

/**
 * A simple {@link Panel} to show/edit {@link Bookmark}s.
 * 
 * @author pausb
 * @see BookmarkManager
 */
@SuppressWarnings("serial")
public class BookmarksPanel extends DashboardPanel {

	LoadableDetachableModel<List<BookmarkCategory>> bmModel;

	public BookmarksPanel(String id) {
		super(id);
		// Make it detachable so its not serialized
		bmModel = new LoadableDetachableModel<List<BookmarkCategory>>() {

			@Override
			protected List<BookmarkCategory> load() {
				return BookmarkManager.MAIN_CATEGORY.getAllSubCategories();
			}

		};
		
		// Make use of chaining models
		PropertyListView<BookmarkCategory> cats = new PropertyListView<BookmarkCategory>("category",bmModel) {

			@Override
			protected void populateItem(ListItem<BookmarkCategory> item) {
				item.add(new Label("name"));
				item.add(new BookmarkItemModel("items"));
			}
			
		};

		add(cats);
	}

	@Override
	public String getName() {
		return "Bookmarks";
	}
}
