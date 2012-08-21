package freenet.winterface.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Localizer;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

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

	// L10N keys
	public final static String L10N_MINOR = "AlertsPage.minor";
	public final static String L10N_WARNING = "AlertsPage.warning";
	public final static String L10N_ERROR = "AlertsPage.error";
	public final static String L10N_CRITICAL = "AlertsPage.critical";

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
	 * @see #getFilteredValidAlerts(int)
	 */
	public synchronized static List<UserAlert> getValidAlerts() {
		// Predicate which returns true for all valid alerts
		Predicate<UserAlert> filter = new Predicate<UserAlert>() {
			@Override
			public boolean apply(UserAlert input) {
				return input.isValid();
			}
		};
		Iterable<UserAlert> allAlerts = Arrays.asList(getManager().getAlerts());
		Iterable<UserAlert> filteredAlerts = Iterables.filter(allAlerts, filter);
		return ImmutableList.copyOf(filteredAlerts);
	}

	/**
	 * Returns a filtered list of valid {@link UserAlert}(s).
	 * 
	 * @param priorityClass
	 *            desired priority class as filtering parameter
	 * @return filtered alerts
	 */
	public synchronized static List<UserAlert> getFilteredValidAlerts(int priorityClass) {
		List<UserAlert> result = new ArrayList<UserAlert>();
		List<UserAlert> allAlerts = getValidAlerts();
		for (UserAlert userAlert : allAlerts) {
			if (userAlert.getPriorityClass() <= priorityClass) {
				result.add(userAlert);
			}
		}
		return result;
	}

	/**
	 * Dismisses an {@link UserAlert}
	 * 
	 * @param alertHashCode
	 *            hashcode of alert to dismiss
	 * @see UserAlert#hashCode()
	 * @see #getManager()
	 */
	public synchronized static void dismissAlert(int alertHashCode) {
		getManager().dismissAlert(alertHashCode);
	}

	/**
	 * Counts number of {@link UserAlert}(s) according to their priority class.
	 * 
	 * @param alerts
	 *            list of alerts to count
	 * @return an array where each index corresponds to a priority class and the
	 *         content equals to number of that class in given list
	 * @see UserAlert#getPriorityClass()
	 */
	public static int[] countAlertsByPriority(List<UserAlert> alerts) {
		// Four priority types -> array of four ints
		int[] result = new int[4];
		for (UserAlert alert : alerts) {
			result[alert.getPriorityClass()]++;
		}
		return result;
	}

	/**
	 * Returns the localized title of priority class.
	 * <p>
	 * Note: this method can only be called from inside of a running application
	 * {@link Application}
	 * </p>
	 * 
	 * @param priorityClass
	 * @return localized priority class
	 * @see UserAlert
	 */
	public static String getLocalizedTitle(int priorityClass) {
		// This would only called if the Application is found in callers thread
		Localizer localizer = Application.get().getResourceSettings().getLocalizer();
		String key = null;
		switch (priorityClass) {
		case UserAlert.MINOR:
			key = L10N_MINOR;
			break;
		case UserAlert.WARNING:
			key = L10N_WARNING;
			break;
		case UserAlert.ERROR:
			key = L10N_ERROR;
			break;
		case UserAlert.CRITICAL_ERROR:
			key = L10N_CRITICAL;
			break;
		}
		return localizer.getString(key, null);
	}

}
