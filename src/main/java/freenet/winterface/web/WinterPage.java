package freenet.winterface.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import freenet.winterface.web.markup.NavPanel;
import freenet.winterface.web.nav.AbstractNavItem;
import freenet.winterface.web.nav.NavContributor;
import freenet.winterface.web.nav.PageNavItem;

/**
 * Base {@link WebPage} for all other WinterFace {@link Page}s.
 * <p>
 * This {@link Page} contains the logo and the navigation menu.<br />
 * This class also contains initial items of navigation menu (see
 * {@link #getMainNav()})
 * </p>
 * 
 * @author pausb
 * @see NakedWinterPage
 */
@SuppressWarnings("serial")
public abstract class WinterPage extends WebPage implements NavContributor {

	/**
	 * Initial list of navigation items
	 */
	private List<AbstractNavItem> navs;

	public WinterPage() {
		navs = new ArrayList<AbstractNavItem>();
		// Add navigation here
		navs.add(new PageNavItem(TestPage.class, "Menu 1"));
		navs.add(new PageNavItem(TestPage2.class, "Helloooooow"));
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		// Navigation Panel
		LoadableDetachableModel<AbstractNavItem> navModel = new LoadableDetachableModel<AbstractNavItem>() {

			@Override
			protected AbstractNavItem load() {
				return getMainNav();
			}
		};
		add(new NavPanel("navigation", navModel));

		// Footer
		add(new Label("footer", Model.of("FOOTER")));

	}

	/**
	 * Serves as helper method to deliver initial navigation
	 * 
	 * @return {@link AbstractNavItem} of top menu level
	 */
	public AbstractNavItem getMainNav() {
		return new PageNavItem(null, null) {

			@Override
			public void onClick(Page page) {
				// This level is not shown so simply ignore this listener
			}

			@Override
			public List<AbstractNavItem> getChilds(Page page) {
				return navs;
			}
		};
	}

	@Override
	public List<AbstractNavItem> getNavigations() {
		return null;
	}
}
