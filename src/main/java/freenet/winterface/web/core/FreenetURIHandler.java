package freenet.winterface.web.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.lang.Args;

import freenet.client.FetchException;
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
		// Get response to use when responding with resource
		final WebResponse response = (WebResponse) requestCycle.getResponse();

		// Set MIME-Type
		if (result.mimeType != null) {
			response.setContentType(result.mimeType);
		}
		// Set Content length
		if (result.size != 0) {
			response.setContentLength(result.size);
		}
		Bucket data = result.getData();
		FetchException fe = result.failed;
		if (data != null) {
			try {
				InputStream is = data.getInputStream();
				OutputStream os = response.getOutputStream();
				IOUtils.copy(is, os);
				is.close();
			} catch (IOException e) {
				logger.error("Error while reading result data.", e);
			}
		} else if (fe.newURI != null) {
			// A newer version is available, so just send a redirect
			PageParameters params = new PageParameters();
			params.set(0, fe.newURI);
			logger.debug("Newer version of URI found. redirecting...");
			throw new RestartResponseException(FreenetURIPage.class, params);
		} else {

		}
	}

	@Override
	public void detach(IRequestCycle requestCycle) {
		// Let FProxyFetchInProgress know that we are finished with result.
		result.close();
	}

}
