package freenet.winterface.web.nav;

import java.util.List;

import org.apache.wicket.Page;

/**
 * An abstract parent class for a navigation menu item
 * 
 * @author pausb
 */
public abstract interface NavItem {

	/**
	 * Returns name of menu item
	 * 
	 * @return name of menu item
	 */
	public String getName();
	
	/**
	 * Returns a list of children for current menu item. This method return a
	 * list not equal to {@code null} if the current page corresponds to this
	 * menu item and if this menu item has children
	 * 
	 * @param page
	 *            current (active) page
	 * @return list of submenus (children)
	 */
	public List<NavItem> getChilds(Page page);

	/**
	 * If menu is active in the given page
	 * 
	 * @param page
	 *            current page
	 * @return {@code true} if menu corresponds to this page
	 */
	public boolean isActive(Page page);
	
	/**
	 * {@link Page} to forward to if menu is selected
	 * 
	 * @param page
	 *            desired page if menu is selected
	 */
	public void onClick(Page page);

}
