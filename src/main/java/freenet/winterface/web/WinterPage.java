package freenet.winterface.web;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.Localizer;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.cookies.CookieUtils;

import freenet.node.Node;
import freenet.winterface.core.Configuration;
import freenet.winterface.core.IPUtils;
import freenet.winterface.web.core.WinterfaceApplication;
import freenet.winterface.web.markup.AlertsPanel;
import freenet.winterface.web.markup.NavPanel;
import freenet.winterface.web.nav.NavContributor;
import freenet.winterface.web.nav.NavItem;
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

	/** Initial list of navigation items */
	private List<NavItem> navs;

	/** Cookie name */
	private final String COOKIE_KEY_PREFIX = "winterface.freenet";

	/**
	 * Constructs
	 */
	public WinterPage() {
		this(null);
	}

	/**
	 * Constructs
	 * 
	 * @param params
	 *            {@link PageParameters}
	 */
	public WinterPage(PageParameters params) {
		super(params);
		initNav();
	}

	/**
	 * Initiates the root navigation menu items
	 */
	private void initNav() {
		navs = new ArrayList<NavItem>();
		// Add navigation here
		navs.add(new PageNavItem(InsertPage.class, "Insert a file"));
		navs.add(new PageNavItem(AddFriendPage.class, "Add a friend"));
		navs.add(new PageNavItem(AlertsPage.class, "See all messages"));
		navs.add(new PageNavItem(QueuePage.class, "Check queues"));
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		// Navigation Panel
		LoadableDetachableModel<NavItem> navModel = new LoadableDetachableModel<NavItem>() {
			@Override
			protected NavItem load() {
				return getMainNav();
			}
		};
		add(new NavPanel("navigation", navModel));

		// Footer
		add(new AlertsPanel("footer"));

	}

	/**
	 * Serves as helper method to deliver initial navigation
	 * 
	 * @return {@link NavItem} of top menu level
	 */
	public NavItem getMainNav() {
		return new PageNavItem(null, null) {
			@Override
			public void onClick(Page page) {
				// This level is not shown so simply ignore this listener
			}

			@Override
			public List<NavItem> getChilds(Page page) {
				return navs;
			}
		};
	}

	@Override
	public List<NavItem> getNavigations() {
		return null;
	}

	/**
	 * A convenient method to feth {@link HttpServletRequest}
	 * 
	 * @return {@link HttpServletRequest} for this {@link WebPage}
	 */
	protected HttpServletRequest getHttpServletRequest() {
		ServletWebRequest request = (ServletWebRequest) getRequest();
		return request.getContainerRequest();
	}

	/**
	 * Returns {@code true} if remote host has full access.
	 * 
	 * @return {@code false} if remote host has no full access.
	 * @see Configuration
	 */
	protected boolean isAllowedFullAccess() {
		String remoteAddr = getHttpServletRequest().getRemoteAddr();
		Configuration config = ((WinterfaceApplication) getApplication()).getConfiguration();
		String[] fullAccessHosts = config.getFullAccessHosts().split(",");
		for (String host : fullAccessHosts) {
			try {
				if (IPUtils.quietMatches(host, remoteAddr)) {
					return true;
				}
			} catch (UnknownHostException e) {
				// ignore
			}
		}
		return false;
	}

	/**
	 * Save given values as a semicolon separated String in a cookie.
	 * 
	 * @param name
	 *            cookie name suffix
	 * @param values
	 *            values
	 */
	public void saveInConfigCookie(String suffix, String... values) {
		cookieUtils().save(COOKIE_KEY_PREFIX + suffix, values);
	}

	/**
	 * Loads values assigned with desired cookie
	 * 
	 * @param name
	 *            cookie name suffix
	 * @return array of assigned values
	 */
	public String[] loadFromConfigCookie(String suffix) {
		String load = cookieUtils().load(COOKIE_KEY_PREFIX + suffix);
		return load != null ? load.split(FormComponent.VALUE_SEPARATOR) : null;
	}

	/**
	 * Convenient method to get {@link WinterfaceApplication}
	 * {@link CookieUtils}
	 */
	private CookieUtils cookieUtils() {
		return ((WinterfaceApplication) getApplication()).getCookieUtils();
	}

	/**
	 * Uses {@link Localizer} to get corresponding value for the given key
	 * <p>
	 * This method is meant for values without place holders. To localize
	 * dynamic values see {@link #localize(String, IModel)}
	 * </p>
	 * 
	 * @param key
	 *            String key
	 * @return localized value
	 */
	protected String localize(String key) {
		return getLocalizer().getString(key, this);
	}

	/**
	 * Uses {@link Localizer} to get corresponding value for the given key.
	 * 
	 * @param key
	 *            String key
	 * @param model
	 *            model to replace place holders in value
	 * @return localized value
	 */
	protected String localize(String key, IModel<?> model) {
		return getLocalizer().getString(key, this, model);
	}

	/**
	 * A convenient method to fetch Freenet {@link Node}
	 */
	protected Node getFreenetNode() {
		return ((WinterfaceApplication) getApplication()).getFreenetWrapper().getNode();
	}
	
	/**
	 * @return plugin configuration
	 */
	protected Configuration getConfiguration() {
		return ((WinterfaceApplication) getApplication()).getConfiguration();
	}

}
