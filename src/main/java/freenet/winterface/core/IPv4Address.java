package freenet.winterface.core;

import java.util.regex.Matcher;

/**
 * An immutable presentation of IPv4.
 * <p>
 * This class supports presentation of IPs in CIDR format and is capable of
 * subnet matching.
 * </p>
 * 
 * @author pausb
 * @see IPAddress
 */
public class IPv4Address implements IPAddress {

	/** A long number having all bits equal to 1 */
	private final static long FULL_MASK = 0xffffffffL;

	/** String representation (without subnet) */
	private final String stringFormat;
	/** {@link Number} representation (long) */
	private final long longFormat;
	/** Byte array representation */
	private final byte[] byteFormat;
	/** Subnet (in decimal) */
	private final int subnet;
	/** Subnet mask */
	private final long subnetMask;
	/** Matcher to access various parts of string representation */
	private final Matcher matcher;

	/**
	 * Constructs.
	 * 
	 * @param addr
	 *            can be an IPv4 address in d.d.d.d format or in CIDR format
	 *            d.d.d.d/d
	 */
	public IPv4Address(String addr) {
		matcher = initMatcher(addr);
		byteFormat = toByte(addr);
		subnet = ("".equals(matcher.group(5))) ? 0 : Integer.parseInt(matcher.group(5).substring(1));
		int hostBits = 32 - subnet;
		subnetMask = (FULL_MASK >> hostBits) << hostBits;
		longFormat = toLong(byteFormat);
		if (addr.indexOf(MASK_CHAR) != -1) {
			stringFormat = addr.substring(0, addr.indexOf(MASK_CHAR));
		} else {
			stringFormat = addr;
		}
	}

	/**
	 * Initializes a {@link Matcher} to be used by other methods
	 * 
	 * @param addr
	 *            IPv4 adress
	 * @return generated {@link Matcher}
	 * @see #PATTERN;
	 */
	private Matcher initMatcher(String addr) {
		Matcher matcher = IPV4_PATTERN.matcher(addr);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid IPv4 format.");
		}
		return matcher;
	}

	/**
	 * Turns a given IPv4 address to its byte array representation.
	 * <p>
	 * <strong>NOTE: Array is in little-endian format: smallest byte at index
	 * 0</strong>
	 * </p>
	 * 
	 * @param addr
	 *            IP address
	 * @return corresponding byte[] representation
	 */
	private byte[] toByte(String addr) {
		byte[] result = new byte[4];
		for (int i = 0; i < 4; i++) {
			result[i] = (byte) (Integer.parseInt(matcher.group(4 - i)) & BYTE_MASK);
		}
		return result;
	}

	/**
	 * Turns a given IPv4 address in byte[] format to its long representation.
	 * 
	 * @param bytes
	 *            IP address in byte[]
	 * @return corresponding long representation
	 */
	private long toLong(byte[] bytes) {
		long result = 0;
		if (bytes.length != 4) {
			throw new NumberFormatException("Array length does not match. (Must be 4)");
		}
		for (int i = 3; i >= 0; i--) {
			long unsignedByte = bytes[i] & BYTE_MASK;
			result += (unsignedByte << 8 * (i));
		}
		return result;
	}

	@Override
	public boolean matches(String other) {
		if (other.contains(MASK_CHAR)) {
			throw new UnsupportedOperationException("No CIDR format is supported for match operation");
		}
		IPv4Address otherIP = new IPv4Address(other);
		return matches(otherIP);
	}

	@Override
	public boolean matches(IPAddress other) {
		if (!getVersion().equals(other.getVersion())) {
			throw new IllegalArgumentException("IP versions doesn't match");
		}
		long otherLong = (Long) ((IPv4Address)other).toNumberFormat();
		if (subnet == 0) {
			return otherLong == longFormat;
		}
		return (otherLong & subnetMask) == (longFormat & subnetMask);
	}

	@Override
	public String toStringFormat() {
		return stringFormat + MASK_CHAR + subnet;
	}

	@Override
	public Number toNumberFormat() {
		return longFormat;
	}

	@Override
	public byte[] toByteFormat() {
		return byteFormat;
	}

	@Override
	public Version getVersion() {
		return Version.IPv4;
	}

}
