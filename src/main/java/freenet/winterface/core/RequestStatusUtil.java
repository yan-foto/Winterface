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
import freenet.support.TimeUtil;

public class RequestStatusUtil {

	// Flags
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

	private final static Logger logger = Logger.getLogger(RequestsUtil.class);

	public static String getPriority(RequestStatus req) {
		String result = Localizer.get().getString(L10N_PRIO_PREFIX + req.getPriority(), null);
		logger.trace(String.format("Priority for RequestStatus (%s) : %s", req.hashCode(), result));
		return result;
	}

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

	public static RequestProgress getProgress(RequestStatus req) {
		return new RequestProgress(req);
	}

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
