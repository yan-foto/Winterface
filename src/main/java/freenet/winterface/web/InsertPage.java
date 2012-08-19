package freenet.winterface.web;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;

import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.file.File;

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
import freenet.node.fcp.ClientPutBase;
import freenet.node.fcp.ClientPutDir;
import freenet.node.fcp.ClientPutMessage;
import freenet.node.fcp.ClientRequest;
import freenet.node.fcp.FCPServer;
import freenet.node.fcp.IdentifierCollisionException;
import freenet.node.fcp.NotAllowedException;
import freenet.support.HexUtil;
import freenet.support.MutableBoolean;
import freenet.support.api.Bucket;
import freenet.support.io.BucketTools;
import freenet.support.io.FileBucket;
import freenet.support.io.NativeThread;
import freenet.winterface.web.markup.LocalBrowserPanel;

/**
 * A {@link WinterPage} to insert files into freenet.
 * <p>
 * Files can either be uploaded through browser or using a local file browser
 * (see {@link LocalBrowserPanel}). If both files are available the local file
 * is inserted.
 * </p>
 * 
 * @author pausb
 */
@SuppressWarnings("serial")
public class InsertPage extends WinterPage {

	// L10N
	private final static String L10N_COMPAT_MODE_PREFIX = "InsertContext";
	private final static String L10N_PERSISTENCE_BROKEN = "QueueToadlet.persistenceBroken";
	private final static String L10N_NODE_SHUTTING_DOWN = "QueueToadlet.shuttingDown";
	private final static String L10N_INTERNAL_ERROR = "InsertException.shortError.3";
	private final static String L10N_UNRESOLVED_META_DATA = "InsertPage.unresolvedMetaData";
	private final static String L10N_ACCESS_DENIED_FILE = "QueueToadlet.errorAccessDeniedFile";
	private final static String L10N_INVALID_URI = "QueueToadlet.errorInvalidURIToU";
	private final static String L10N_NO_FILE_OR_CANNOT_READ = "QueueToadlet.errorNoFileOrCannotRead";
	private final static String L10N_INSERT_FILE_LABEL = "QueueToadlet.insertFileInsertFileLabel";
	private final static String L10N_UPLOAD_SUCCEEDED = "QueueToadlet.uploadSucceededTitle";
	private final static String L10N_UPLOAD_SUCCEEDED_SIMPLE = "WelcomeToadlet.insertSucceededTitle";
	private final static String L10N_NO_FILE_SELCETED = "QueueToadlet.errorNoFileSelected";

	/** Log4j Logger */
	private final static Logger logger = Logger.getLogger(InsertPage.class);

	/**
	 * Three types of insert keys
	 */
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
		final RadioGroup<InsertMethod> methodGroup = new RadioGroup<InsertMethod>("methodGroup", Model.of(InsertMethod.RANDOM));
		Radio<InsertMethod> canonicalKey = new Radio<InsertMethod>("canonicalKey", Model.of(InsertMethod.CANONICAL));
		Radio<InsertMethod> randomKey = new Radio<InsertMethod>("randomKey", Model.of(InsertMethod.RANDOM));
		Radio<InsertMethod> specificKey = new Radio<InsertMethod>("specificKey", Model.of(InsertMethod.SPECIFIC));
		methodGroup.add(canonicalKey, randomKey, specificKey);
		insertForm.add(methodGroup);

		// Textfield for specific key
		final TextField<String> specificKeyContent = new TextField<String>("specificKeyContent", Model.of(""));
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
		final TextField<byte[]> splitFileKey = new TextField<byte[]>("splitFileKey", new Model<byte[]>()) {
			@SuppressWarnings("unchecked")
			@Override
			public <C> IConverter<C> getConverter(Class<C> type) {
				return (IConverter<C>) hexConverter;
			}
		};
		insertForm.add(splitFileKey);

		// Browse local
		final IModel<String> selectedModel = new Model<String>("");
		final HiddenField<String> selectedFile = new HiddenField<String>("selectedFile", selectedModel);
		LocalBrowserPanel localBrowser = new LocalBrowserPanel("localBrowser", Model.of("/")) {
			@Override
			public void fileSelected(String path, AjaxRequestTarget target) {
				logger.debug(path + " was selected for insertion");
				selectedModel.setObject(path);
				updateAjaxComponent(target, insertForm);
			}
		};
		Label localSelected = new Label("localSelected", selectedModel);
		insertForm.add(selectedFile, localBrowser, localSelected);
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
				String localFile = selectedFile.getModelObject();

				// Create respective URI
				FreenetURI insertURI = createInsertURI(insertMethod, specificKey, form);
				short uploadedFromType = ClientPutMessage.UPLOAD_FROM_DIRECT;

				// File related stuff
				String fileName = "", identifier = "", MIMEType = "";
				Bucket tmpBucket = null;
				File directoryFile = null;
				File uploadedFile = null;
				if (localFile != null && !"".equals(localFile.trim())) {
					uploadedFile = new File(localFile);
				}
				boolean browsedFileIsOk = false;
				try {
					// User has selected file from local file browser
					if (uploadedFile != null && uploadedFile.canRead()) {
						logger.trace("File selected LocalBrowserPanel");
						// Add directory checking
						if (uploadedFile.exists()) {
							fileName = uploadedFile.getName();
							if (uploadedFile.isDirectory()) {
								// If file is a directory the insert DBJob would
								// use clientPutDir instead of ClientPut (see
								// #queueInsert)
								directoryFile = uploadedFile;
							} else {
								MIMEType = new MimetypesFileTypeMap().getContentType(uploadedFile);
								tmpBucket = new FileBucket(uploadedFile, true, false, false, false, false);
							}
							browsedFileIsOk = true;
							uploadedFromType = ClientPutMessage.UPLOAD_FROM_DISK;
						}
					}
					// User has uploaded file
					else if (upload != null && !browsedFileIsOk) {
						logger.trace("File selected by browser upload");
						fileName = upload.getClientFileName();
						MIMEType = upload.getContentType();
						tmpBucket = createPersistentBucket(upload.getSize());
						BucketTools.copyFrom(tmpBucket, upload.getInputStream(), -1);
					}
				} catch (IOException e) {
					logger.error("Error reading uploaded file");
					String errorMessage = InsertPage.this.localize(L10N_NO_FILE_OR_CANNOT_READ);
					form.error(errorMessage);
					updateAjaxComponent(target, form);
					return;
				}
				// Stop here if no file is selected
				if (uploadedFile == null && upload == null) {
					form.error(localize(L10N_NO_FILE_SELCETED));
					updateAjaxComponent(target, form);
					return;
				}
				identifier = fileName + "-fred-" + System.currentTimeMillis();
				if (insertURI.getDocName() == null) {
					// File name is only needed in case of CHK and SSK without
					// specified file name
					fileName = null;
				}
				// Compatibility Mode
				if (CompatibilityMode.COMPAT_UNKNOWN.equals(compMode)) {
					compMode = CompatibilityMode.COMPAT_CURRENT;
				}
				queueInsert(directoryFile, insertURI, identifier, compression, uploadedFromType, uploadedFile, MIMEType, tmpBucket, fileName, compMode,
						overrideSplitfileKey, insertForm);
				// Reset form
				form.clearInput();
				// Clear hidden field
				selectedFile.setModelObject("");
				updateAjaxComponent(target, form);
			}

		};
		submit.add(new AttributeModifier("value", buttonLabel));
		insertForm.add(submit);

		add(insertForm);
	}

	/**
	 * A convenient method to add {@link Component}s to an
	 * {@link AjaxRequestTarget} <b>if</b> it's not null.
	 * 
	 * @param target
	 *            target to add to
	 * @param component
	 *            to add to target
	 */
	private void updateAjaxComponent(AjaxRequestTarget target, Component component) {
		if (target != null) {
			target.add(component);
		}
	}

	/**
	 * Depending on user input, creates a corresponding {@link FreenetURI}
	 * 
	 * @param insertMethod
	 *            chosen method by user
	 * @param specificKey
	 *            chosen specific key (optional)
	 * @param form
	 *            original form to write errors to
	 * @return created FreenetURI
	 */
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

	/**
	 * Creates a persistent temporary bucket in case the file is uploaded
	 * through browser
	 * 
	 * @param size
	 *            size of bucket
	 * @return created bucket
	 * @throws IOException
	 *             in case bucket cannot be created
	 */
	private Bucket createPersistentBucket(long size) throws IOException {
		final Bucket tmpBucket = getFreenetNode().clientCore.persistentTempBucketFactory.makeBucket(size);
		return tmpBucket;
	}

	/**
	 * Prepares a {@link DBJob} for selected {@link File} and starts it.
	 * 
	 * @param directoryFile
	 *            directory {@link File} (mandatory <i>if</i> file is a
	 *            directory)
	 * @param insertURI
	 *            {@link FreenetURI} of insert
	 * @param identifier
	 *            file identifier
	 * @param compress
	 *            compress method
	 * @param uploadFromType
	 *            source of upload
	 * @param origFile
	 *            original file (is ignored <i>if</i> file is a directory)
	 * @param MIMEtype
	 *            content type of file (is ignored <i>if</i> file is a
	 *            directory)
	 * @param data
	 *            Bucket containing data (mandatory if <i>if</i> file is not a
	 *            directory)
	 * @param fileName
	 *            name of file
	 * @param cmode
	 *            compatibility mode
	 * @param overrideSplitfileKey
	 *            custom split file key (optional)
	 * @param form
	 *            original {@link Form} to write errors to
	 */
	private void queueInsert(final File directoryFile, final FreenetURI insertURI, final String identifier, final boolean compress, final short uploadFromType,
			final File origFile, final String MIMEtype, final Bucket data, final String fileName, final CompatibilityMode cmode,
			final byte[] overrideSplitfileKey, final Form<?> form) {
		final NodeClientCore core = getFreenetNode().clientCore;
		final FCPServer fcp = core.getFCPServer();
		final MutableBoolean done = new MutableBoolean();
		final Throwable ex = new Throwable();

		DBJob insertJob = new DBJob() {

			@Override
			public boolean run(ObjectContainer container, ClientContext context) {
				ClientPutBase clientPut = null;
				try {
					// File to insert is a directory
					if (directoryFile != null) {
						clientPut = new ClientPutDir(fcp.getGlobalForeverClient(), insertURI, identifier, Integer.MAX_VALUE,
								RequestStarter.BULK_SPLITFILE_PRIORITY_CLASS, ClientRequest.PERSIST_FOREVER, null, false, !compress, -1, directoryFile, null,
								false, true, false, false, Node.FORK_ON_CACHEABLE_DEFAULT, HighLevelSimpleClientImpl.EXTRA_INSERTS_SINGLE_BLOCK,
								HighLevelSimpleClientImpl.EXTRA_INSERTS_SPLITFILE_HEADER, false, overrideSplitfileKey, fcp, container);
					}
					// File to insert is a file
					else if (data != null) {
						clientPut = new ClientPut(fcp.getGlobalForeverClient(), insertURI, identifier, Integer.MAX_VALUE, null,
								RequestStarter.BULK_SPLITFILE_PRIORITY_CLASS, ClientRequest.PERSIST_FOREVER, null, false, !compress, -1, uploadFromType,
								origFile, MIMEtype, data, null, fileName, false, false, Node.FORK_ON_CACHEABLE_DEFAULT,
								HighLevelSimpleClientImpl.EXTRA_INSERTS_SINGLE_BLOCK, HighLevelSimpleClientImpl.EXTRA_INSERTS_SPLITFILE_HEADER, false, cmode,
								overrideSplitfileKey, fcp, container);
					}
					// Start insertion
					if (clientPut != null) {
						fcp.startBlocking(clientPut, container, context);
					}
					return true;
				} catch (Throwable e) {
					// We take care of this later
					logger.error("Error while starting DBJob");
					ex.initCause(e);
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

		// Queue the insert
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

		// Queue created job
		Throwable cause = ex.getCause();
		if (cause != null) {
			if (cause instanceof FileNotFoundException) {
				logger.error("Error while persisting", cause);
				String errorMessage = localize(L10N_NO_FILE_OR_CANNOT_READ);
				form.error(errorMessage);
			} else if (cause instanceof MalformedURLException) {
				logger.error("Error while persisting", cause);
				String errorMessage = localize(L10N_INVALID_URI);
				form.error(errorMessage);
			} else if (cause instanceof IdentifierCollisionException) {
				// This happens when same file tries to be inserted in same
				// milliseconds
				logger.error("Error while persisting", cause);
			} else if (cause instanceof NotAllowedException) {
				logger.error("Error while persisting", cause);
				Map<String, String> substitution = new HashMap<String, String>();
				substitution.put("file", fileName);
				String errorMessage = localize(L10N_ACCESS_DENIED_FILE, Model.ofMap(substitution));
				form.error(errorMessage);
			} else if (cause instanceof MetadataUnresolvedException) {
				// Shouldn't happen
				logger.error("Error while persisting", cause);
				String errorMessage = localize(L10N_UNRESOLVED_META_DATA);
				form.warn(errorMessage);
			} else {
				logger.error("Internal error", cause);
				String errorMessage = localize(L10N_INTERNAL_ERROR);
				form.error(errorMessage);
			}
			return;
		}

		// see FreenetURI#getDocName
		if (fileName == null) {
			form.info(localize(L10N_UPLOAD_SUCCEEDED_SIMPLE));
		} else {
			Map<String, String> substitution = new HashMap<String, String>();
			substitution.put("filename", fileName);
			form.info(localize(L10N_UPLOAD_SUCCEEDED, Model.ofMap(substitution)));
		}
		return;
	}
}
