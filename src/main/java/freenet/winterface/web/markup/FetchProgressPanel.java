package freenet.winterface.web.markup;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

@SuppressWarnings("serial")
public class FetchProgressPanel extends Panel {

	private IModel<Float> progressModel;

	public FetchProgressPanel(String id, IModel<Float> model) {
		super(id);
		this.progressModel = model;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		StringBuffer buffer = new StringBuffer("<progress");
		if (!(progressModel.getObject() <= 0)) {
			buffer.append(" value=");
			buffer.append(progressModel.getObject());
		}
		buffer.append("></progress>");
		Label progress = new Label("progress",Model.of(buffer.toString()));
		progress.setEscapeModelStrings(false);
		add(progress);
	}
	
}
