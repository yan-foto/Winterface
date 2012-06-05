package freenet.winterface.web.markup;

import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;

import freenet.node.PeerManager;
import freenet.winterface.core.FreenetWrapper;
import freenet.winterface.core.WinterfaceApplication;


@SuppressWarnings("serial")
public class PeersPanel extends DashboardPanel {

	public PeersPanel(String id) {
		super(id);
		WinterfaceApplication app = (WinterfaceApplication) getApplication();
		FreenetWrapper freenetWrapper = app.getFreenetWrapper();
		PeerManager peers = freenetWrapper.getNode().peers;
		// Container
		WebMarkupContainer container = new WebMarkupContainer("peers-stat");
		container.setOutputMarkupId(true); // Needed for Ajax auto refresh
		container.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(30)));
		add(container);
		// All peers
		Label allPeers = new Label("peers-count",Model.of(peers.countConnectedPeers()));
		container.add(allPeers);
		// Darknet Peers
		Label darknetPeers = new Label("darknet-count", Model.of(peers.countConnectedDarknetPeers()));
		container.add(darknetPeers);
		// Opennet Peers
		Label opennetPeers = new Label("opennet-count", Model.of(peers.countConnectedDarknetPeers()));
		container.add(opennetPeers);
		// Backed off peers
		Label backedOff = new Label("backed-count", Model.of(peers.countBackedOffPeers(true)));
		container.add(backedOff);
		// Seed Nodes
		Label seedCount = new Label("seed-count", Model.of(peers.countSeednodes()));
		container.add(seedCount);
	}

	@Override
	public String getName() {
		return "Peers Status";
	}

}
