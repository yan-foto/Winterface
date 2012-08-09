package freenet.winterface.web.markup;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

import freenet.winterface.web.core.AjaxFallbackCssButton;

@SuppressWarnings("serial")
public class LocalBrowserPanel extends Panel {
	
	private final static String BREAD_CRUMB_SEPARATOR = "»";

	public LocalBrowserPanel(String id, IModel<String> model) {
		super(id, model);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		// Parent container needed for AJAX refresh
		final WebMarkupContainer mainContainer = new WebMarkupContainer("mainContainer");
		mainContainer.setOutputMarkupId(true);
		add(mainContainer);
		// Current path
		LoadableDetachableModel<String> currentDirModel = new LoadableDetachableModel<String>() {
			@Override
			protected String load() {
				final String startPath = LocalBrowserPanel.this.getDefaultModelObjectAsString();
				startPath.replaceAll(File.pathSeparator, BREAD_CRUMB_SEPARATOR);
				return startPath;
			}
		};
		Label currentDir = new Label("currentDir",currentDirModel);
		mainContainer.add(currentDir);
		// Form to fetch selected items
		final Form<Void> filesForm = new Form<Void>("filesForm");
		// List of files and directories in current path
		LoadableDetachableModel<List<File>> childrenModel = new LoadableDetachableModel<List<File>>() {
			@Override
			protected List<File> load() {
				String startPath = LocalBrowserPanel.this.getDefaultModelObjectAsString();
				System.out.println("PATH IS : "+startPath);
				final File start = new File(startPath);
				return Arrays.asList(start.listFiles());
			}
		};
		ListView<File> childrenList = new ListView<File>("children",childrenModel) {
			@Override
			protected void populateItem(final ListItem<File> item) {
				// Selection checkbox
				final CheckBox selectFile = new CheckBox("selectFile",Model.of(false));
				item.add(selectFile);
				// TODO make AjaxFallbackCSSButton icons available for all components
				String imgSrc;
				if(item.getModelObject().isDirectory()) {
					imgSrc = "folder";
				} else {
					imgSrc = "file";
				}
				item.add(new Image("fileType",new PackageResourceReference(AjaxFallbackCssButton.class, "img/"+imgSrc+".png")));
				// Link when clicked on item
				AjaxFallbackLink<Void> fileLink = new AjaxFallbackLink<Void>("link") {
					@Override
					public void onClick(AjaxRequestTarget target) {
						if(item.getModelObject().isDirectory()) {
							try {
								item.findParent(LocalBrowserPanel.class).setDefaultModel(Model.of(item.getModelObject().getCanonicalPath()));
							} catch (IOException e) {
								// shouldn't happen
								e.printStackTrace();
							}
						} else {
							// Toggle the selection
							selectFile.setModel(Model.of(!selectFile.getModelObject()));
							selectFile.modelChanged();
						}
						if(target!=null) {
							target.add(mainContainer);
						}
					}
				};
				// Link Label
				fileLink.setBody(Model.of(item.getModelObject().getName()));
				item.add(fileLink);
			}
		};
		// Always to true when used inside a form
		childrenList.setReuseItems(true);
		filesForm.add(childrenList);
		mainContainer.add(filesForm);
	}

}
