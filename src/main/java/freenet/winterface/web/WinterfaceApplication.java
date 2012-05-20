package freenet.winterface.web;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

public class WinterfaceApplication extends WebApplication{

	@Override
	public Class<? extends Page> getHomePage() {
		return StartPage.class;
	}

}
