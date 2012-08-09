package freenet.winterface.web;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;

import com.db4o.ObjectContainer;

import freenet.client.HighLevelSimpleClientImpl;
import freenet.client.InsertContext.CompatibilityMode;
import freenet.client.MetadataUnresolvedException;
import freenet.client.async.ClientContext;
import freenet.client.async.DBJob;
import freenet.client.async.DatabaseDisabledException;
import freenet.keys.FreenetURI;
import freenet.node.Node;
import freenet.node.NodeClientCore;
import freenet.node.RequestStarter;
import freenet.node.fcp.ClientPut;
import freenet.node.fcp.ClientPutMessage;
import freenet.node.fcp.ClientRequest;
import freenet.node.fcp.FCPServer;
import freenet.node.fcp.IdentifierCollisionException;
import freenet.node.fcp.NotAllowedException;
import freenet.support.HexUtil;
import freenet.support.MutableBoolean;
import freenet.support.api.Bucket;
import freenet.support.io.NativeThread;
import freenet.winterface.web.core.AjaxFallbackCssButton;

@SuppressWarnings("serial")
public class InsertPage extends WinterPage {

	private final static String L10N_COMPAT_MODE_PREFIX = "InsertContext";
	private final static String L10N_PERSISTENCE_BROKEN = "QueueToadlet.persistenceBroken";
	private final static String L10N_NODE_SHUTTING_DOWN = "QueueToadlet.shuttingDown";
	private final static String L10N_INTERNAL_ERROR = "InsertException.shortError.3";
	private final static String L10N_UNRESOLVED_META_DATA = "InsertPage.unresolvedMetaData";
	private final static String L10N_ACCESS_DENIED_FILE = "QueueToadlet.errorAccessDeniedFile";
	private final static String L10N_INVALID_URI = "QueueToadlet.errorInvalidURIToU";
	private final static String L10N_NO_FILE_OR_CANNOT_READ = "QueueToadlet.errorNoFileOrCannotRead";
	private final static String L10N_BROWSE_FILES_LABEL = "QueueToadlet.insertFileBrowseButton";
	private final static String L10N_INSERT_FILE_LABEL = "QueueToadlet.insertFileInsertFileLabel";

	private final static Logger logger = Logger.getLogger(InsertPage.class);

	enum InsertMethod {
		CANONICAL("CHK@"), RANDOM("SSK@"), SPECIFIC(null);
		private String keyType;

		InsertMethod(String keyType) {
			this.keyType = keyType;
		}

		public String getKeyType() {
			return this.keyType;
		}
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		final Form<Void> insertForm = new Form<Void>("insertForm");
		// Feedback panel
		insertForm.add(new FeedbackPanel("feedback"));

		// Insert methods
		final RadioGroup<InsertMethod> methodGroup = new RadioGroup<InsertMethod>("methodGroup");
		Radio<InsertMethod> canonicalKey = new Radio<InsertMethod>("canonicalKey", Model.of(InsertMethod.CANONICAL));
		Radio<InsertMethod> randomKey = new Radio<InsertMethod>("randomKey", Model.of(InsertMethod.RANDOM));
		Radio<InsertMethod> specificKey = new Radio<InsertMethod>("specificKey", Model.of(InsertMethod.SPECIFIC));
		methodGroup.add(canonicalKey, randomKey, specificKey);
		insertForm.add(methodGroup);

		// Textfield for specific key
		final TextField<String> specificKeyContent = new TextField<String>("specificKeyContent");
		insertForm.add(specificKeyContent);

		// Options
		// TODO set visibility for NORMAL/ADVANCED mode
		// Compression
		final RadioGroup<Boolean> compressionGroup = new RadioGroup<Boolean>("compressionGroup", Model.of(true));
		Radio<Boolean> enable = new Radio<Boolean>("enableCompression", Model.of(true));
		Radio<Boolean> disable = new Radio<Boolean>("disableCompression", Model.of(false));
		compressionGroup.add(enable, disable);
		insertForm.add(compressionGroup);
		// Compatibility
		List<CompatibilityMode> modes = Arrays.asList(CompatibilityMode.values());
		EnumChoiceRenderer<CompatibilityMode> customEnumRenderer = new EnumChoiceRenderer<CompatibilityMode>() {
			@Override
			protected String resourceKey(CompatibilityMode object) {
				return L10N_COMPAT_MODE_PREFIX + "." + super.resourceKey(object);
			}
		};
		final DropDownChoice<CompatibilityMode> compatibilityMode = new DropDownChoice<CompatibilityMode>("compatibilityMode",
				Model.of(CompatibilityMode.COMPAT_UNKNOWN), modes, customEnumRenderer);
		insertForm.add(compatibilityMode);
		// Splitfile encryption key
		// Converter to get byte from input hex
		// TODO Put this either in own class or a factory
		final IConverter<byte[]> hexConverter = new IConverter<byte[]>() {
			@Override
			public byte[] convertToObject(String value, Locale locale) {
				if (value == null || "".equals(value.trim())) {
					return null;
				}
				return HexUtil.hexToBytes(value);
			}

			@Override
			public String convertToString(byte[] value, Locale locale) {
				if (value != null) {
					return HexUtil.bytesToHex(value);
				}
				return null;
			}
		};
		final TextField<byte[]> splitFileKey = new TextField<byte[]>("splitFileKey") {
			@SuppressWarnings("unchecked")
			@Override
			public <C> IConverter<C> getConverter(Class<C> type) {
				return (IConverter<C>) hexConverter;
			}
		};
		insertForm.add(splitFileKey);

		// Browse local
		String buttonText = localize(L10N_BROWSE_FILES_LABEL);
		AjaxFallbackCssButton browseLocal = new AjaxFallbackCssButton("localBrowse", Model.of(buttonText)) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				// TODO implement a nice ajax file browser
			}
		};
		insertForm.add(browseLocal);
		// Browse file
		final FileUploadField browseFile = new FileUploadField("browseFile");
		insertForm.add(browseFile);

		// Submit
		String buttonLabel = localize(L10N_INSERT_FILE_LABEL);
		AjaxFallbackButton submit = new AjaxFallbackButton("submit", insertForm) {
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				// Data retrieved from form
				InsertMethod insertMethod = methodGroup.getModelObject();
				String specificKey = specificKeyContent.getModelObject();
				boolean compression = compressionGroup.getModelObject();
				CompatibilityMode compMode = compatibilityMode.getModelObject();
				byte[] overrideSplitfileKey = splitFileKey.getModelObject();
				FileUpload upload = browseFile.getFileUpload();

				// Create respective URI
				FreenetURI insertURI = createInsertURI(insertMethod, specificKey, form);
				short uploadedFromType = findFromType();
				// File related stuff
				String fileName =  createFileName(upload);
				String identifier = fileName + "-" + System.currentTimeMillis();
				if (insertURI.getDocName() == null) {
					// File name is only needed in case of CHK and SSK without
					// specified file name
					fileName = null;
				}
				String MIMEType = createMIMEType(upload);
				Bucket tmpBucket = null;
				try {
					tmpBucket = createPersistentBucket(upload);
				} catch (IOException e) {
					logger.error("Error reading uploaded file");
					String errorMessage = InsertPage.this.localize(L10N_NO_FILE_OR_CANNOT_READ);
					form.error(errorMessage);
				}
				// Compatibility Mode
				if (CompatibilityMode.COMPAT_UNKNOWN.equals(compMode)) {
					compMode = CompatibilityMode.COMPAT_CURRENT;
				}
				queueInsert(insertURI, identifier, compression, uploadedFromType, MIMEType, tmpBucket, fileName, compMode, overrideSplitfileKey, insertForm);
			}

		};
		submit.add(new AttributeModifier("value", buttonLabel));
		insertForm.add(submit);

		add(insertForm);
	}

	private FreenetURI createInsertURI(InsertMethod insertMethod, String specificKey, Form<?> form) {
		FreenetURI insertURI = null;
		try {
			switch (insertMethod) {
			case CANONICAL:
				insertURI = new FreenetURI(InsertMethod.CANONICAL.keyType);
				break;
			case RANDOM:
				insertURI = new FreenetURI(InsertMethod.RANDOM.keyType);
				break;
			case SPECIFIC:
				insertURI = new FreenetURI(specificKey);
				break;
			}
		} catch (MalformedURLException e) {
			// This should only happen if user provides own key
			if (InsertMethod.SPECIFIC.equals(insertMethod)) {
				logger.error("Error creating URI with desired key");
				String errorMessage = InsertPage.this.localize(L10N_INVALID_URI);
				form.error(errorMessage);
			}
		}
		return insertURI;
	}
	
	private short findFromType() {
		return ClientPutMessage.UPLOAD_FROM_DIRECT;
	}

	private Bucket createPersistentBucket(FileUpload upload) throws IOException {
		long size = upload.getSize();
		final Bucket tmpBucket = getFreenetNode().clientCore.tempBucketFactory.makeBucket(size);
		return tmpBucket;
	}

	private String createFileName(FileUpload upload) {
		return upload.getClientFileName();
	}
	
	private String createMIMEType(FileUpload upload) {
		return "";
	}
	
	private void queueInsert(final FreenetURI insertURI, final String identifier, final boolean compress,final short uploadFromType, final String MIMEtype, final Bucket data,
			final String fileName, final CompatibilityMode cmode, final byte[] overrideSplitfileKey, final Form<?> form) {
		final NodeClientCore core = getFreenetNode().clientCore;
		final FCPServer fcp = core.getFCPServer();

		final MutableBoolean done = new MutableBoolean();
		DBJob insertJob = new DBJob() {
			@Override
			public boolean run(ObjectContainer container, ClientContext context) {
				try {
					final ClientPut clientPut = new ClientPut(fcp.getGlobalForeverClient(), insertURI, identifier, Integer.MAX_VALUE, null,
							RequestStarter.BULK_SPLITFILE_PRIORITY_CLASS, ClientRequest.PERSIST_FOREVER, null, false, !compress, -1,
							uploadFromType, null, MIMEtype, data, null, fileName, false, false, Node.FORK_ON_CACHEABLE_DEFAULT,
							HighLevelSimpleClientImpl.EXTRA_INSERTS_SINGLE_BLOCK, HighLevelSimpleClientImpl.EXTRA_INSERTS_SPLITFILE_HEADER, false, cmode,
							overrideSplitfileKey, fcp, container);
					if (clientPut != null) {
						fcp.startBlocking(clientPut, container, context);
					}
					return true;
				} catch (FileNotFoundException e) {
					logger.error("Error while persisting", e);
					String errorMessage = InsertPage.this.localize(L10N_NO_FILE_OR_CANNOT_READ);
					form.error(errorMessage);
					return false;
				} catch (MalformedURLException e) {
					logger.error("Error while persisting", e);
					String errorMessage = InsertPage.this.localize(L10N_INVALID_URI);
					form.error(errorMessage);
					return false;
				} catch (IdentifierCollisionException e) {
					// This happens when same file tries to be inserted in same
					// milliseconds
					logger.error("Error while persisting", e);
					return false;
				} catch (NotAllowedException e) {
					logger.error("Error while persisting", e);
					String errorMessage = InsertPage.this.localize(L10N_ACCESS_DENIED_FILE, Model.of(fileName));
					form.error(errorMessage);
					return false;
				} catch (MetadataUnresolvedException e) {
					// Shouldn't happen
					logger.error("Error while persisting", e);
					String errorMessage = InsertPage.this.localize(L10N_UNRESOLVED_META_DATA);
					form.warn(errorMessage);
					return false;
				} catch (Exception e) {
					logger.error("Internal error", e);
					String errorMessage = InsertPage.this.localize(L10N_INTERNAL_ERROR);
					form.error(errorMessage);
				} finally {
					synchronized (done) {
						// To make sure the method doesn't return before insert
						// is actually put into queue.
						done.value = true;
						done.notifyAll();
					}
				}
				return false;
			}
		};
		// Queue created job
		try {
			core.queue(insertJob, NativeThread.HIGH_PRIORITY + 1, false);
		} catch (DatabaseDisabledException e) {
			logger.error("Error while persisting", e);
			if (core.node.isStopping()) {
				String errorMessage = localize(L10N_NODE_SHUTTING_DOWN);
				form.error(errorMessage);
			} else {
				String errorMessage = localize(L10N_PERSISTENCE_BROKEN);
				form.error(errorMessage);
			}
		}
		// Wait to make sure DBJob has been started and insert has been put into
		// queue before returning
		synchronized (done) {
			while (!done.value) {
				try {
					done.wait();
				} catch (InterruptedException e) {
					// Ignore
				}
			}
		}
		return;
	}

}
