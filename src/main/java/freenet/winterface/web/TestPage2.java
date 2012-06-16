package freenet.winterface.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;

import freenet.winterface.web.nav.NavItem;
import freenet.winterface.web.nav.PageNavItem;

@SuppressWarnings("serial")
public class TestPage2 extends WinterPage{
	
	public TestPage2() {
		add(new Label("path", getRequest().getUrl().canonical().toString()));
	}
	
	@Override
	public List<NavItem> getNavigations() {
		List<NavItem> result = new ArrayList<NavItem>();
		result.add(new PageNavItem(Dashboard.class, "Back Home"));
		return result;
	}
}
