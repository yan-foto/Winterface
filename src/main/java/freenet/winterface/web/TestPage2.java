package freenet.winterface.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;

import freenet.winterface.web.nav.AbstractNavItem;
import freenet.winterface.web.nav.PageNavItem;

public class TestPage2 extends WinterPage{
	
	/**
	 * Generated serial version ID
	 */
	private static final long serialVersionUID = -286439131102758658L;
	
	public TestPage2() {
		add(new Label("path", getRequest().getUrl().canonical().toString()));
	}
	
	@Override
	public List<AbstractNavItem> getNavigations() {
		List<AbstractNavItem> result = new ArrayList<AbstractNavItem>();
		result.add(new PageNavItem(Dashboard.class, "Back Home"));
		return result;
	}
}
