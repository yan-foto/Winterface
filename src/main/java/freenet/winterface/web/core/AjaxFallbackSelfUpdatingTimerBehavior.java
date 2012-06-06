package freenet.winterface.web.core;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.util.time.Duration;

/**
 * A {@link Behavior} similar to {@link AjaxSelfUpdatingTimerBehavior} with the
 * difference that if JavaScript is not available a meta-refresh tag is added to
 * {@link Component}s corresponding {@link Page}, which causes the page to
 * refresh regarding the desired {@link Duration}
 * <p>
 * Please note that JS detection is done by utilizing {@link WebClientInfo},
 * which collects data only <b>once</b> per session. So if user turns off the
 * JavaScript in middle of the session, this method return faulty results (see source of 
 * {@link AjaxFallbackSelfUpdatingTimerBehavior#isJSEnabled()}).
 * </p>
 * 
 * @author pausb
 * 
 */
@SuppressWarnings("serial")
public class AjaxFallbackSelfUpdatingTimerBehavior extends AjaxSelfUpdatingTimerBehavior {

	/**
	 * Constructs
	 * 
	 * @param updateInterval
	 *            desired {@link Duration} to refresh content
	 */
	public AjaxFallbackSelfUpdatingTimerBehavior(Duration updateInterval) {
		super(updateInterval);
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response) {
		if (isJSEnabled()) {
			super.renderHead(component, response);
		} else {
			response.renderString("<meta http-equiv=\"refresh\" content=\"" + getUpdateInterval().seconds() + "\">");
		}
	}

	/**
	 * Checks if JavaScript is enable.
	 * 
	 * @return {@code false} if JavaScript is not available.
	 */
	protected boolean isJSEnabled() {
		WebClientInfo clientInfo = (WebClientInfo) getComponent().getSession().getClientInfo();
		return clientInfo.getProperties().isJavaEnabled();
	}

}
