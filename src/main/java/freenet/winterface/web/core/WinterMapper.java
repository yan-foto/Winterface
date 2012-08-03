package freenet.winterface.web.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.wicket.Page;
import org.apache.wicket.core.request.mapper.AbstractBookmarkableMapper;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.IRequestMapper;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.info.PageComponentInfo;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.mapper.parameter.PageParametersEncoder;

/**
 * Responsible to map arbitrary {@link URL}s to desired {@link Page}s.
 * <p>
 * You can use a desired mapping using e.g.
 * {@link WinterMapper#registerMapping(String, Class)} to map all {@link URL}s
 * starting with given {@link String} to a desired {@link Page}.
 * <p>
 * 
 * @author pausb
 * @see AbstractBookmarkableMapper
 * 
 */
public class WinterMapper extends AbstractBookmarkableMapper {

	/**
	 * Fallback {@link IRequestMapper}. In case there is no defined mapping for
	 * given {@link URL}, the fallback mapper is activated
	 */
	private IRequestMapper delegate;

	/** Contains mappings */
	private HashMap<String, Class<? extends IRequestablePage>> mappings;

	/** File to read static mappings from */
	private static final String MAPPINGS_FILE = "mappings.properties";
	/** Log4j Logger */
	private static final Logger logger = Logger.getLogger(WinterMapper.class);

	/**
	 * Constructs.
	 * <p>
	 * This is set {@code private} to make {@link WinterMapper} a singleton.
	 * </p>
	 * 
	 * @param delegate
	 *            fallback {@link IRequestMapper}
	 */
	public WinterMapper(IRequestMapper delegate) {
		this.delegate = delegate;
		mappings = new HashMap<String, Class<? extends IRequestablePage>>();
		loadMappings();
	}

	/**
	 * Register a new mapping which maps all {@link URL} starting with given
	 * {@link String} to desired {@link Page}
	 * 
	 * @param startsWith
	 *            {@link URL} start
	 * @param pageClass
	 *            {@link Page} to forward to
	 */
	public void registerMapping(String startsWith, Class<? extends IRequestablePage> pageClass) {
		mappings.put(startsWith, pageClass);
	}

	@Override
	public int getCompatibilityScore(Request request) {
		return delegate.getCompatibilityScore(request);
	}

	@Override
	public IRequestHandler mapRequest(Request request) {
		if (urlDesired(request.getClientUrl()) != null) {
			return super.mapRequest(request);
		}
		return delegate.mapRequest(request);
	}

	@Override
	public Url mapHandler(IRequestHandler requestHandler) {
		Url url = super.mapHandler(requestHandler);
		if (urlDesired(url) != null) {
			return url;
		}
		return delegate.mapHandler(requestHandler);
	}

	@Override
	protected UrlInfo parseRequest(Request request) {
		Url url = request.getUrl();
		Class<? extends IRequestablePage> desired = urlDesired(url);
		if (desired != null) {
			PageComponentInfo info = getPageComponentInfo(url);
			PageParameters pageParameters = extractPageParameters(request, 0, new PageParametersEncoder());
			return new UrlInfo(info, desired, pageParameters);
		}
		return null;
	}

	@Override
	protected Url buildUrl(UrlInfo info) {
		Url url = new Url();
		url.getSegments().add(getContext().getNamespace());
		url.getSegments().add(getContext().getBookmarkableIdentifier());
		url.getSegments().add(info.getPageClass().getName());

		encodePageComponentInfo(url, info.getPageComponentInfo());

		return encodePageParameters(url, info.getPageParameters(), new PageParametersEncoder());
	}

	@Override
	protected boolean pageMustHaveBeenCreatedBookmarkable() {
		return false;
	}

	/**
	 * Checks if {@link WinterMapper} is responsible for the given {@link URL}
	 * 
	 * @param url
	 *            {@link URL} to check
	 * @return {@code true} if {@link URL}belongs to this mapper
	 */
	private Class<? extends IRequestablePage> urlDesired(Url url) {
		if (url != null) {
			String addr = url.canonical().toString();
			for (String pattern : mappings.keySet()) {
				if (addr.startsWith(pattern)) {
					return mappings.get(pattern);
				}
			}
		}
		return null;
	}

	/**
	 * Loads static mappings from disk
	 * 
	 * @see #MAPPINGS_FILE
	 */
	private void loadMappings() {
		Properties mappings = new Properties();
		InputStream mappingStream = getClass().getClassLoader().getResourceAsStream(MAPPINGS_FILE);
		try {
			logger.debug("Trying to read external mappings from " + MAPPINGS_FILE);
			mappings.load(mappingStream);
			for (Entry<Object, Object> entry : mappings.entrySet()) {
				String startsWith = (String) entry.getKey();
				String className = (String) entry.getValue();
				Class<? extends IRequestablePage> clazz = Class.forName(className).asSubclass(IRequestablePage.class);
				registerMapping(startsWith, clazz);
				logger.debug(String.format("Urls starting with %s will be mapped to %s", startsWith, className));
			}
		} catch (FileNotFoundException e) {
			logger.debug("No external mapping file found!");
		} catch (IOException e) {
			logger.debug("Error while reading external mapping file.", e);
		} catch (ClassNotFoundException e) {
			logger.debug("No such class for mapping is not available", e);
		}
	}

}
