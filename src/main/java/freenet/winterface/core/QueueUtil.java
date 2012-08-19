package freenet.winterface.core;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wicket.Application;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import freenet.client.FetchException;
import freenet.client.async.DatabaseDisabledException;
import freenet.client.filter.ContentFilter;
import freenet.client.filter.MIMEType;
import freenet.node.RequestStarter;
import freenet.node.fcp.DownloadRequestStatus;
import freenet.node.fcp.FCPServer;
import freenet.node.fcp.RequestStatus;
import freenet.node.fcp.UploadDirRequestStatus;
import freenet.node.fcp.UploadFileRequestStatus;
import freenet.winterface.web.core.WinterfaceApplication;

public class QueueUtil {

	private int queueSize;

	private final int requestedClass;
	private final FCPServer fcp;

	public final ImmutableBiMap<String, List<RequestStatus>> dl_f_u_mime;
	private final Map<String, List<RequestStatus>> dl_f_u_mimeBackingMap;
	public final ImmutableBiMap<String, List<RequestStatus>> dl_f_b_mime;
	private final Map<String, List<RequestStatus>> dl_f_b_mimeBackingMap;

	public final ImmutableBiMap<Integer, List<RequestStatus>> requests;
	private final Map<Integer, List<RequestStatus>> requestsBackingMap;
	
	public final long totalQueueDownloadSize;
	public final long totalQueueUploadSize;
	
	public final static int DL = 1;
	public final static int DL_C_DISK = DL << 1 | DL;
	public final static int DL_C_TEMP = DL << 2 | DL;
	public final static int DL_F = DL << 3 | DL;
	public final static int DL_UC = DL << 4 | DL;
	public final static int DL_F_U_MIME = DL << 5 | DL;
	public final static int DL_F_B_MIME = DL << 6 | DL;
	public final static int DL_ALL = DL | DL_C_DISK | DL_C_TEMP | DL_F | DL_UC | DL_F_U_MIME | DL_F_B_MIME;
	public final static List<Integer> DOWNLOAD_CLASSES = ImmutableList.of(DL_C_DISK, DL_C_TEMP, DL_F, DL_UC, DL_F_U_MIME, DL_F_B_MIME);

	public final static int UP = DL << 7;
	public final static int UP_C = UP << 1 | UP;
	public final static int UP_C_DIR = UP << 2 | UP;
	public final static int UP_F = UP << 3 | UP;
	public final static int UP_F_DIR = UP << 4 | UP;
	public final static int UP_UC = UP << 5 | UP;
	public final static int UP_UC_DIR = UP << 6 | UP;
	public final static int UP_ALL = UP | UP_C | UP_C_DIR | UP_F | UP_F_DIR | UP_UC | UP_UC_DIR;
	public final static List<Integer> UPLOAD_CLASSES = ImmutableList.of(UP_C, UP_C_DIR, UP_F, UP_F_DIR, UP_UC, UP_UC_DIR);

	public final static BiMap<Integer, String> codeNameMap;

	private final static Logger logger = Logger.getLogger(QueueUtil.class);

	static {
		Builder<Integer, String> builder = ImmutableBiMap.<Integer, String> builder();
		builder.put(DL_C_DISK, "completedDownloadToDisk");
		builder.put(DL_C_TEMP, "completedDownloadToTemp");
		builder.put(DL_F, "failedDownload");
		builder.put(DL_UC, "uncompletedDownload");
		builder.put(DL_F_U_MIME, "failedUnknownMIMEType");
		builder.put(DL_F_B_MIME, "failedBadMIMEType");
		builder.put(UP_C, "completedUpload");
		builder.put(UP_C_DIR, "completedDirUpload");
		builder.put(UP_F, "failedUpload");
		builder.put(UP_UC, "uncompletedUpload");
		builder.put(UP_UC_DIR, "uncompletedDirUpload");
		codeNameMap = builder.build();
	}

	public QueueUtil(int requestedClass) throws DatabaseDisabledException {
		requestsBackingMap = Maps.newHashMap();
		dl_f_b_mimeBackingMap = Maps.newHashMap();
		dl_f_u_mimeBackingMap = Maps.newHashMap();
		queueSize = 0;
		logger.debug("Getting request queue for code " + Integer.toBinaryString(requestedClass));
		this.requestedClass = requestedClass;
		fcp = ((WinterfaceApplication) Application.get()).getFreenetWrapper().getNode().clientCore.getFCPServer();
		RequestStatus[] globalRequests;
		globalRequests = fcp.getGlobalRequests();
		short lowestQueuedPrio = RequestStarter.MINIMUM_PRIORITY_CLASS;
		long tmpTotalQueuedDownloadSize = 0;
		long tmpTotalQueuedUploadSize = 0;
		for (RequestStatus req : globalRequests) {
			// If current request (req) is a download request and user wanted to
			// get download requests (DL flag)
			if (req instanceof DownloadRequestStatus && isDesired(DL)) {
				DownloadRequestStatus download = (DownloadRequestStatus) req;
				if (download.hasSucceeded()) {
					if (download.toTempSpace()) {
						addToList(download, DL_C_TEMP);
					} else {
						addToList(download, DL_C_TEMP);
					}
				} else if (download.hasFinished() && isDesired(DL_F)) {
					int failureCode = download.getFailureCode();
					if (failureCode == FetchException.CONTENT_VALIDATION_UNKNOWN_MIME) {
						String mimeType = download.getMIMEType();
						mimeType = ContentFilter.stripMIMEType(mimeType);
						addToMap(download, mimeType, DL_F_U_MIME);
					} else if (failureCode == FetchException.CONTENT_VALIDATION_BAD_MIME) {
						String mimeType = download.getMIMEType();
						mimeType = ContentFilter.stripMIMEType(mimeType);
						MIMEType type = ContentFilter.getMIMEType(mimeType);
						if (type == null) {
							if (isDesired(DL_F_U_MIME)) {
								logger.warn("Bad MIME failure code yet MIME is " + mimeType + " which does not have a handler!");
								addToMap(download, mimeType, DL_F_U_MIME);
							}
						} else {
							addToMap(download, mimeType, DL_F_B_MIME);
						}
					} else {
						addToList(download, DL_F);
					}
				} else {
					short prio = download.getPriority();
					if (prio < lowestQueuedPrio) {
						lowestQueuedPrio = prio;
					}
					addToList(download, DL_UC);
					long size = download.getDataSize();
					if (size > 0) {
						tmpTotalQueuedDownloadSize += size;
					}
				}
			}
			// If current request (req) is an upload request and user wanted to
			// get upload requests (UP flag)
			else if (req instanceof UploadFileRequestStatus && isDesired(UP)) {
				UploadFileRequestStatus upload = (UploadFileRequestStatus) req;
				if (upload.hasSucceeded()) {
					addToList(upload, UP_C);
				} else if (upload.hasFinished()) {
					addToList(upload, UP_F);
				} else {
					short prio = upload.getPriority();
					if (prio < lowestQueuedPrio) {
						lowestQueuedPrio = prio;
					}
					addToList(upload, UP_UC);
				}
				long size = upload.getDataSize();
				if (size > 0) {
					tmpTotalQueuedUploadSize += size;
				}
			}
			// If current request (req) is an upload dir request and user wanted
			// to get upload requests (DL flag)
			else if (req instanceof UploadDirRequestStatus && isDesired(UP)) {
				UploadDirRequestStatus upload = (UploadDirRequestStatus) req;
				if (upload.hasSucceeded()) {
					addToList(upload, UP_C_DIR);
				} else if (upload.hasFinished()) {
					addToList(upload, UP_F_DIR);
				} else {
					short prio = upload.getPriority();
					if (prio < lowestQueuedPrio) {
						lowestQueuedPrio = prio;
					}
					addToList(upload, UP_UC_DIR);
				}
				long size = upload.getTotalDataSize();
				if (size > 0) {
					tmpTotalQueuedUploadSize += size;
				}
			}
		}
		totalQueueDownloadSize = tmpTotalQueuedDownloadSize;
		totalQueueUploadSize = tmpTotalQueuedUploadSize;
		// Create immutable bimaps from backing maps
		requests = ImmutableBiMap.copyOf(requestsBackingMap);
		dl_f_b_mime = ImmutableBiMap.copyOf(dl_f_b_mimeBackingMap);
		dl_f_u_mime = ImmutableBiMap.copyOf(dl_f_u_mimeBackingMap);
	}

	private boolean isDesired(int targetClass) {
		return matches(requestedClass, targetClass);
	}

	public static boolean matches(int base, int target) {
		return ((base & target) == target);
	}

	private void addToList(RequestStatus request, int targetClass) {
		if (!isDesired(targetClass)) {
			return;
		}
		List<RequestStatus> list = requestsBackingMap.get(targetClass);
		if (list == null) {
			list = Lists.newLinkedList();
		}
		list.add(request);
		requestsBackingMap.put(targetClass, list);
		queueSize++;
		logger.trace("Added request " + request.hashCode() + " to list with code " + Integer.toBinaryString(targetClass));
	}

	public List<RequestStatus> getList(int targetClass) {
		classMustBeSingle(targetClass);
		List<RequestStatus> result = requests.get(targetClass);
		return result != null ? ImmutableList.copyOf(result) : null;
	}

	private void classMustBeSingle(int targetClass) {
		int ones = countOnes(targetClass);
		if (ones != 2) {
			throw new IllegalArgumentException("Complex target classes cannot be accepted. Only one list at a time. You seem to query " + (ones - 1)
					+ " lists.");
		}
	}

	public Map<String, List<RequestStatus>> getMap(int targetClass) {
		if (targetClass == DL_F_B_MIME) {
			return dl_f_b_mime;
		} else if (targetClass == DL_F_U_MIME) {
			return dl_f_u_mime;
		} else {
			throw new IllegalArgumentException("Only applicable for values " + DL_F_B_MIME + " and " + DL_F_U_MIME);
		}
	}

	private void addToMap(RequestStatus request, String MIMEType, int targetClass) {
		if (!isDesired(targetClass)) {
			return;
		}
		List<RequestStatus> list = null;
		switch (targetClass) {
		case DL_F_B_MIME:
			list = dl_f_b_mimeBackingMap.get(MIMEType);
			if (list == null) {
				list = Lists.newLinkedList();
			}
			dl_f_b_mimeBackingMap.put(MIMEType, list);
			break;
		case DL_F_U_MIME:
			list = dl_f_u_mimeBackingMap.get(MIMEType);
			if (list == null) {
				list = Lists.newLinkedList();
			}
			dl_f_u_mimeBackingMap.put(MIMEType, list);
			break;
		default:
			logger.warn("Tried to add a RequestStatus with an unknown class: " + targetClass);
		}
		if (list != null) {
			list.add(request);
		}
		queueSize++;
	}

	public List<RequestStatus> get(int targetClass) {
		classMustBeSingle(targetClass);
		ImmutableCollection<List<RequestStatus>> values = null;
		if (targetClass == DL_F_U_MIME) {
			values = ImmutableList.copyOf(dl_f_b_mime.values());
		} else if (targetClass == DL_F_B_MIME) {
			values = ImmutableList.copyOf(dl_f_b_mime.values());
		}
		if (values != null) {
			List<RequestStatus> result = Lists.newLinkedList();
			for (List<RequestStatus> list : values) {
				result.addAll(list);
			}
			return ImmutableList.copyOf(result);
		} else {
			return getList(targetClass);
		}
	}

	public int getQueueSize() {
		return queueSize;
	}
	
	public FCPServer getFCPServer() {
		return fcp;
	}

	/**
	 * 
	 * @param number
	 * @return
	 * @see <a
	 *      href="http://graphics.stanford.edu/~seander/bithacks.html#CountBitsSetParallel">reference</a>
	 */
	private int countOnes(int i) {
		i = i - ((i >> 1) & 0x55555555);
		i = (i & 0x33333333) + ((i >> 2) & 0x33333333);
		return (((i + (i >> 4)) & 0x0F0F0F0F) * 0x01010101) >> 24;
	}

}
