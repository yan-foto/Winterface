package freenet.winterface.core;

import java.net.InetAddress;
import java.net.UnknownHostException;

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

	/** Subnet (in decimal) */
	private final int subnet;
	/** Subnet mask */
	private final long subnetMask;
	/** {@link InetAddress} being wrapped */
	private final InetAddress inetAddress;
	/** Number format */
	private final long numberFormat;

	/**
	 * Constructs.
	 * 
	 * @param addr
	 *            any host in a format supported by
	 *            {@link InetAddress#getByName(String)}
	 * @throws UnknownHostException
	 */
	public IPv4Address(String addr) throws UnknownHostException {
		int maskIndex;
		String host;
		if ((maskIndex = addr.indexOf(MASK_CHAR)) > -1) {
			subnet = Integer.parseInt(addr.substring(maskIndex + 1));
			host = addr.substring(0, maskIndex);
		} else {
			subnet = 0;
			host = addr;
		}

		inetAddress = InetAddress.getByName(host);
		numberFormat = toLong(inetAddress.getAddress());
		int hostBits = 32 - subnet;
		subnetMask = (FULL_MASK >> hostBits) << hostBits;
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
		for (int i = 0; i < 4; i++) {
			long unsignedByte = bytes[i] & BYTE_MASK;
			result += (unsignedByte << 8 * (3 - i));
		}
		return result;
	}

	@Override
	public boolean matches(String other) throws UnknownHostException {
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
		long otherNumber = (Long) ((IPv4Address) other).toNumberFormat();
		if (subnet == 0) {
			return otherNumber == numberFormat;
		}
		return (otherNumber & subnetMask) == (numberFormat & subnetMask);
	}

	@Override
	public InetAddress getInetAddress() {
		return inetAddress;
	}

	@Override
	public Number toNumberFormat() {
		return numberFormat;
	}

	@Override
	public Version getVersion() {
		return Version.IPv4;
	}

}
