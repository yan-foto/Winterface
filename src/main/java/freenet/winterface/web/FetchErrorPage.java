package freenet.winterface.web;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;

import freenet.client.DefaultMIMETypes;
import freenet.client.FetchException;
import freenet.client.filter.UnsafeContentTypeException;
import freenet.clients.http.FProxyFetchResult;
import freenet.keys.FreenetURI;
import freenet.pluginmanager.PluginInfoWrapper;
import freenet.pluginmanager.PluginManager;
import freenet.support.HTMLEncoder;
import freenet.support.SizeUtil;
import freenet.winterface.core.Configuration;
import freenet.winterface.core.FreenetLink;
import freenet.winterface.core.RequestsUtil;

/**
 * A {@link WinterPage} in case any errors happens while fetching a
 * {@link FreenetURI}
 * 
 * @author pausb
 * @see FProxyFetchResult
 * @see FreenetURIPage
 */
@SuppressWarnings("serial")
public class FetchErrorPage extends WinterPage {

	/** Result to retrieve {@link FetchException} from */
	private transient FProxyFetchResult result;
	/** {@link FreenetURI} in {@link String} format which caused the error */
	private transient String path;

	// L10N
	private final static String SIZE_LABEL = "FProxyToadlet.sizeLabel";
	private final static String FINALIZED_MIME = "FProxyToadlet.mimeType";
	private final static String UNFINALIZED_MIME = "FProxyToadlet.expectedMimeType";
	private final static String UNKNOWN_MIME = "FProxyToadlet.unknownMIMEType";
	private final static String EXPL_TOO_BIG = "FProxyToadlet.largeFileExplanationAndOptions";
	private final static String EXPL_CANNOT_RETRIEVE = "FProxyToadlet.unableToRetrieve";
	private final static String FATAL_ERROR = "FProxyToadlet.errorIsFatal";
	private final static String OPEN_WITH_KEYEXPLORER = "FetchErrorPage.openWithKeyExplorer";
	private final static String OPEN_WITH_SITEEXPLORER = "FetchErrorPage.openWithSiteExplorer";
	private final static String OPEN_AS_TEXT = "FetchErrorPage.openAsText";
	private final static String DOWNLOAD_TO_DISK = "FetchErrorPage.openForceDisk";
	private final static String OPEN_FORCE = "FetchErrorPage.openForce";
	private final static String RETRY_NOW = "FProxyToadlet.retryNow";
	// Plugin IDs
	private final static String PLUGIN_KEYUTILS = "plugins.KeyUtils.KeyUtilsPlugin";
	private final static String PLUGIN_KEYEXPLORER = "plugins.KeyExplorer.KeyExplorer";
	private final static String PLUGIN_THAWBROWSER = "plugins.ThawIndexBrowser.ThawIndexBrowser";

	/** Log4j logger */
	private final static Logger logger = Logger.getLogger(FetchErrorPage.class);

	/**
	 * Constructs.
	 * 
	 * @param result
	 *            containing the {@link FetchException}
	 * @param path
	 *            {@link FreenetURI} which caused the error
	 */
	public FetchErrorPage(FProxyFetchResult result, String path) {
		super();
		this.result = result;
		this.path = path;
		logger.error("Rendering failed for URI "+path, result.failed);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		addFileInfo();
		boolean hasFilterErrors = addErrorDetails();
		addRelatedOptions(hasFilterErrors);
		FetchException error = result.failed;
		String explanationKey = null;
		switch (error.mode) {
		case FetchException.TOO_BIG:
			// TODO add always download too big button
			explanationKey = EXPL_TOO_BIG;
			break;
		default:
			explanationKey = EXPL_CANNOT_RETRIEVE;
			break;
		}
		// Add explanation
		String explanationValue = localize(explanationKey);
		Label explanation = new Label("explanation", Model.of(explanationValue));
		add(explanation);
	}

	/**
	 * Adds file name (as a link for refetch), size and MIME type.
	 */
	private void addFileInfo() {
		final FetchException error = result.failed;
		StringBuffer buffer;
		// Link to file
		ExternalLink fileLink = new ExternalLink("fileLink", "/" + path);
		fileLink.setBody(Model.of(getFileName()));
		add(fileLink);
		// Size
		String sizeLabel = localize(SIZE_LABEL);
		buffer = new StringBuffer(sizeLabel);
		buffer.append(SizeUtil.formatSize(error.expectedSize));
		if (!error.finalizedSize()) {
			String mayChange = localize("FProxyToadlet.mayChange");
			buffer.append(mayChange);
		}
		Label size = new Label("fileSize", Model.of(buffer.toString()));
		add(size);
		// MIME-Type
		buffer = new StringBuffer();
		String value = null;
		if (error.getExpectedMimeType() != null) {
			String key = error.finalizedSize() ? FINALIZED_MIME : UNFINALIZED_MIME;
			Map<String, String> replacement = new HashMap<String, String>();
			replacement.put("mime", error.getExpectedMimeType());
			value = localize(key, Model.ofMap(replacement));
		} else {
			value = localize(UNKNOWN_MIME);
		}
		Label mime = new Label("fileMime", Model.of(value));
		add(mime);
	}

	/**
	 * Adds filter exception details (if any), shows if error is fatal, writes
	 * the error code (if available)
	 * 
	 * @return {@code true} if error was causef by filter exception
	 * @see UnsafeContentTypeException
	 * @see FetchException#errorCodes
	 * @see FetchException#isFatal()
	 */
	private boolean addErrorDetails() {
		boolean causedByFilter = false;
		UnsafeContentTypeException filterException = null;
		FetchException error = result.failed;
		if (error.getCause() instanceof UnsafeContentTypeException) {
			filterException = (UnsafeContentTypeException) error.getCause();
			causedByFilter = true;
		}
		Label fatalError = new Label("fatalError");
		WebMarkupContainer filterErrorContainer = new WebMarkupContainer("filterErrorContainer");
		if (filterException == null) {
			// Hide filter exception details container
			filterErrorContainer.setVisible(false);
			// Fatal error
			boolean isFatal = error.isFatal();
			if (isFatal) {
				String fatalValue = localize(FATAL_ERROR);
				fatalError.setDefaultModel(Model.of(fatalValue));
			}
			// Hide element if error is not fatal
			fatalError.setVisible(isFatal);
		} else {
			// Detailed list of filter exception
			List<String> details = filterException.details();
			ListView<String> filterError = new ListView<String>("filterError", details) {
				@Override
				protected void populateItem(ListItem<String> item) {
					Label detail = new Label("detail", Model.of(item.getModelObject()));
					item.add(detail);
				}
			};
			filterErrorContainer.add(filterError);
		}
		add(fatalError, filterErrorContainer);
		// Add error codes
		Label errorCode = new Label("errorCode");
		if (error.errorCodes != null) {
			errorCode.setDefaultModel(Model.of(error.errorCodes.toVerboseString()));
		} else {
			// No error code -> hide the element
			errorCode.setVisible(false);
		}
		add(errorCode);

		return causedByFilter;
	}

	/**
	 * Add a list of options, depending on type of error, {@link Configuration},
	 * and available plugins
	 * 
	 * @param causedByFilter
	 *            if error is caused by a filter exception
	 */
	public void addRelatedOptions(boolean causedByFilter) {
		PluginInfoWrapper p;
		FetchException error = result.failed;
		final WebRequest request = (WebRequest) getRequest();
		final String mime = error.getExpectedMimeType();
		PluginManager pm = getFreenetNode().pluginManager;
		Map<String, String> options = new HashMap<String, String>();
		// Dig in plugins
		// TODO make this more dynamic. Plugins should add themselves hier
		if ((error.mode == FetchException.NOT_IN_ARCHIVE || error.mode == FetchException.NOT_ENOUGH_PATH_COMPONENTS)) {
			// first look for the newest version
			if ((p = pm.getPluginInfo(PLUGIN_KEYUTILS)) != null) {
				logger.trace("Key Utils found: Adding option");
				if (p.getPluginLongVersion() < 5010) {
					options.put("/KeyUtils/?automf=true&key=" + path, localize(OPEN_WITH_KEYEXPLORER));
				} else {
					options.put("/KeyUtils/?key=" + path, localize(OPEN_WITH_KEYEXPLORER));
					options.put("/KeyUtils/Site?key=" + path, localize(OPEN_WITH_SITEEXPLORER));
				}
			} else if ((p = pm.getPluginInfo(PLUGIN_KEYEXPLORER)) != null) {
				logger.trace("Key Explorer found: Adding option");
				if (p.getPluginLongVersion() > 4999) {
					options.put("/KeyExplorer/?automf=true&key=" + path, localize(OPEN_WITH_KEYEXPLORER));
				} else {
					options.put("/plugins/plugins.KeyExplorer.KeyExplorer/?key=" + path, localize(OPEN_WITH_KEYEXPLORER));
				}
			}
		}
		FreenetLink textLink;
		WebMarkupContainer optionsContainer = new WebMarkupContainer("optionsContainer");
		if (causedByFilter) {
			logger.trace("Error caused by filter expection. Adding options");
			if (mime.equals("application/x-freenet-index") && pm.getPluginInfo(PLUGIN_THAWBROWSER) != null) {
				logger.trace("Thaw browser found: Adding option");
				options.put("/plugins/plugins.ThawIndexBrowser.ThawIndexBrowser/?key=" + path, localize(PLUGIN_THAWBROWSER));
			}
			// Option to open as text
			textLink = FreenetLink.createLink(path, mime, request);
			options.put(textLink.toString(), localize(OPEN_AS_TEXT));
			// Force download
			textLink = FreenetLink.createLink(path, mime, request);
			textLink.forceDownload = true;
			options.put(textLink.toString(), localize(DOWNLOAD_TO_DISK));
			// Force open as expected mime
			if (!(mime.equals("application/octet-stream") || mime.equals("application/x-msdownload"))) {
				Map<String, String> substitution = new HashMap<String, String>();
				substitution.put("mime", HTMLEncoder.encode(mime));
				textLink = FreenetLink.createLink(path, mime, request);
				textLink.force = RequestsUtil.getForceValue(path, System.currentTimeMillis());
				options.put(textLink.toString(), localize(OPEN_FORCE, Model.ofMap(substitution)));
			}
		}
		// Retry link
		if ((!error.isFatal() || causedByFilter) && (!getConfiguration().isPublicGateway() || isAllowedFullAccess())) {
			logger.trace("Adding retry option");
			textLink = FreenetLink.createLink(path, mime, request);
			options.put(textLink.toString(), localize(RETRY_NOW));
		}
		RepeatingView pluginsOptions = new RepeatingView("pluginsOptions");
		logger.trace(String.format("Adding a total sum of %d options", options.size()));
		for (Entry<String, String> item : options.entrySet()) {
			WebMarkupContainer liContainer = new WebMarkupContainer(pluginsOptions.newChildId());
			ExternalLink link = new ExternalLink("pluginOption", item.getKey());
			link.setBody(Model.of(item.getValue()));
			liContainer.add(link);
			pluginsOptions.add(liContainer);
		}
		optionsContainer.add(pluginsOptions);
		optionsContainer.setVisible(!options.isEmpty());
		add(optionsContainer);
	}

	/**
	 * Gets the file name with regard to its expected MIME type
	 * 
	 * @return calculated file name
	 */
	private String getFileName() {
		FreenetURI uri = null;
		String s = "";
		String expectedMimeType = result.failed.getExpectedMimeType();
		try {
			uri = new FreenetURI(path);
			s = uri.getPreferredFilename();
			int dotIdx = s.lastIndexOf('.');
			String ext = DefaultMIMETypes.getExtension(expectedMimeType);
			if (ext == null)
				ext = "bin";
			if ((dotIdx == -1) && (expectedMimeType != null)) {
				s += '.' + ext;
				return s;
			}
			if (dotIdx != -1) {
				String oldExt = s.substring(dotIdx + 1);
				if (DefaultMIMETypes.isValidExt(expectedMimeType, oldExt))
					return s;
				return s + '.' + ext;
			}
			s += '.' + ext;
		} catch (MalformedURLException e) {
			// Cannot happen
		}
		return s;
	}

	@Override
	protected void onDetach() {
		super.onDetach();
		result.close();
	}
	
	@Override
	protected void configureResponse(WebResponse response) {
		super.configureResponse(response);
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}

}
