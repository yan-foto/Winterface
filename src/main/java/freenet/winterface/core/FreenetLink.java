package freenet.winterface.core;

import java.net.URI;

import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.http.WebRequest;

import freenet.keys.FreenetURI;
import freenet.support.URLEncoder;

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
	public static FreenetLink createLink(String base, String mime, WebRequest request) {
		IRequestParameters params = request.getRequestParameters();
		boolean forceDownload = params.getParameterValue(RequestsUtil.PARAM_FORCE_DOWNLOAD).toBoolean(false);
		String headerMax = request.getHeader(RequestsUtil.HEADER_MAX_SIZE);
		long maxSize = headerMax != null ? Long.parseLong(headerMax) : RequestsUtil.MAX_LENGTH;
		String force = params.getParameterValue(RequestsUtil.PARAM_FORCE).toOptionalString();
		int maxRetries = params.getParameterValue(RequestsUtil.PARAM_MAX_RETRIES).toInt(-2);

		return new FreenetLink(base, mime, maxSize, force, forceDownload, maxRetries);
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
			sb.append(c).append(RequestsUtil.PARAM_MIME).append("=").append(URLEncoder.encode(mime, false));
			c = '&';
		}
		if (maxSize > 0 && maxSize != RequestsUtil.MAX_LENGTH) {
			sb.append(c).append(RequestsUtil.PARAM_MAX_RETRIES).append("=").append(maxSize);
			c = '&';
		}
		if (force != null) {
			sb.append(c).append(RequestsUtil.PARAM_FORCE).append("=").append(force);
			c = '&';
		}
		if (forceDownload) {
			sb.append(c).append(RequestsUtil.PARAM_FORCE_DOWNLOAD).append("=").append("true");
			c = '&';
		}
		if (maxRetries >= -1) {
			sb.append(c).append(RequestsUtil.PARAM_MAX_RETRIES).append("=").append(maxRetries);
			c = '&';
		}
		return sb.toString();
	}
}
