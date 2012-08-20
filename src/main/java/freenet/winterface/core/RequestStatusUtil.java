package freenet.winterface.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wicket.Localizer;
import org.apache.wicket.model.Model;

import freenet.keys.FreenetURI;
import freenet.node.fcp.ClientPut.COMPRESS_STATE;
import freenet.node.fcp.DownloadRequestStatus;
import freenet.node.fcp.RequestStatus;
import freenet.node.fcp.UploadDirRequestStatus;
import freenet.node.fcp.UploadFileRequestStatus;
import freenet.node.fcp.UploadRequestStatus;
import freenet.support.SizeUtil;
import freenet.support.TimeUtil;
import freenet.winterface.web.core.RequestStatusView;

/**
 * A Util class to calculate and localize different properties of a
 * {@link RequestStatus}
 * <p>
 * Depending on type of {@link RequestStatus} ({@link DownloadRequestStatus},
 * {@link UploadRequestStatus} or {@link UploadDirRequestStatus}), the
 * properties are calculated.
 * </p>
 * 
 * @author pausb
 * @see RequestProgress
 * @see RequestStatusView
 * @see QueueUtil
 */
public class RequestStatusUtil {

	/** Flag denoting no content type is available or can be calculated */
	public final static String FLAG_NO_MIME = "NO_MIME";

	// L10N
	private final static String L10N_PERSISTENCE_FOREVER = "QueueToadlet.persistenceForever";
	private final static String L10N_PERSISTENCE_NONE = "QueueToadlet.persistenceNone";
	private final static String L10N_PERSISTENCE_REBOOT = "QueueToadlet.persistenceReboot";
	private final static String L10N_NONE = "QueueToadlet.none";
	private final static String L10N_UNKNOWN = "QueueToadlet.unknown";
	private final static String L10N_UNKNOW_LA = "QueueToadlet.lastActivity.unknown";
	private final static String L10N_AGO_LA = "QueueToadlet.lastActivity.ago";
	private final static String L10N_PRIO_PREFIX = "QueueToadlet.priority";

	/** Log4j logger */
	private final static Logger logger = Logger.getLogger(RequestsUtil.class);

	/**
	 * @param req
	 *            desired {@link RequestStatus}
	 * @return localized {@link String} corresponding to priority
	 */
	public static String getPriority(RequestStatus req) {
		String result = Localizer.get().getString(L10N_PRIO_PREFIX + req.getPriority(), null);
		logger.trace(String.format("Priority for RequestStatus (%s) : %s", req.hashCode(), result));
		return result;
	}

	/**
	 * @param req
	 *            desired {@link RequestStatus}
	 * @return size of {@link RequestStatus} calculated using
	 *         {@link SizeUtil#formatSize(long)} or -1 if unknown
	 * @see SizeUtil
	 */
	public static long getSize(RequestStatus req) {
		long result;
		if (req instanceof DownloadRequestStatus) {
			result = ((DownloadRequestStatus) req).getDataSize();
		} else if (req instanceof UploadRequestStatus) {
			result = ((UploadRequestStatus) req).getDataSize();
		} else {
			return -1;
		}
		logger.trace(String.format("Size for RequestStatus (%s) : %s", req.hashCode(), result));
		return result;
	}

	/**
	 * @param req
	 *            desired {@link RequestStatus}
	 * @return content type of request of {@link #FLAG_NO_MIME} flag if it
	 *         cannot be calculated
	 */
	public static String getMIME(RequestStatus req) {
		String result;
		if (req instanceof DownloadRequestStatus) {
			result = ((DownloadRequestStatus) req).getMIMEType();
		} else if (req instanceof UploadFileRequestStatus) {
			result = ((UploadFileRequestStatus) req).getMIMEType();
		} else {
			result = FLAG_NO_MIME;
		}
		logger.trace(String.format("MIME for RequestStatus (%s) : %s", req.hashCode(), result));
		return result;
	}

	/**
	 * @param req
	 *            desired {@link RequestStatus}
	 * @return {@link COMPRESS_STATE} of request
	 */
	public static COMPRESS_STATE getCompressState(RequestStatus req) {
		COMPRESS_STATE result;
		if (req instanceof UploadFileRequestStatus) {
			result = ((UploadFileRequestStatus) req).isCompressing();
		} else {
			result = COMPRESS_STATE.WORKING;
		}
		logger.trace(String.format("Compress state for RequestStatus (%s) : %s", req.hashCode(), result));
		return result;
	}

	/**
	 * @param req
	 *            desired {@link RequestStatus}
	 * @return current progress
	 * @see RequestProgress
	 */
	public static RequestProgress getProgress(RequestStatus req) {
		return new RequestProgress(req);
	}

	/**
	 * @param req
	 *            desired {@link RequestStatus}
	 * @return localized {@link String} of latest activity
	 */
	public static String getLastActivity(RequestStatus req) {
		String result;
		long lastActiveTime = req.getLastActivity();
		Localizer localizer = Localizer.get();
		if (lastActiveTime == 0) {
			result = localizer.getString(L10N_UNKNOW_LA, null, L10N_UNKNOW_LA);
		} else {
			String lastActivityAgo = TimeUtil.formatTime(System.currentTimeMillis() - lastActiveTime);
			Map<String, String> substitution = new HashMap<String, String>();
			substitution.put("time", lastActivityAgo);
			result = localizer.getString(L10N_AGO_LA, null, Model.ofMap(substitution), L10N_AGO_LA);
		}
		logger.trace(String.format("Last activity for RequestStatus (%s) : %s", req.hashCode(), result));
		return result;
	}

	/**
	 * @param req
	 *            desired {@link RequestStatus}
	 * @return localized persistence status
	 */
	public static String getPersistence(RequestStatus req) {
		String key;
		if (req.isPersistentForever()) {
			key = L10N_PERSISTENCE_FOREVER;
		} else if (req.isPersistent()) {
			key = L10N_PERSISTENCE_REBOOT;
		} else {
			key = L10N_PERSISTENCE_NONE;
		}
		key = Localizer.get().getString(key, null);
		logger.trace(String.format("Persistence key for RequestStatus (%s) : %s", req.hashCode(), key));
		return key;
	}

	/**
	 * @param req
	 *            desired {@link RequestStatus}
	 * @return file name of request (can also be none)
	 */
	public static String getFileName(RequestStatus req) {
		File file = null;
		String result = null;
		if (req instanceof DownloadRequestStatus) {
			file = ((DownloadRequestStatus) req).getDestFilename();
		} else if (req instanceof UploadFileRequestStatus) {
			file = ((UploadFileRequestStatus) req).getOrigFilename();
		}
		if (file == null) {
			result = Localizer.get().getString(L10N_NONE, null, L10N_NONE);
		} else {
			result = file.toString();
		}
		logger.trace(String.format("File name for RequestStatus (%s) : %s", req.hashCode(), result));
		return result;
	}

	/**
	 * @param req
	 *            desired {@link RequestStatus}
	 * @return An array containing a link to the {@link FreenetURI} of request.
	 *         First item of array contains the link body and second one the
	 *         anchor
	 */
	public static String[] getKeyLink(RequestStatus req) {
		String[] result = new String[2];
		FreenetURI uri = null;
		String postfix = "";
		if (req instanceof DownloadRequestStatus) {
			uri = ((DownloadRequestStatus) req).getURI();
		} else if (req instanceof UploadFileRequestStatus) {
			uri = ((UploadFileRequestStatus) req).getFinalURI();
		} else if (req instanceof UploadDirRequestStatus) {
			postfix += "/";
			uri = ((UploadDirRequestStatus) req).getFinalURI();
		}
		if (uri != null) {
			result[0] = uri.toShortString();
			result[1] = "/" + uri + postfix;
		} else {
			result[0] = result[1] = Localizer.get().getString(L10N_UNKNOWN, null, L10N_UNKNOWN);
		}
		logger.trace(String.format("Link for RequestStatus (%s) : %s", req.hashCode(), result[1]));
		return result;
	}

}
