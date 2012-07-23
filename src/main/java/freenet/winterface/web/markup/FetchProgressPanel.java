package freenet.winterface.web.markup;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import freenet.clients.http.FProxyFetchInProgress;
import freenet.clients.http.FProxyFetchResult;
import freenet.clients.http.FProxyFetchWaiter;

@SuppressWarnings("serial")
public class FetchProgressPanel extends Panel {
	
	public FetchProgressPanel(String id, IModel<?> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		Object dmo = getDefaultModelObject();
		add(new Label("timeStarted"));
		add(new Label("totalBlocks"));
		add(new Label("fetchedBlocks"));
	}
	
	/**
	 * Returns current progress in percent.
	 * <p>
	 * This method is used to generate a value for progress tag in
	 * {@link FetchProgressPanel}
	 * </p>
	 * 
	 * @param waiter
	 *            {@link FProxyFetchWaiter} to get current progress from
	 * @return 0.0 &lt; progress &lt; 1.0
	 */
	private float getProgressPercent(FProxyFetchInProgress progress) {
		FProxyFetchResult resultFast = progress.getWaiter().getResultFast();
		float result = -1.0f;
		if (resultFast.requiredBlocks > 0) {
			result = resultFast.fetchedBlocks / resultFast.requiredBlocks;
		}
		resultFast.close();
		return result;
	}
	
}
