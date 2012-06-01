package freenet.winterface.web.nav;

import org.apache.wicket.Page;

/**
 * An abstract implementation of {@link NavCallbackInterface}
 * 
 * @author pausb
 * @see NavCallbackInterface
 * 
 */
public abstract class AbstractNavCallback implements NavCallbackInterface {

	/**
	 * Label of menu
	 */
	private String name;

	/**
	 * Constructs
	 * 
	 * @param name
	 *            menu label
	 */
	public AbstractNavCallback(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isActive(Page page) {
		return false;
	}

}
