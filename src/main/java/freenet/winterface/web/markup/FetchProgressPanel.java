package freenet.winterface.web.markup;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import freenet.clients.http.FProxyFetchResult;

@SuppressWarnings("serial")
public class FetchProgressPanel extends Panel {

	public FetchProgressPanel(String id, IModel<FProxyFetchResult> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new Label("elapsed", elapsedModel));
		add(new Label("totalBlocks"));
		add(new Label("fetchedBlocks"));
		add(new Label("progress",progressModel).setEscapeModelStrings(false));
	}
	
	private final LoadableDetachableModel<String> progressModel = new LoadableDetachableModel<String>() {
		@Override
		protected String load() {
			FProxyFetchResult result = (FProxyFetchResult) FetchProgressPanel.this.getDefaultModelObject();
			StringBuffer progress = new StringBuffer();
			progress.append("<progress");
			if(result.totalBlocks>0) {
				float percent = ((float)result.fetchedBlocks / (float)result.totalBlocks);
				progress.append(" value=\"").append(percent).append("\">");
				progress.append(percent).append(" %");
			} else {
				progress.append(">");
			}
			progress.append("</progress>");
			return progress.toString();
		}
	};
	
	private final LoadableDetachableModel<String> elapsedModel = new LoadableDetachableModel<String>() {
		@Override
		protected String load() {
			FProxyFetchResult result = (FProxyFetchResult) FetchProgressPanel.this.getDefaultModelObject();
			long elapsed = (System.currentTimeMillis() - result.timeStarted) / 1000;
			String second = getApplication().getResourceSettings().getLocalizer().getString("FetchProgressPanel.timeUnit", FetchProgressPanel.this);
			return elapsed + " " + second;
		}
	};

}
