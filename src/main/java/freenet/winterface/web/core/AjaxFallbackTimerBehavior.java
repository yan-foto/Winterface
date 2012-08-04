package freenet.winterface.web.core;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.util.time.Duration;

public abstract class AjaxFallbackTimerBehavior extends AbstractAjaxTimerBehavior {

	private final String refreshPath;
	private static final long serialVersionUID = 1L;

	public AjaxFallbackTimerBehavior(Duration updateInterval) {
		this(updateInterval,null);
	}
	
	public AjaxFallbackTimerBehavior(Duration updateIntervale, String refreshPath) {
		super(updateIntervale);
		this.refreshPath = refreshPath;
	}
	
	@Override
	public void renderHead(Component component, IHeaderResponse response) {
		super.renderHead(component, response);
		String content = String.valueOf(getUpdateInterval().seconds());
		if(refreshPath!=null) {
			content+=(";URL='"+refreshPath);
		}
		response.render(StringHeaderItem.forString("<noscript><meta http-equiv=\"refresh\" content=\"" + content + "\"></noscript>"));
	}

}
