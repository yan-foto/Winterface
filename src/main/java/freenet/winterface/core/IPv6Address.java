package freenet.winterface.core;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * An immutable presentation of IPv6.
 * <p>
 * This class supports presentation of IPs in CIDR format and is capable of
 * subnet matching. <br />
 * </p>
 * 
 * @author pausb
 * @see IPAddress
 */
public class IPv6Address implements IPAddress {

	/** A big integer having all bits equal to 1 */
	private final static BigInteger FULL_MASK;

	/** Subnet (in decimal) */
	private final int subnet;
	/** Subnet mask */
	private final BigInteger subnetMask;
	/** {@link InetAddress} being wrapped */
	private final InetAddress inetAddress;
	/** Number format */
	private final BigInteger numberFormat;

	static {
		// Initialize an array of byte with all entries set to 1
		byte[] mask_temp = new byte[128];
		Arrays.fill(mask_temp, Byte.MIN_VALUE);
		FULL_MASK = new BigInteger(mask_temp);
	}

	/**
	 * Constructs.
	 * 
	 * @param addr
	 *            any host in a format supported by
	 *            {@link InetAddress#getByName(String)}
	 * @throws UnknownHostException
	 */
	public IPv6Address(String addr) throws UnknownHostException {
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
		numberFormat = new BigInteger(inetAddress.getAddress());
		int hostBits = 128 - subnet;
		subnetMask = FULL_MASK.shiftRight(hostBits).shiftLeft(hostBits);
	}

	@Override
	public boolean matches(String other) throws UnknownHostException {
		if (other.contains(MASK_CHAR)) {
			throw new UnsupportedOperationException("No CIDR format is supported for match operation");
		}
		IPv6Address otherIP = new IPv6Address(other);
		return matches(otherIP);
	}

	@Override
	public boolean matches(IPAddress other) {
		if (!getVersion().equals(other.getVersion())) {
			throw new IllegalArgumentException("IP versions do not match");
		}
		BigInteger otherNumber = (BigInteger) other.toNumberFormat();
		if (subnet == 0) {
			return numberFormat.equals(otherNumber);
		}
		BigInteger otherAndSubMask = otherNumber.add(subnetMask);
		BigInteger thisAndSubMask = otherNumber.and(subnetMask);
		return (thisAndSubMask.equals(otherAndSubMask));
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
		return Version.IPv6;
	}
	
	public static void main(String[] args) {
		try {
			IPUtils.matches("127.0.0.1", "0:0:0:0:0:0:0:1");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

}
