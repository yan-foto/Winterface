package freenet.winterface.web;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

/**
 * {@link WebPage} to show when HTTP errors occur
 * 
 * @author pausb
 * 
 */
@SuppressWarnings("serial")
public class ErrorPage extends WebPage {

	@Override
	protected void onInitialize() {
		super.onInitialize();
		Label message = new Label("message", Model.of("Run Rabbit! Run!"));
		add(message);
	}

}
