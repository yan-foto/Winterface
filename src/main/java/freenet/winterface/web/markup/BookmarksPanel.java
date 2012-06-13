package freenet.winterface.web.markup;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
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

	/**
	 * Constructs
	 * @param id of HTML tag to replace this panel with
	 */
	public BookmarksPanel(String id) {
		super(id);
		
		// Make it detachable so its not serialized
		LoadableDetachableModel<List<BookmarkCategory>> bmModel = new LoadableDetachableModel<List<BookmarkCategory>>() {

			@Override
			protected List<BookmarkCategory> load() {
				ArrayList<BookmarkCategory> result = new ArrayList<BookmarkCategory>();
				result.add(BookmarkManager.MAIN_CATEGORY);
				return result;
			}

		};
		
		// Make use of chaining models
		PropertyListView<BookmarkCategory> cats = new PropertyListView<BookmarkCategory>("categories",bmModel) {

			@Override
			protected void populateItem(ListItem<BookmarkCategory> item) {
				item.add(new BookmarkCategoryPanel("content",item.getModel(),""));
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
