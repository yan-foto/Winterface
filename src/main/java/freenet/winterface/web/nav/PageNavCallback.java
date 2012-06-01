package freenet.winterface.web.nav;

import java.util.List;

import org.apache.wicket.Page;

/**
 * Convenient way to create menu items in navigation panel
 * 
 * @author pausb
 * @see NavPanel
 * @see NavCallbackInterface
 */
public class PageNavCallback extends AbstractNavCallback {

	/**
	 * {@link Page} corresponding to this menu item
	 */
	private Class<? extends Page> pageClass;

	/**
	 * Constructs
	 * 
	 * @param pageClass
	 *            {@link Page} corresponding to this menu item
	 * @param name
	 *            name of menu item
	 */
	public PageNavCallback(Class<? extends Page> pageClass, String name) {
		super(name);
		this.pageClass = pageClass;
	}

	@Override
	public List<NavCallbackInterface> getChilds(Page page) {
		List<NavCallbackInterface> result = null;
		if (isActive(page)) {
			if (page instanceof NavigationContributer) {
				result = ((NavigationContributer) page).getNavigations();
			}
		}
		return result;
	}

	@Override
	public boolean isActive(Page page) {
		return pageClass.equals(page.getClass());
	}

	@Override
	public void onClick(Page page) {
		page.setResponsePage(pageClass);
	}

}
