package freenet.winterface.web.core;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.model.Model;

import freenet.clients.http.bookmark.BookmarkItem;
import freenet.clients.http.bookmark.BookmarkManager;
import freenet.winterface.web.markup.ConfirmPanel;

@SuppressWarnings("serial")
public class BookmarkItemView extends PropertyListView<BookmarkItem> {
	
	private String parentPath;
	
	private static final Logger logger = Logger.getLogger(BookmarkItemView.class);

	public BookmarkItemView(String id,String parentPath) {
		super(id);
		setReuseItems(true);
		this.parentPath = parentPath;
	}

	@Override
	protected void populateItem(final ListItem<BookmarkItem> item) {
		System.out.println(item);
		item.add(new Label("name"));
		// Add Controls
		final WebMarkupContainer feedback = new WebMarkupContainer("feedback");
		feedback.setOutputMarkupId(true);
		item.add(feedback);
		// Delete Button + Confirm Panel
		AjaxFallbackLink<String> deleteButton = new AjaxFallbackLink<String>("delete", Model.of("delete")) {
			@Override
			public void onClick(final AjaxRequestTarget target) {
				ConfirmPanel cp = new ConfirmPanel(feedback,"Are you sure?") {
					@Override
					protected void onOk() {
						BookmarkManager bookmarkManager = ((WinterfaceApplication)getPage().getApplication()).getFreenetWrapper().getBookmarkManager();
						String bookmarkPath = parentPath+"/"+item.getModel().getObject().getName();
						bookmarkManager.removeBookmark(bookmarkPath);
						bookmarkManager.storeBookmarks();
						logger.info("Removed bookmark "+bookmarkPath);
					}
				};
				item.replace(cp);
				if(target!=null) {
					target.add(cp);
				}
			}
		};
		deleteButton.add(new Label("delete-label", Model.of("Delete")));
		item.add(deleteButton);
	}
	
}
