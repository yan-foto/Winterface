package freenet.winterface.web.core;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;

import freenet.clients.http.bookmark.BookmarkCategory;
import freenet.clients.http.bookmark.BookmarkItem;
import freenet.clients.http.bookmark.BookmarkManager;
import freenet.winterface.web.core.AjaxFallbackCssButton.ButtonIcon;
import freenet.winterface.web.markup.BookmarkCategoryPanel;
import freenet.winterface.web.markup.BookmarksPanel;

/**
 * A {@link PropertyListView} to render {@link BookmarkItem}s of a
 * {@link BookmarkCategory}.
 * 
 * @author pausb
 * @see BookmarkCategoryPanel
 * @see BookmarksPanel
 */
@SuppressWarnings("serial")
public class BookmarkItemView extends PropertyListView<BookmarkItem> {

	/** {@link B1ookmarkManager} to manage this {@link BookmarkItem}s */
	private transient BookmarkManager bookmarkManager;

	/** Absolute path of parent item */
	private final String parentPath;

	/** Log4j logger */
	private static final Logger logger = Logger.getLogger(BookmarkItemView.class);

	/**
	 * Constructs
	 * 
	 * @param id
	 *            Wicket ID of list container
	 * @param parentPath
	 *            Absolute path of parent item
	 */
	public BookmarkItemView(String id, String parentPath) {
		super(id);
		setReuseItems(true);
		this.parentPath = parentPath;
		bookmarkManager = ((WinterfaceApplication) getApplication()).getFreenetWrapper().getBookmarkManager();
	}

	@Override
	protected void populateItem(final ListItem<BookmarkItem> item) {
		final String bookmarkPath = parentPath + "/" + item.getModel().getObject().getName();
		// Class to differentiate lines
		boolean odd = (item.getIndex() % 2 == 0);
		String rowClass = (odd ? "odd" : "even") + "-row";
		item.add(new AttributeAppender("class", rowClass));
		// Item name
		item.add(new Label("name"));
		// Feedback Panel
		Component feedback = item.findParent(BookmarkCategoryPanel.class).get("feedback");
		// Edit link
		AjaxFallbackCssButton editButton = createEditButton();
		item.add(editButton);
		// Delete link (with confirm)
		AjaxFallbackConfirmLink deleteButton = createDeleteButton(bookmarkPath, feedback);
		item.add(deleteButton);
		// Cut link
		AjaxFallbackCssButton cutButton = createCutButton();
		item.add(cutButton);
		// Move up
		AjaxFallbackCssButton upButton = createMoveupButton(item, bookmarkPath);
		item.add(upButton);
		// Move down
		AjaxFallbackCssButton downButton = createMoveDownButton(item, bookmarkPath);
		item.add(downButton);
	}

	/**
	 * Creates edit button for current bookmark item
	 * 
	 * @return edit button
	 */
	private AjaxFallbackCssButton createEditButton() {
		AjaxFallbackCssButton editButton = new AjaxFallbackCssButton("edit") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				// TODO Auto-generated method stub

			}
		};
		editButton.setIcon(ButtonIcon.PENCIL);
		return editButton;
	}

	/**
	 * Create delete button for current bookmark item
	 * 
	 * @param bookmarkPath
	 *            item's path
	 * @param feedback
	 *            panel to show feedback
	 * @return delte button
	 */
	private AjaxFallbackConfirmLink createDeleteButton(final String bookmarkPath, Component feedback) {
		AjaxFallbackConfirmLink deleteButton = new AjaxFallbackConfirmLink("delete", feedback) {
			@Override
			public void onConfirm(AjaxRequestTarget target) {
				bookmarkManager.removeBookmark(bookmarkPath);
				bookmarkManager.storeBookmarks();
				logger.info("Removed bookmark " + bookmarkPath);
			}
		};
		deleteButton.setIcon(ButtonIcon.CANCEL);
		return deleteButton;
	}

	/**
	 * Creates cut button for current bookmark item
	 * 
	 * @return cut button TODO: no implemented yet!
	 */
	private AjaxFallbackCssButton createCutButton() {
		AjaxFallbackCssButton cutButton = new AjaxFallbackCssButton("cut") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				// TODO Auto-generated method stub
			}
		};
		cutButton.setIcon(ButtonIcon.CUT);
		return cutButton;
	}

	/**
	 * Creates button to move bookmark one step up in bookmark list
	 * 
	 * @param item
	 *            bookmark item
	 * @param bookmarkPath
	 *            bookmark's path
	 * @return move up button
	 */
	private AjaxFallbackCssButton createMoveupButton(final ListItem<BookmarkItem> item, final String bookmarkPath) {
		AjaxFallbackCssButton upButton = new AjaxFallbackCssButton("up") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				bookmarkManager.moveBookmarkUp(bookmarkPath, true);
				logger.info("Moved bookmark up " + bookmarkPath);
			}
		};
		upButton.setIcon(ButtonIcon.ARROW_UP);
		// First item cannot be moved up again
		if (item.getIndex() == 0) {
			upButton.setVisible(false);
		}
		return upButton;
	}

	/**
	 * Creates button to move bookmark one step up in bookmark list
	 * 
	 * @param item
	 *            bookmark item
	 * @param bookmarkPath
	 *            bookmark's path
	 * @return move down button
	 */
	private AjaxFallbackCssButton createMoveDownButton(final ListItem<BookmarkItem> item, final String bookmarkPath) {
		AjaxFallbackCssButton downButton = new AjaxFallbackCssButton("down") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				bookmarkManager.moveBookmarkDown(bookmarkPath, true);
				logger.info("Moved bookmark down " + bookmarkPath);
			}
		};
		downButton.setIcon(ButtonIcon.ARROW_DOWN);
		// Last item cannot be moved down
		if (item.getIndex() == getViewSize() - 1) {
			downButton.setVisible(false);
		}
		return downButton;
	}

}
