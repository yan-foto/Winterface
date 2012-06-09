package freenet.winterface.web.core;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;

import freenet.clients.http.bookmark.BookmarkItem;

@SuppressWarnings("serial")
public class BookmarkItemModel extends PropertyListView<BookmarkItem> {

	
	public BookmarkItemModel(String id) {
		super(id);
	}

	@Override
	protected void populateItem(ListItem<BookmarkItem> item) {
		item.add(new Label("name"));
	}
	
}
