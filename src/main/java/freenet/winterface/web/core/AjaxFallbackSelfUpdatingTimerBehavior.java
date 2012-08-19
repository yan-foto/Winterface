package freenet.winterface.web.core;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.util.time.Duration;

/**
 * A {@link Behavior} similar to {@link AjaxSelfUpdatingTimerBehavior} with the
 * difference that if JavaScript is not available a meta-refresh tag is added to
 * {@link Component}s corresponding {@link Page}, which causes the page to
 * refresh regarding the desired {@link Duration}
 * <p>
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
		super.renderHead(component, response);
		response.render(StringHeaderItem.forString("<noscript><meta http-equiv=\"refresh\" content=\"" + getUpdateInterval().seconds() + "\"></noscript>"));
	}

}
