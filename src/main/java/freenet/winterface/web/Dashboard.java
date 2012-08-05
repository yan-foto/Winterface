package freenet.winterface.web;

import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;

import freenet.node.PeerManager;
import freenet.winterface.web.core.WinterfaceApplication;
import freenet.winterface.web.markup.BookmarksPanel;
import freenet.winterface.web.markup.PeersPanel;
import freenet.winterface.web.markup.VersionPanel;


/**
 * Freenet's dashboard: containing all important information
 * 
 * @author pausb
 * 
 */
@SuppressWarnings("serial")
public class Dashboard extends WinterPage {
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new BookmarksPanel("bookmarks-panel"));
		// Model to access PeerManager for PeersPanel
		final LoadableDetachableModel<PeerManager> peerManagerModel = new LoadableDetachableModel<PeerManager>() {
			@Override
			protected PeerManager load() {
				return ((WinterfaceApplication) getApplication()).getFreenetWrapper().getNode().peers;
			}
		};
		add(new PeersPanel("peers-panel",new CompoundPropertyModel<PeerManager>(peerManagerModel)));
		add(new VersionPanel("version"));
	}
	
}
