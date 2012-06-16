package freenet.winterface.web.nav;

import java.util.List;

import org.apache.wicket.Page;

/**
 * Convenient way to create menu items in navigation panel
 * 
 * @author pausb
 * @see NavPanel
 * @see NavItem
 */
public class PageNavItem implements NavItem {
	
	/**
	 * Name of menu item
	 */
	private final String menuName;

	/**
	 * {@link Page} corresponding to this menu item
	 */
	private final Class<? extends Page> pageClass;

	/**
	 * Constructs
	 * 
	 * @param pageClass
	 *            {@link Page} corresponding to this menu item
	 * @param name
	 *            name of menu item
	 */
	public PageNavItem(Class<? extends Page> pageClass, String name) {
		this.menuName = name;
		this.pageClass = pageClass;
	}

	@Override
	public List<NavItem> getChilds(Page page) {
		List<NavItem> result = null;
		if (isActive(page)) {
			if (page instanceof NavContributor) {
				result = ((NavContributor) page).getNavigations();
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

	@Override
	public String getName() {
		return menuName;
	}

}
