package freenet.winterface.web.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.lang.Args;

import freenet.clients.http.FProxyFetchResult;
import freenet.keys.FreenetURI;
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

		try {
			InputStream is = result.getData().getInputStream();
			OutputStream os = response.getOutputStream();
			IOUtils.copy(is, os);
			is.close();
		} catch (IOException e) {
			logger.error("Error while reading result data.", e);
		} catch (NullPointerException e) {
			// XXX How can this even happen!?
			logger.error("Result's data is null!",e);
		} 
	}

	@Override
	public void detach(IRequestCycle requestCycle) {
		// Let FProxyFetchInProgress know that we are finished with result.
		result.close();
	}

}
