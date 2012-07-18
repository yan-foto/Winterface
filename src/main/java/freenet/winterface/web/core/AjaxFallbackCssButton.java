package freenet.winterface.web.core;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

@SuppressWarnings("serial")
public abstract class AjaxFallbackCssButton extends AjaxFallbackLink<String> implements IHeaderContributor {

	private IModel<String> labelModel;
	private ButtonIcon icon = ButtonIcon.TICK;
	private boolean showIcon;

	public enum ButtonIcon {
		TICK, CANCEL, DELETE, ARROW_UP, ARROW_DOWN, ARROW_OUT,PENCIL,CUT
	}

	public AjaxFallbackCssButton(String id) {
		this(id, null);
	}

	public AjaxFallbackCssButton(String id, IModel<String> model) {
		this(id, model, null);
	}

	public AjaxFallbackCssButton(String id, IModel<String> model, ButtonIcon icon) {
		super(id);
		this.labelModel = model;
		this.icon = icon;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		// Customize Css class
		add(new AttributeAppender("class", Model.of("button")," "));
		// Add label
		Component label = null;
		if (labelModel != null) {
			label = new Label(getId() + "-label", labelModel);
			if(showIcon) {
				label.add(new AttributeAppender("class",Model.of("with-icon")," "));
				label.add(new AttributeAppender("class", Model.of(iconName())," "));
			}
		}
		else {
			label = new Image(getId()+"-label", getResource("img/"+iconName()+".png"));
		}
		add(label);
	}

	public AjaxFallbackCssButton setIcon(ButtonIcon icon) {
		this.icon = icon;
		return this;
	}

	public AjaxFallbackCssButton setIconVisible(boolean show) {
		this.showIcon = show;
		return this;
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.renderCSSReference(getResource("css-buttons.css"));
	}
	
	private PackageResourceReference getResource(String name) {
		return new PackageResourceReference(AjaxFallbackCssButton.class, name);
	}
	
	private String iconName() {
		return icon.toString().toLowerCase();
	}

}
