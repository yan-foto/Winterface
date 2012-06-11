package freenet.winterface.web.core;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;

import freenet.clients.http.bookmark.BookmarkItem;

@SuppressWarnings("serial")
public class BookmarkItemView extends PropertyListView<BookmarkItem> {

	
	public BookmarkItemView(String id) {
		super(id);
	}

	@Override
	protected void populateItem(ListItem<BookmarkItem> item) {
		item.add(new Label("name"));
//		item.add(new AjaxFallbackLink<BookmarkItem>("delete") {
//
//			@Override
//			public void onClick(AjaxRequestTarget target) {
//				BookmarkItem item = getModelObject();
//				Component component = target.getPage().get("bookmarks-panel:bookmarks-container");
//				component.replaceWith(new Label(item.getName()));
//				target.add(component);
//			}
//		}.add(new Label("delete-label",Model.of("Delete"))));
	}
	
}
