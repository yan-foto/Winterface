package freenet.winterface.core;

import java.util.regex.Pattern;

/**
 * A general representaion of IPv4 and IPv6.
 * 
 * @author pausb
 * @see IPv4Address
 * @see IPv6Address
 */
public interface IPAddress {

	/** Regex of valid IPv4 */
	String IPV4_FORMAT = "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})([/(\\d{1,2})]?)";
	/** {@link Pattern} which matches a valid IPv4 */
	Pattern IPV4_PATTERN = Pattern.compile(IPV4_FORMAT);
	/** Regex of valid IPv6 */
	String IPV6_FORMAT = "([a-fA-F\\d]{1,4}):([a-fA-F\\d]{1,4}):([a-fA-F\\d]{1,4}):([a-fA-F\\d]{1,4}):([a-fA-F\\d]{1,4}):([a-fA-F\\d]{1,4}):([a-fA-F\\d]{1,4}):([a-fA-F\\d]{1,4})([/\\d{1,3}]?)";
	/** {@link Pattern} which matches a valid IPv6 */
	Pattern IPV6_PATTERN = Pattern.compile(IPV6_FORMAT);
	/** Character denoting start of subnet mask */
	String MASK_CHAR = "/";
	/** A mask to imitate unsigned byte */
	short BYTE_MASK = 0xff;

	/**
	 * Version of IP (v4 or v6)
	 */
	enum Version {
		IPv4, IPv6
	};

	/**
	 * Returns {@code true} if IP matches given address.
	 * <p>
	 * Note that the comparision can only be done between IPs of same version
	 * (e.g. IPv4 or IPv6)
	 * </p>
	 * 
	 * @param other
	 *            IP in string format
	 * @return {@code false} if given IP doesn't match this one
	 */
	boolean matches(String other);

	/**
	 * Returns {@code true} if IP matches given address.
	 * <p>
	 * Note that the comparision can only be done between IPs of same version
	 * (e.g. IPv4 or IPv6)
	 * </p>
	 * 
	 * @param other
	 *            Other IP
	 * @return {@code false} if given IP doesn't match this one
	 */
	boolean matches(IPAddress other);

	/**
	 * Returns string representation of IP address
	 * 
	 * @return {@link String} representation
	 */
	String toStringFormat();

	/**
	 * Returns number representation of IP address
	 * 
	 * @return {@link Number} representation
	 */
	Number toNumberFormat();

	/**
	 * Returns byte array representation of IP address
	 * 
	 * @return {@link Byte} array representation
	 */
	byte[] toByteFormat();

	/**
	 * Returns version of IP address
	 * 
	 * @return {@link Version}
	 */
	Version getVersion();
}
