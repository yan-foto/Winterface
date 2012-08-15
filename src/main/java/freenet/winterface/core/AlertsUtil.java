package freenet.winterface.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.Component;

import freenet.node.useralerts.UserAlert;
import freenet.node.useralerts.UserAlertManager;
import freenet.winterface.web.WinterPage;
import freenet.winterface.web.core.WinterfaceApplication;

/**
 * Util to make life easier when using {@link UserAlertManager} and
 * {@link UserAlert}
 * 
 * @author pausb
 * 
 */
public class AlertsUtil {

	/**
	 * Returns Freenet's {@link UserAlertManager}.
	 * <p>
	 * <b>WARNING</b> This method throws an exception if its being called
	 * outside of {@link WinterfaceApplication}. So call this always inside a
	 * {@link Component} (e.g. {@link WinterPage}).
	 * <p>
	 * 
	 * @return {@link UserAlertManager}
	 * @see FreenetWrapper
	 */
	public static UserAlertManager getManager() {
		Application application = Application.get();
		if (!(application instanceof WinterfaceApplication)) {
			throw new IllegalStateException("No Winterface application is running");
		}
		return ((WinterfaceApplication) application).getFreenetWrapper().getNode().clientCore.alerts;
	}

	/**
	 * Returns a {@link List} of <i>valid</i> {@link UserAlert}(s).
	 * 
	 * @return valid alerts
	 * @see #getFilteredAlerts(int)
	 */
	public synchronized static List<UserAlert> getAllAlerts() {
		List<UserAlert> result = new ArrayList<UserAlert>();
		for (UserAlert userAlert : getManager().getAlerts()) {
			if (userAlert.isValid()) {
				result.add(userAlert);
			}
		}
		return result;
	}

	/**
	 * Returns a filtered list of valid {@link UserAlert}(s).
	 * 
	 * @param priorityClass
	 *            desired priority class as filtering parameter
	 * @return filtered alerts
	 */
	public synchronized static List<UserAlert> getFilteredAlerts(int priorityClass) {
		List<UserAlert> result = new ArrayList<UserAlert>();
		List<UserAlert> allAlerts = getAllAlerts();
		for (UserAlert userAlert : allAlerts) {
			if (userAlert.getPriorityClass() <= priorityClass) {
				result.add(userAlert);
			}
		}
		return result;
	}
	
	/**
	 * Dismisses an {@link UserAlert}
	 * @param alertHashCode hashcode of alert to dismiss
	 * @see UserAlert#hashCode()
	 * @see #getManager()
	 */
	public synchronized static void dismissAlert(int alertHashCode) {
		getManager().dismissAlert(alertHashCode);
	}

}
