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
	
	final LoadableDetachableModel<PeerManager> peerManagerModel = new LoadableDetachableModel<PeerManager>() {
		@Override
		protected PeerManager load() {
			return ((WinterfaceApplication) getApplication()).getFreenetWrapper().getNode().peers;
		}
	};

	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new BookmarksPanel("bookmarks-panel"));
		add(new PeersPanel("peers-panel",new CompoundPropertyModel<PeerManager>(peerManagerModel)));
		add(new VersionPanel("version"));
	}
	
}
