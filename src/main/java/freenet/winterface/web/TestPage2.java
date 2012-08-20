package freenet.winterface.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;

import freenet.winterface.web.markup.LocalBrowserPanel;
import freenet.winterface.web.nav.NavItem;
import freenet.winterface.web.nav.PageNavItem;

@SuppressWarnings("serial")
public class TestPage2 extends WinterPage{
	
	public TestPage2() {
		add(new LocalBrowserPanel("path",Model.of("/home/pouyan/")){
			@Override
			public void fileSelected(String path, AjaxRequestTarget target) {
				System.out.println("**************** "+path);
			}
			
		});
	}
	
	@Override
	public List<NavItem> getNavigations() {
		List<NavItem> result = new ArrayList<NavItem>();
		result.add(new PageNavItem(Dashboard.class, "Back Home"));
		return result;
	}
}
