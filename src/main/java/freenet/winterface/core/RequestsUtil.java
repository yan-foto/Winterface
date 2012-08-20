package freenet.winterface.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import org.apache.wicket.Application;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.http.WebRequest;

import freenet.crypt.RandomSource;
import freenet.crypt.SHA256;
import freenet.keys.FreenetURI;
import freenet.support.HexUtil;
import freenet.support.URLEncoder;
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
	 * A helper class to create links to fetch files
	 * 
	 * @see FreenetURI
	 */
	public class FreenetLink {
		/** Base URI (A {@link FreenetURI}) */
		public String base;
		/** Content type of file to fetch */
		public String mime;
		/** Maximum size of file to fetch */
		public long maxSize;
		/** If content type is to be forced */
		public String force;
		/** If download is to be forced */
		public boolean forceDownload;
		/** Maximum number of retries in case of fetch failure */
		public int maxRetries;

		/**
		 * Constructs
		 * 
		 * @param base
		 *            Base URI (A {@link FreenetURI})
		 * @param mime
		 *            Content type of file to fetch
		 * @param maxSize
		 *            Maximum size of file to fetch
		 * @param force
		 *            If content type is to be forced
		 * @param forceDownload
		 *            If download is to be forced
		 * @param maxRetries
		 *            Maximum number of retries in case of fetch failure
		 */
		public FreenetLink(String base, String mime, long maxSize, String force, boolean forceDownload, int maxRetries) {
			this.base = base;
			this.mime = mime;
			this.maxSize = maxSize;
			this.force = force;
			this.forceDownload = forceDownload;
			this.maxRetries = maxRetries;
		}

		/**
		 * Creates a link corresponding to given options
		 */
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("/");
			sb.append(base);
			char c = '?';
			if (mime != null && mime.length() != 0) {
				sb.append(c).append(PARAM_MIME).append("=").append(URLEncoder.encode(mime, false));
				c = '&';
			}
			if (maxSize > 0 && maxSize != RequestsUtil.MAX_LENGTH) {
				sb.append(c).append(PARAM_MAX_RETRIES).append("=").append(maxSize);
				c = '&';
			}
			if (force != null) {
				sb.append(c).append(PARAM_FORCE).append("=").append(force);
				c = '&';
			}
			if (forceDownload) {
				sb.append(c).append(PARAM_FORCE_DOWNLOAD).append("=").append("true");
				c = '&';
			}
			if (maxRetries >= -1) {
				sb.append(c).append(PARAM_MAX_RETRIES).append("=").append(maxRetries);
				c = '&';
			}
			return sb.toString();
		}
	}

	/**
	 * Creates a link with regard to given base {@link URI}, content type, and
	 * parameters of {@link WebRequest}
	 * 
	 * @param base
	 *            {@link FreenetURI} to fetch
	 * @param mime
	 *            content type
	 * @param request
	 *            to get parameters from
	 * @return created {@link FreenetLink}
	 */
	public FreenetLink createLink(String base, String mime, WebRequest request) {
		IRequestParameters params = request.getRequestParameters();
		boolean forceDownload = params.getParameterValue(PARAM_FORCE_DOWNLOAD).toBoolean(false);
		String headerMax = request.getHeader(HEADER_MAX_SIZE);
		long maxSize = headerMax != null ? Long.parseLong(headerMax) : MAX_LENGTH;
		String force = params.getParameterValue(PARAM_FORCE).toOptionalString();
		int maxRetries = params.getParameterValue(PARAM_MAX_RETRIES).toInt(-2);

		return new FreenetLink(base, mime, maxSize, force, forceDownload, maxRetries);
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
	public String getForceValue(String key, long time) {
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
