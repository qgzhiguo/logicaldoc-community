package com.logicaldoc.gui.frontend.client.tenant;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.logicaldoc.gui.common.client.DefaultAsyncCallback;
import com.logicaldoc.gui.common.client.IgnoreAsyncCallback;
import com.logicaldoc.gui.common.client.beans.GUIKeystore;
import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.util.ItemFactory;
import com.logicaldoc.gui.common.client.widgets.Upload;
import com.logicaldoc.gui.frontend.client.services.DocumentService;
import com.logicaldoc.gui.frontend.client.services.SignService;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * This popup window is used to upload a new workflow schema to the server.
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 6.3
 */
public class KeystoreUploader extends Window {

	private Upload uploader;

	private IButton submitButton;

	private VLayout layout = new VLayout();

	private TenantKeystorePanel keystorePanel;

	private ValuesManager vm;

	private DynamicForm form;

	public KeystoreUploader(TenantKeystorePanel keystorePanel) {
		setHeaderControls(HeaderControls.HEADER_LABEL, HeaderControls.CLOSE_BUTTON);
		this.keystorePanel = keystorePanel;
		setTitle(I18N.message("uploadkeystore"));
		setMinWidth(420);
		setCanDragResize(true);
		setIsModal(true);
		setShowModalMask(true);
		centerInPage();
		setAutoWidth();

		layout.setMembersMargin(2);
		layout.setMargin(2);

		submitButton = new IButton(I18N.message("submit"));
		submitButton.addClickHandler(event -> onSubmit());

		prepareForm();

		layout.addMember(form);

		uploader = new Upload(submitButton);
		layout.addMember(uploader);
		layout.addMember(submitButton);
		addItem(layout);

		// Cleanup the upload folder
		DocumentService.Instance.get().cleanUploadedFileFolder(new IgnoreAsyncCallback<>());
	}

	private void prepareForm() {
		form = new DynamicForm();
		form.setWidth100();
		form.setAlign(Alignment.LEFT);
		form.setColWidths("1px, 100%");
		vm = new ValuesManager();
		form.setValuesManager(vm);

		TextItem localCAalias = ItemFactory.newSimpleTextItem("localCAalias", "localcaalias",
				keystorePanel.getKeystore() != null ? keystorePanel.getKeystore().getOrganizationAlias() : null);
		localCAalias.setRequired(true);
		localCAalias.setSelectOnFocus(true);
		localCAalias.setWrapTitle(false);

		TextItem password = ItemFactory.newPasswordItem("password", "keystorepasswd",
				keystorePanel.getKeystore() != null ? keystorePanel.getKeystore().getPassword() : null);
		password.setRequired(false);
		password.setWrapTitle(false);

		form.setItems(localCAalias, password);
	}

	public void onSubmit() {
		if (uploader.getUploadedFile() == null) {
			SC.warn(I18N.message("filerequired"));
			return;
		}

		if (Boolean.FALSE.equals(vm.validate()))
			return;

		GUIKeystore keystore = new GUIKeystore();
		if (keystorePanel.getKeystore() != null)
			keystore = keystorePanel.getKeystore();

		keystore.setOrganizationAlias(vm.getValueAsString("localCAalias"));
		keystore.setPassword(vm.getValueAsString("password"));
		keystore.setTenantId(keystorePanel.getTenantId());

		SignService.Instance.get().imporKeystore(keystore, new DefaultAsyncCallback<>() {
			@Override
			public void onSuccess(Void arg) {
				keystorePanel.initGUI();

				// Cleanup the upload folder
				DocumentService.Instance.get().cleanUploadedFileFolder(new AsyncCallback<>() {

					@Override
					public void onFailure(Throwable caught) {
						destroy();
					}

					@Override
					public void onSuccess(Void result) {
						destroy();
					}
				});
			}
		});
	}
	
	@Override
	public boolean equals(Object other) {
		return super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}