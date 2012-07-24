package freenet.winterface.core;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.regex.Matcher;

import freenet.support.HexUtil;

/**
 * An immutable presentation of IPv6.
 * <p>
 * This class supports presentation of IPs in CIDR format and is capable of
 * subnet matching. <br />
 * Note that this class <i>does not</i> support abbreviated version of addresses
 * with merged consecutive zero sections:
 * <ul>
 * <li>Supported: {@code 0000:0000:0000:0000:0000:0000:0000:0001}</li>
 * <li>Supported: {@code 0:0:0:0:0:0:0:1}</li>
 * <li>Not supported: {@code ::1}</li>
 * </ul>
 * </p>
 * 
 * @author pausb
 * @see IPAddress
 */
public class IPv6Address implements IPAddress {

	/** A big integer having all bits equal to 1 */
	private final static BigInteger FULL_MASK;

	/** String representation (without subnet) */
	private final String stringFormat;
	/** {@link Number} representation (long) */
	private final BigInteger numberFormat;
	/** Byte array representation */
	private final byte[] byteFormat;
	/** Subnet (in decimal) */
	private final int subnet;
	/** Subnet mask */
	private final BigInteger subnetMask;
	/** Matcher to access various parts of string representation */
	private final Matcher matcher;

	static {
		// Initialize an array of byte with all entries set to 1
		byte[] mask_temp = new byte[128];
		Arrays.fill(mask_temp, Byte.MIN_VALUE);
		FULL_MASK = new BigInteger(mask_temp);
	}

	public IPv6Address(String addr) {
		matcher = initMatcher(addr);
		byteFormat = toByte(addr);
		subnet = ("".equals(matcher.group(9))) ? 0 : Integer.parseInt(matcher.group(9).substring(1));
		int hostBits = 128 - subnet;
		subnetMask = FULL_MASK.shiftRight(hostBits).shiftLeft(hostBits);
		numberFormat = new BigInteger(byteFormat);
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
	 *            IPv6 adress
	 * @return generated {@link Matcher}
	 * @see #PATTERN;
	 */
	private Matcher initMatcher(String addr) {
		Matcher matcher = IPV6_PATTERN.matcher(addr);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid IPv6 format.");
		}
		return matcher;
	}

	/**
	 * Turns a given IPv6 address to its byte array representation.
	 * <p>
	 * <strong>NOTE: Array is in big-endian format: highest byte at index
	 * 0</strong>
	 * </p>
	 * 
	 * @param addr
	 *            IP address
	 * @return corresponding byte[] representation
	 */
	private byte[] toByte(String addr) {
		byte[] result = new byte[16];
		for (int i = 0; i < 16; i += 2) {
			String group = matcher.group((i >> 1) + 1);
			group = padSection(group);
			byte[] tmp = HexUtil.hexToBytes(group);
			result[i] = tmp[0];
			result[i + 1] = tmp[1];
		}
		return result;
	}

	/**
	 * Puts zeros in front of sections with leading zeroes removed:
	 * <p>
	 * {@code [:]02f[:]} -> {@code [:]002f[:]}
	 * <p>
	 * 
	 * @param section
	 *            section to pad
	 * @return padded section
	 */
	private String padSection(String section) {
		for (int i = 0; i < 4 - section.length(); i++) {
			section = "0" + section;
		}
		return section;
	}

	@Override
	public boolean matches(String other) {
		if (other.contains(MASK_CHAR)) {
			throw new UnsupportedOperationException("No CIDR format is supported for match operation");
		}
		IPv6Address otherIP = new IPv6Address(other);
		return matches(otherIP);
	}

	@Override
	public boolean matches(IPAddress other) {
		if (!getVersion().equals(other.getVersion())) {
			throw new IllegalArgumentException("IP versions doesn't match");
		}
		BigInteger otherBigInt = (BigInteger) ((IPv6Address) other).numberFormat;
		if (subnet == 0) {
			return numberFormat.equals(otherBigInt);
		}
		BigInteger otherAndSubMask = otherBigInt.add(subnetMask);
		BigInteger thisAndSubMask = numberFormat.and(subnetMask);
		return (thisAndSubMask.equals(otherAndSubMask));
	}

	@Override
	public String toStringFormat() {
		return stringFormat;
	}

	@Override
	public Number toNumberFormat() {
		return numberFormat;
	}

	@Override
	public byte[] toByteFormat() {
		return byteFormat;
	}

	@Override
	public Version getVersion() {
		return Version.IPv6;
	}

}
