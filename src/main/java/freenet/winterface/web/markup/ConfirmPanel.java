package freenet.winterface.web.markup;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

@SuppressWarnings("serial")
public abstract class ConfirmPanel extends Panel {

	private Component parent;
	private String msg;

	public ConfirmPanel(Component parent, String msg) {
		super(parent.getId());
		this.parent = parent;
		this.msg = msg;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		Label msg = new Label("message", Model.of(this.msg));
		AjaxFallbackLink<String> okButton = new AjaxFallbackLink<String>("ok-button") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				onOk();
				replaceBack(target);
			}
		};
		okButton.add(new Label("ok-label","OK"));
		
		AjaxFallbackLink<String> cancelButton = new AjaxFallbackLink<String>("cancel-button") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				replaceBack(target);
			}
			
		};
		cancelButton.add(new Label("cancel-label","Cancel"));
		add(msg);
		add(okButton);
		add(cancelButton);
		
	}
	
	private void replaceBack(AjaxRequestTarget target) {
		ConfirmPanel.this.replaceWith(parent);
		if(target!=null) {
			target.add(parent);
		}
	}
	
	protected abstract void onOk();

}
