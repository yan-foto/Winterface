package freenet.winterface.web.core;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

@SuppressWarnings("serial")
public abstract class AjaxFallbackCssButton extends AjaxFallbackLink<String> implements IHeaderContributor {

	private IModel<String> labelModel;
	private button_icon icon = button_icon.TICK;
	private boolean showIcon;

	public static enum button_icon {
		TICK, CANCEL, DELETE, ARROW_UP, ARROW_DOWN, ARROW_OUT,PENCIL,CUT
	}

	public AjaxFallbackCssButton(String id) {
		this(id, null);
	}

	public AjaxFallbackCssButton(String id, IModel<String> model) {
		this(id, model, button_icon.TICK);
	}

	public AjaxFallbackCssButton(String id, IModel<String> model, button_icon icon) {
		super(id);
		this.labelModel = model;
		this.icon = icon;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		// Customize Css class
		add(new AttributeAppender("class", " " + "button"));
		// Add label
		Component label = null;
		if (labelModel != null) {
			label = new Label(getId() + "-label", labelModel);
			if (showIcon) {
				label.add(new AttributeAppender("class", " " + iconName()));
			}
		}else {
			label = new Image(getId()+"-label", getResource("img/"+iconName()+".png"));
		}
		add(label);
	}

	public void setIcon(button_icon icon) {
		this.icon = icon;
	}

	public void setIconVisible(boolean show) {
		this.showIcon = show;
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		if (labelModel != null && showIcon) {
			response.renderCSSReference(getResource("css-buttons.css"));
		}
	}
	
	private PackageResourceReference getResource(String name) {
		return new PackageResourceReference(AjaxFallbackCssButton.class, name);
	}
	
	private String iconName() {
		return icon.toString().toLowerCase();
	}

}
