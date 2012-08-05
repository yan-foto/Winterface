package freenet.winterface.web.markup;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

@SuppressWarnings("serial")
public abstract class ConfirmPanel extends Panel {

	/** Place-holder {@link Component} to replace this panel with */
	private Component placeHolder;
	/** Confirmation message */
	private String msg;

	/**
	 * Constructs.
	 * 
	 * @param parent
	 *            place-holder {@link Component} to show this panel in
	 * @param msg
	 *            confirmation message
	 */
	public ConfirmPanel(Component parent, String msg) {
		super(parent.getId());
		this.placeHolder = parent;
		this.msg = msg;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		Label msg = new Label("message", Model.of(this.msg));
		AjaxFallbackLink<String> okButton = new AjaxFallbackLink<String>("ok-button") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				onOk(target);
				replaceBack(target);
			}
		};
		okButton.add(new Label("ok-label", "OK"));

		AjaxFallbackLink<String> cancelButton = new AjaxFallbackLink<String>("cancel-button") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				replaceBack(target);
			}
		};
		cancelButton.add(new Label("cancel-label", "Cancel"));
		add(msg);
		add(okButton);
		add(cancelButton);

	}

	/**
	 * Removes {@link ConfirmPanel} and replaces the place-holder back
	 * 
	 * @param target
	 *            Ajax request target
	 */
	private void replaceBack(AjaxRequestTarget target) {
		ConfirmPanel.this.replaceWith(placeHolder);
		if (target != null) {
			target.add(placeHolder);
		}
	}

	/**
	 * Callback when confirm button is clicked.
	 * 
	 * @param target
	 *            AJAX request target
	 */
	protected abstract void onOk(AjaxRequestTarget target);

}
