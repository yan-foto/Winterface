package freenet.winterface.web;

import freenet.winterface.web.markup.BookmarksPanel;


/**
 * Freenet's dashboard: containing all important information
 * 
 * @author pausb
 * 
 */
public class Dashboard extends WinterPage {

	/**
	 * Generated serial version ID
	 */
	private static final long serialVersionUID = 6838537407373153818L;

	public Dashboard() {
		super();
		add(new BookmarksPanel("bookmarks-panel"));
	}
	
}
