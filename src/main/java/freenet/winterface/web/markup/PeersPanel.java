package freenet.winterface.web.markup;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.time.Duration;

import freenet.node.PeerManager;

/**
 * {@link DashboardPanel} which shows sumarized information about peers.
 * 
 * @author pausb
 */
@SuppressWarnings("serial")
public class PeersPanel extends DashboardPanel {

	/**
	 * Constructs
	 * 
	 * @param id
	 *            {@link Component} markup ID
	 * @param model
	 *            {@link IModel} to retrieve data from
	 */
	public PeersPanel(String id, IModel<PeerManager> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		// Container
		WebMarkupContainer container = new WebMarkupContainer("peers-stat");
		container.setOutputMarkupId(true); // Needed for Ajax auto refresh
		container.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(30)));
		add(container);
		// All peers
		Label allPeers = new Label("countConnectedPeers");
		container.add(allPeers);
		// Darknet Peers
		Label darknetPeers = new Label("countConnectedDarknetPeers");
		container.add(darknetPeers);
		// Opennet Peers
		Label opennetPeers = new Label("countConnectedOpennetPeers");
		container.add(opennetPeers);
		// Backed off peers
		Label backedOff = new Label("countBackedOffPeersEither");
		container.add(backedOff);
		// Seed Nodes
		Label seedCount = new Label("countSeednodes");
		container.add(seedCount);
	}

	@Override
	public String getName() {
		return "Peers Status";
	}

}
