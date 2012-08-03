package freenet.winterface.web.markup;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

import freenet.node.NodeStarter;
import freenet.node.Version;

/**
 * {@link DashboardPanel} showing node's version information
 * 
 * @author pausb
 */
@SuppressWarnings("serial")
public class VersionPanel extends DashboardPanel {

	/**
	 * Constructs
	 * 
	 * @param id
	 *            {@link Component} markup ID
	 */
	public VersionPanel(String id) {
		super(id);
		// Freenet Version info
		String freenet = "Freenet " + Version.publicVersion + " Build #" + Version.buildNumber() + " " + Version.cvsRevision();
		add(new Label("freenet-ver", Model.of(freenet)));
		// Freenet-ext version info
		String freenetExt = "Freenet-ext Build #" + NodeStarter.extBuildNumber;
		if (NodeStarter.extBuildNumber < NodeStarter.RECOMMENDED_EXT_BUILD_NUMBER) {
			freenetExt += (" (" + NodeStarter.RECOMMENDED_EXT_BUILD_NUMBER + "is recommended)");
		}
		freenetExt += (" " + NodeStarter.extRevisionNumber);
		add(new Label("freenet-ext-ver", Model.of(freenetExt)));
	}

	@Override
	public String getName() {
		return "Version";
	}

}
