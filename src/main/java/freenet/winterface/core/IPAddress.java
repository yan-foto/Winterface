package freenet.winterface.core;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A general representation of IPv4 and IPv6.
 * 
 * @author pausb
 * @see IPv4Address
 * @see IPv6Address
 */
public interface IPAddress {

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
	 * @throws UnknownHostException
	 */
	boolean matches(String other) throws UnknownHostException;

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
	 * Returns the wrapped {@link InetAddress}
	 * 
	 * @return wrapped address
	 */
	InetAddress getInetAddress();
	
	/**
	 * Returns number representation of IP address
	 * 
	 * @return {@link Number} representation
	 */
	Number toNumberFormat();

	/**
	 * Returns version of IP address
	 * 
	 * @return {@link Version}
	 */
	Version getVersion();
	
}
