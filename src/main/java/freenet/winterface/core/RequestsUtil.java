package freenet.winterface.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.wicket.Application;

import freenet.crypt.RandomSource;
import freenet.crypt.SHA256;
import freenet.keys.FreenetURI;
import freenet.support.HexUtil;
import freenet.winterface.web.core.WinterfaceApplication;

/**
 * A Util class for requests being sent from browser to Winterface.
 * 
 * @author pausb
 */
public final class RequestsUtil {

	/** Parameter key for max retries */
	public final static String PARAM_MAX_RETRIES = "max-retries";
	/** Parameter to force content type */
	public final static String PARAM_FORCE = "force";
	/** Parameter denoting to force download of content */
	public final static String PARAM_FORCE_DOWNLOAD = "forcedownload";
	/** Parameter denoting MIME type */
	public final static String PARAM_MIME = "type";
	/** Header key for max size */
	public final static String HEADER_MAX_SIZE = "max-size";
	/** Header key for accepted MIME types */
	public final static String HEADER_ACCEPT = "accept";

	/** Number of maximum redirect follows */
	public final static short MAX_RECURSION = 5;
	// ?force= links become invalid after 2 hours.
	public static final long FORCE_GRAIN_INTERVAL = 60 * 60 * 1000;
	/** Maximum size for transparent pass-through, should be a config option */
	// 2MB plus a bit due to buggy inserts
	public static long MAX_LENGTH = (2 * 1024 * 1024 * 11) / 10;

	/**
	 * Avoid instantiation
	 */
	private RequestsUtil() {
		// nothing!
	}

	/**
	 * Creates force value for given key
	 * 
	 * @param key
	 *            a {@link FreenetURI}
	 * @param time
	 *            current time
	 * @return generated force value
	 */
	public static String getForceValue(String key, long time) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] random = new byte[32];
		RandomSource randomSource = ((WinterfaceApplication) Application.get()).getFreenetWrapper().getNode().clientCore.random;
		randomSource.nextBytes(random);

		try {
			bos.write(random);
			bos.write(key.getBytes("UTF-8"));
			bos.write(Long.toString(time / FORCE_GRAIN_INTERVAL).getBytes("UTF-8"));
		} catch (IOException e) {
			throw new Error(e);
		}

		String f = HexUtil.bytesToHex(SHA256.digest(bos.toByteArray()));
		return f;
	}

}
