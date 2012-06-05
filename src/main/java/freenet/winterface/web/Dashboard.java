package freenet.winterface.web;

import freenet.winterface.web.markup.BookmarksPanel;
import freenet.winterface.web.markup.PeersPanel;


/**
 * Freenet's dashboard: containing all important information
 * 
 * @author pausb
 * 
 */
@SuppressWarnings("serial")
public class Dashboard extends WinterPage {

	public Dashboard() {
		super();
		add(new BookmarksPanel("bookmarks-panel"));
		add(new PeersPanel("peers-panel"));
	}
	
}
