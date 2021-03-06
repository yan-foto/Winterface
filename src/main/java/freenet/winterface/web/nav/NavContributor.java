package freenet.winterface.web.nav;

import java.util.List;

import org.apache.wicket.Page;

/**
 * Every {@link Page} contributing to navigation menu must implement this
 * interface
 * 
 * @author pausb
 * @see NavPanel
 * @see NavItem
 */
public interface NavContributor {

	/**
	 * Returns a {@link List} of menu items for this {@link Page}
	 * 
	 * @return children menu items of {@link Page}
	 */
	List<NavItem> getNavigations();
}
