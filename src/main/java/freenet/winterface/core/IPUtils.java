package freenet.winterface.core;

import java.util.regex.Matcher;

/**
 * Does all IP related calculations such as subnetting.
 * 
 * @author pausb
 * @see IPAddress
 * @see IPFilter
 */
public final class IPUtils {

	/** Hint characte if its IPv4 */
	final static String IPV4_HINT = ".";
	/** Hint characte if its IPv6 */
	final static String IPV6_HINT = ":";

	/**
	 * Creates an {@link IPAddress} from given {@link String}.
	 * 
	 * @param addr
	 *            string to turn into {@link IPAddress}
	 * @return generated {@link IPAddress}
	 */
	public static IPAddress stringToIP(String addr) {
		if (addr.contains(IPV4_HINT)) {
			return new IPv4Address(addr);
		} else if (addr.contains(IPV6_HINT)) {
			return new IPv6Address(IPV6_HINT);
		} else {
			throw new IllegalArgumentException("Invalid format");
		}
	}

	/**
	 * Compares <i>other</i> IP address against <i>base</i> IP address.
	 * <p>
	 * Note that only base IP is allowed to be in CIDR format, other wise an
	 * {@link IllegalArgumentException} is thrown.
	 * </p>
	 * 
	 * @param base
	 *            IP in {@link String} format
	 * @param other
	 *            IP in {@link String} format
	 * @return {@code true} if base IP <i>contains</i> other IP
	 */
	public static boolean matches(String base, String other) {
		IPAddress baseIP = stringToIP(base);
		IPAddress otherIp = stringToIP(other);
		return baseIP.matches(otherIp);
	}

	/**
	 * Acts same as {@link #matches(String, String)}, but catches all possible
	 * {@link RuntimeException}s (e.g. different IP versions) and simply returns
	 * {@code false}.
	 * 
	 * @param base
	 *            IP in {@link String} format
	 * @param other
	 *            IP in {@link String} format
	 * @return {@code true} if base IP <i>contains</i> other IP
	 */
	public static boolean quietMatches(String base, String other) {
		try {
			return matches(base, other);
		} catch (RuntimeException e) {
			// do nothing;
		}
		return false;
	}

	/**
	 * Returns {@code true} if address is a valid IPv4 or IPv6
	 * 
	 * @param addr
	 *            IP address in {@link String} format
	 * @return {@code false} if address in invalid
	 * @see IPAddress
	 */
	public static boolean isValid(String addr) {
		Matcher matcher = IPAddress.IPV4_PATTERN.matcher(addr);
		if (matcher.matches()) {
			return true;
		}
		matcher = IPAddress.IPV6_PATTERN.matcher(addr);
		return matcher.matches();
	}
}
