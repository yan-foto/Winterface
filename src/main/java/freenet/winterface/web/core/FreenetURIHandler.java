package freenet.winterface.web.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.string.StringValue;

import freenet.clients.http.FProxyFetchResult;
import freenet.keys.FreenetURI;
import freenet.support.api.Bucket;
import freenet.winterface.web.FreenetURIPage;

/**
 * An {@link IRequestHandler} to write data directly to {@link WebResponse} when
 * fetching of a {@link FreenetURI} has been successful.
 * <p>
 * Data is read from {@link FProxyFetchResult#data} as {@link InputStream} and
 * is written to {@link WebResponse}
 * </p>
 * 
 * @author pausb
 * @see FreenetURIPage
 */
public class FreenetURIHandler implements IRequestHandler {

	/** Result to write to response */
	private FProxyFetchResult result;

	/** Force download parameter */
	private final static String FORCE_DOWNLOAD_PARAM = "forcedownload";
	/** Log4j logger */
	private final static Logger logger = Logger.getLogger(FreenetURIHandler.class);

	/**
	 * Constructs.
	 * 
	 * @param result
	 *            {@link FProxyFetchResult} to write to response
	 */
	public FreenetURIHandler(FProxyFetchResult result) {
		Args.notNull(result, "FetchResult");
		this.result = result;
	}

	@Override
	public void respond(IRequestCycle requestCycle) {
		logger.trace("Starting to write data to response");
		// Get request to extract data from
		final WebRequest request = (WebRequest) requestCycle.getRequest();
		// Get response to use when responding with resource
		final WebResponse response = (WebResponse) requestCycle.getResponse();
		
		// Original URL
		String url = request.getUrl().canonical().toString();
		FreenetURI uri = null;
		try {
			uri = new FreenetURI(url);
		} catch (MalformedURLException e) {
			// This cannot possibly happen
			// FreenetURIPage.java checks everything first before passing data here
			logger.error("Cannot handle malformed URIs", e);
		}
		// Force download
		StringValue forceParam = request.getRequestParameters().getParameterValue(FORCE_DOWNLOAD_PARAM);
		if(!forceParam.isNull()) {
			response.addHeader("Content-Disposition", "attachment; filename=\"" + uri.getPreferredFilename() + '"');
			response.addHeader("Cache-Control", "private");
			response.addHeader("Content-Transfer-Encoding", "binary");
			// really the above should be enough, but ...
			// was application/x-msdownload, but some unix browsers offer to open that in Wine as default!
			// it is important that this type not be understandable, but application/octet-stream doesn't work.
			// see http://onjava.com/pub/a/onjava/excerpt/jebp_3/index3.html
			// Testing on FF3.5.1 shows that application/x-force-download wants to run it in wine,
			// whereas application/force-download wants to save it.
			response.setContentType("application/force-download");
			response.setStatus(HttpServletResponse.SC_OK);
		} else {
			// Set MIME-Type
			if (result.mimeType != null) {
				response.setContentType(result.mimeType);
			}
			
		}
		

		// Set Content length
		if (result.size != 0) {
			response.setContentLength(result.size);
		}
		Bucket data = result.getData();
		if (data != null) {
			try {
				InputStream is = data.getInputStream();
				OutputStream os = response.getOutputStream();
				IOUtils.copy(is, os);
				is.close();
			} catch (IOException e) {
				logger.error("Error while reading result data.", e);
			}
		} else {
			logger.error("Null result was sent for processing!");
		}
	}

	@Override
	public void detach(IRequestCycle requestCycle) {
		// Let FProxyFetchInProgress know that we are finished with result.
		result.close();
	}

}
