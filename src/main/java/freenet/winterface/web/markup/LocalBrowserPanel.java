package freenet.winterface.web.markup;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.comparator.DirectoryFileComparator;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.comparator.NameFileComparator;
import org.apache.commons.io.comparator.SizeFileComparator;
import org.apache.commons.io.filefilter.SizeFileFilter;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

import freenet.winterface.web.core.AjaxFallbackCssButton;

/**
 * An Ajax file browser with fallback mode.
 * <p>
 * This {@link Panel} can be used to browse local files and is capable of
 * sorting files and directories (see {@link SortType}). <b>NOTE:</b> this panel
 * only lists files of size >= 1B.
 * </p>
 * 
 * @author pausb
 */
// TODO make all links into fallback submit buttons so if its nested in another
// form, the state of the parent is preserved if this panel is changed
@SuppressWarnings("serial")
public abstract class LocalBrowserPanel extends Panel {

	/** Various types to sort list of {@link File}s */
	enum SortType {
		/** Sort by type (directory/file) ({@link DirectoryFileComparator}) */
		TYPE,
		/** Sort by name ({@link NameFileComparator}) */
		NAME,
		/** Sort by size ({@link SizeFileComparator}) */
		SIZE,
		/** Sort by last modification time ({@link LastModifiedFileComparator}) */
		LASTMOD;
	}

	/** Default sort type */
	private SortType sortType = SortType.TYPE;

	/** Default start path */
	private final static String root = System.getProperty("user.home");

	/** Log4j logger */
	private final static Logger logger = Logger.getLogger(LocalBrowserPanel.class);

	/**
	 * Constructs
	 * 
	 * @param id
	 *            id of tag to replace this {@link Panel} with
	 * @param model
	 *            contains starting path to browse
	 */
	public LocalBrowserPanel(String id, IModel<String> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		// Parent container needed for AJAX refresh
		final WebMarkupContainer mainContainer = new WebMarkupContainer("mainContainer");
		mainContainer.setOutputMarkupId(true);
		// Current path it gets updated every time panel's model is updated
		final LoadableDetachableModel<String> currentDirModel = new LoadableDetachableModel<String>() {
			@Override
			protected String load() {
				return LocalBrowserPanel.this.getDefaultModelObjectAsString();
			}
		};
		// List of files and directories in current path
		final LoadableDetachableModel<List<File>> childrenModel = new LoadableDetachableModel<List<File>>() {
			@Override
			protected List<File> load() {
				String startPath = LocalBrowserPanel.this.getDefaultModelObjectAsString();
				if (startPath == null || "".equals(startPath.trim())) {
					logger.warn("Panel model object was null! Maybe an empty path was given.");
					// This can happen if user turns JS on and off!
					detach();
					startPath = root;
				}
				logger.trace("Browsing to path " + startPath);
				final File start = new File(startPath);
				File[] children = start.listFiles((FileFilter) new SizeFileFilter(1));
				Comparator<File> comparator = null;
				switch (LocalBrowserPanel.this.sortType) {
				case TYPE:
					comparator = new DirectoryFileComparator();
					break;
				case NAME:
					comparator = new NameFileComparator();
					break;
				case SIZE:
					comparator = new SizeFileComparator();
					break;
				case LASTMOD:
					comparator = new LastModifiedFileComparator();
					break;
				}
				Arrays.sort(children, comparator);
				return Arrays.asList(children);
			}
		};
		// Current directory
		Form<Void> pathForm = new Form<Void>("pathForm");
		final TextField<String> currentDir = new TextField<String>("currentDir", currentDirModel);
		currentDir.setRequired(true);
		// TODO: this should be removed in next of release of Wicket. Currently
		// it is necessary
		Form<?> parentForm = findParent(Form.class);
		parentForm = parentForm == null ? pathForm : parentForm;
		AjaxFallbackButton browserTo = new AjaxFallbackButton("browseTo", parentForm) {
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				LocalBrowserPanel.this.setDefaultModelObject(currentDir.getModelObject());
				resort(null, target, mainContainer, childrenModel);
			}
		};
		// Up directory
		AjaxFallbackLink<Void> upDir = new AjaxFallbackLink<Void>("upDir") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				File current = new File(LocalBrowserPanel.this.getDefaultModelObjectAsString());
				// Never go up if in root
				if (!Arrays.asList(File.listRoots()).contains(current)) {
					LocalBrowserPanel.this.setDefaultModelObject(current.getParent());
					resort(null, target, mainContainer, childrenModel);
				}
			}
		};
		pathForm.add(currentDir, browserTo, upDir);
		mainContainer.add(pathForm);
		// Headers
		// Type (File / Directory)
		AjaxFallbackLink<SortType> typeSort = new AjaxFallbackLink<SortType>("typeSort", Model.of(SortType.TYPE)) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				resort(this, target, mainContainer, childrenModel);
			}
		};
		// Name
		AjaxFallbackLink<SortType> nameSort = new AjaxFallbackLink<SortType>("nameSort", Model.of(SortType.NAME)) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				resort(this, target, mainContainer, childrenModel);
			}
		};
		// Size
		AjaxFallbackLink<SortType> sizeSort = new AjaxFallbackLink<SortType>("sizeSort", Model.of(SortType.SIZE)) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				resort(this, target, mainContainer, childrenModel);
			}
		};
		// Last modification
		AjaxFallbackLink<SortType> lastModSort = new AjaxFallbackLink<SortType>("lastModSort", Model.of(SortType.LASTMOD)) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				resort(this, target, mainContainer, childrenModel);
			}
		};
		mainContainer.add(typeSort, nameSort, sizeSort, lastModSort);

		ListView<File> childrenList = new ListView<File>("children", childrenModel) {
			@Override
			protected void populateItem(final ListItem<File> item) {
				// components
				String imgSrc;
				if (item.getModelObject().isDirectory()) {
					imgSrc = "folder";
				} else {
					imgSrc = "file";
				}
				// TODO make AjaxFallbackCSSButton icons available for all
				Image typeImg = new Image("fileType", new PackageResourceReference(AjaxFallbackCssButton.class, "img/" + imgSrc + ".png")) {
					@Override
					protected boolean shouldAddAntiCacheParameter() {
						// We don't want to rerender each icon each time the
						// list is updated
						return false;
					}
				};
				item.add(typeImg);
				// Link (name) when clicked on item
				final File file = item.getModelObject();
				AjaxFallbackLink<Void> fileLink = new AjaxFallbackLink<Void>("link") {
					@Override
					public void onClick(AjaxRequestTarget target) {
						try {
							if (file.isDirectory()) {
								LocalBrowserPanel.this.setDefaultModelObject(file.getCanonicalPath().toString());
								// Force redraw
								childrenModel.detach();
							} else {
								// Toggle the selection
								fileSelected(file.getCanonicalPath().toString(), target);
							}
						} catch (IOException e) {
							// Ignore. this cannot happen
						}
						if (target != null) {
							target.add(mainContainer);
						}
					}
				};
				// Link Label
				fileLink.setBody(Model.of(file.getName()));
				item.add(fileLink);
				// Size
				// Directories are considered of size zero in sorting
				String length = file.isDirectory() ? "-" : file.length() + "";
				Label size = new Label("size", Model.of(length));
				item.add(size);
				// Las modified
				Label lastModified = new Label("lastModified", Model.of(new Date(file.lastModified())));
				item.add(lastModified);
				// Download link
				AjaxFallbackLink<Void> download = new AjaxFallbackLink<Void>("download") {
					@Override
					public void onClick(AjaxRequestTarget target) {
						try {
							LocalBrowserPanel.this.fileSelected(item.getModelObject().getCanonicalPath().toString(), target);
						} catch (IOException e) {
							// Ignore. this cannot happen
						}
					}
				};
				item.add(download);
			}
		};
		mainContainer.add(childrenList);
		add(mainContainer);
	}

	/**
	 * Resets the sorting of files.
	 * 
	 * @param sortLink
	 *            {@link AjaxFallbackLink} caused the resort
	 * @param target
	 *            in case of AJAX call
	 * @param container
	 *            {@link WebMarkupContainer} containing all files
	 * @param model
	 *            {@link IModel} containing children (will be detached to force
	 *            rerendering)
	 */
	private void resort(AjaxFallbackLink<SortType> sortLink, AjaxRequestTarget target, WebMarkupContainer container, IModel<?> model) {
		if (sortLink != null) {
			LocalBrowserPanel.this.sortType = sortLink.getModelObject();
		}
		model.detach();
		if (target != null) {
			target.add(container);
		}
	}

	/**
	 * Is called when a {@link File} is selected to upload
	 * 
	 * @param path
	 *            of file to upload
	 * @param target
	 *            if request is AJAX
	 */
	public abstract void fileSelected(String path, AjaxRequestTarget target);

}
