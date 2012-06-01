package freenet.winterface.web.markup;

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

import freenet.clients.http.bookmark.Bookmark;
import freenet.clients.http.bookmark.BookmarkCategory;
import freenet.clients.http.bookmark.BookmarkItem;
import freenet.clients.http.bookmark.BookmarkManager;

/**
 * A simple {@link Panel} to show/edit {@link Bookmark}s.
 * 
 * @author pausb
 * @see BookmarkManager
 */
public class BookmarksPanel extends Panel {

	/**
	 * Generated serial version ID
	 */
	private static final long serialVersionUID = -1277711728654734727L;
	
	public BookmarksPanel(String id) {
		super(id);
		// Get all bookmark categories and encapsulate it as an IModel
		final List<BookmarkCategory> bookmarkItems = BookmarkManager.MAIN_CATEGORY.getAllSubCategories();
		ListView<BookmarkCategory> bookmarkView = new ListView<BookmarkCategory>("category", bookmarkItems) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<BookmarkCategory> item) {
				// Name
				BookmarkCategory modelObject = item.getModelObject();
				item.add(new Label("name", modelObject.getName()));

				// List bookmarks belonging to this category
				List<BookmarkItem> items = modelObject.getItems();
				ListView<BookmarkItem> bookmarks = new ListView<BookmarkItem>("bookmarks", items) {

					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(ListItem<BookmarkItem> item) {
						item.add(new Label("bookmark", item.getModelObject().getName()));
					}
				};
				item.add(bookmarks);
			}
		};
		add(bookmarkView);
	}

}
