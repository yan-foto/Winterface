package freenet.winterface.core;

import org.apache.wicket.Localizer;

import freenet.node.fcp.ClientPut.COMPRESS_STATE;
import freenet.node.fcp.RequestStatus;

public class RequestProgress {

	public int fetchedPercent = -1;
	public int failedPercent = -1;
	public int fatallyFailedPercent = -1;
	public int minPercent = -1;
	public int mainPercent = -1;

	public final int specialFlag;

	public final static int FLAG_PROG_UNKNOWN = -1;
	public final static int FLAG_PROG_STARTING = -2;
	public final static int FLAG_PROG_COMP_W = -3;
	public final static int FLAG_PROG_COMP = -4;

	public final static String L10N_PROG_UNKNOWN = "QueueToadlet.unknown";
	public final static String L10N_PROG_STARTING = "QueueToadlet.starting";
	public final static String L10N_PROG_COMP_W = "QueueToadlet.awaitingCompression";
	public final static String L10N_PROG_COMP = "QueueToadlet.compressing";

	public RequestProgress(RequestStatus req) {

		int total = req.getTotalBlocks();
		int min = req.getMinBlocks();
		int fetched = req.getFetchedBlocks();
		int failed = req.getFailedBlocks();
		int fatallyFailed = req.getFatalyFailedBlocks();

		// Copied from QueueToadlet.
		if (total < min /* FIXME why? */) {
			total = min;
		}

		if (!req.isStarted() || ((fetched < 0) || (total <= 0))) {
			specialFlag = FLAG_PROG_STARTING;
		} else if (COMPRESS_STATE.WAITING.equals(RequestStatusUtil.getCompressState(req))) {
			specialFlag = FLAG_PROG_COMP_W;
		} else if (COMPRESS_STATE.WORKING.equals(RequestStatusUtil.getCompressState(req))) {
			specialFlag = FLAG_PROG_COMP;
		} else {
			specialFlag = 0;
		}
		if (total != 0) {
			// No special state
			this.fetchedPercent = (int) (fetched / (double) total * 100);
			this.failedPercent = (int) (failed / (double) total * 100);
			this.fatallyFailedPercent = (int) (fatallyFailed / (double) total * 100);
			this.minPercent = (int) (min / (double) total * 100);
			this.mainPercent = (int) (((fetched / (double) min) * 1000) / 10.0);
		}

	}

	public String localizeSpecialFlag() {
		return localizeSpecialFlag(specialFlag);
	}

	public static String localizeSpecialFlag(int specialFlag) {
		if (specialFlag > -1 || specialFlag < -4) {
			throw new IllegalArgumentException("Special flag out of bound [-4,-1]");
		}
		String key = null;
		switch (specialFlag) {
		case (FLAG_PROG_UNKNOWN):
			key = L10N_PROG_UNKNOWN;
			break;
		case (FLAG_PROG_STARTING):
			key = L10N_PROG_STARTING;
			break;
		case (FLAG_PROG_COMP_W):
			key = L10N_PROG_COMP_W;
			break;
		case (FLAG_PROG_COMP):
			key = L10N_PROG_COMP;
			break;
		}
		return Localizer.get().getString(key, null, key);
	}

}
