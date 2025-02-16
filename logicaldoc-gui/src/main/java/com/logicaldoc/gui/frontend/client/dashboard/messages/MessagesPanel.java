package com.logicaldoc.gui.frontend.client.dashboard.messages;

import com.logicaldoc.gui.common.client.DefaultAsyncCallback;
import com.logicaldoc.gui.common.client.Session;
import com.logicaldoc.gui.common.client.beans.GUIMessage;
import com.logicaldoc.gui.common.client.beans.GUIUser;
import com.logicaldoc.gui.common.client.controllers.UserController;
import com.logicaldoc.gui.common.client.controllers.UserObserver;
import com.logicaldoc.gui.common.client.data.MessagesDS;
import com.logicaldoc.gui.common.client.grid.DateListGridField;
import com.logicaldoc.gui.common.client.grid.IdListGridField;
import com.logicaldoc.gui.common.client.grid.RefreshableListGrid;
import com.logicaldoc.gui.common.client.grid.UserListGridField;
import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.util.GridUtil;
import com.logicaldoc.gui.common.client.util.LD;
import com.logicaldoc.gui.frontend.client.services.MessageService;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;

/**
 * This panel shows the list of system messages and allows the selection.
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 6.0
 */
public class MessagesPanel extends VLayout implements UserObserver {

	private static final String SUBJECT = "subject";

	private RefreshableListGrid grid;

	private HTMLPane body;

	public MessagesPanel() {
		setWidth100();
		setHeight100();
		UserController.get().addObserver(this);
	}

	@Override
	public void onDraw() {
		Layout listing = new VLayout();
		listing.setHeight("75%");
		listing.setShowResizeBar(true);

		ListGridField id = new IdListGridField();

		ListGridField priority = new MessagePriorityListGridField();

		ListGridField subject = new ListGridField(SUBJECT, I18N.message(SUBJECT));
		subject.setCanFilter(true);

		UserListGridField from = new UserListGridField("from", "avatar", I18N.message("from"));
		from.setCanFilter(true);

		ListGridField sent = new DateListGridField("sent", "sent");

		grid = new RefreshableListGrid() {
			@Override
			protected String getCellCSSText(ListGridRecord rec, int rowNum, int colNum) {
				if (getFieldName(colNum).equals(SUBJECT)) {
					if ("false".equals(rec.getAttributeAsString("read"))) {
						return "font-weight:bold;";
					} else {
						return super.getCellCSSText(rec, rowNum, colNum);
					}
				} else {
					return super.getCellCSSText(rec, rowNum, colNum);
				}
			}
		};
		grid.setEmptyMessage(I18N.message("notitemstoshow"));
		grid.setShowRecordComponents(true);
		grid.setShowRecordComponentsByCell(true);
		grid.setCanFreezeFields(true);
		grid.setAutoFetchData(true);
		grid.setSelectionType(SelectionStyle.MULTIPLE);
		grid.setFilterOnKeypress(true);
		grid.setShowFilterEditor(false);
		grid.setDataSource(new MessagesDS());
		grid.setFields(id, priority, subject, from, sent);
		grid.sort("sent", SortDirection.DESCENDING);

		// Count the total unread messages
		grid.addDataArrivedHandler(event -> {
			Record[] records = grid.getRecordList().toArray();
			int unread = 0;
			for (Record rec : records) {
				if ("false".equals(rec.getAttributeAsString("read")))
					unread++;
			}

			Session.get().getUser().setUnreadMessages(unread);
		});

		grid.addSelectionChangedHandler(event -> {
			final Record rec = grid.getSelectedRecord();
			if (rec != null)
				MessageService.Instance.get().getMessage(Long.parseLong(rec.getAttributeAsString("id")), true,
						new DefaultAsyncCallback<>() {

							@Override
							public void onSuccess(GUIMessage message) {
								rec.setAttribute("read", "true");
								grid.refreshRow(grid.getRecordIndex(rec));
								body.setContents(grid.getSelectedRecord().getAttributeAsString("text"));
							}
						});
		});

		grid.addCellContextClickHandler(event -> {
			showContextMenu();
			event.cancel();
		});

		ToolStrip toolStrip = new ToolStrip();
		toolStrip.setHeight(20);
		toolStrip.setWidth100();
		toolStrip.addSpacer(2);
		ToolStripButton add = new ToolStripButton();
		add.setTitle(I18N.message("sendmessage"));
		toolStrip.addButton(add);
		add.addClickHandler(click -> new MessageDialog().show());
		ToolStripButton refresh = new ToolStripButton();
		refresh.setTitle(I18N.message("refresh"));
		toolStrip.addButton(refresh);
		refresh.addClickHandler(event -> refresh());
		toolStrip.addFill();

		listing.setMembers(toolStrip, grid);

		body = new HTMLPane();
		body.setContentsType(ContentsType.PAGE);
		body.setShowEdges(true);
		setMembers(listing, body);
	}

	private void refresh() {
		if (grid != null) {
			grid.refresh(new MessagesDS());
			body.setContents(" ");
		}
	}

	private void showContextMenu() {
		Menu contextMenu = new Menu();

		ListGridRecord[] selection = grid.getSelectedRecords();
		if (selection == null || selection.length == 0)
			return;

		MenuItem delete = new MenuItem();
		delete.setTitle(I18N.message("ddelete"));
		delete.addClickHandler(event -> LD.ask(I18N.message("question"), I18N.message("confirmdelete"), answer -> {
			if (Boolean.TRUE.equals(answer)) {
				MessageService.Instance.get().delete(GridUtil.getIds(selection), new DefaultAsyncCallback<>() {
					@Override
					public void onSuccess(Void result) {
						grid.removeSelectedData();
					}
				});
			}
		}));

		contextMenu.setItems(delete);
		contextMenu.showContextMenu();
	}

	@Override
	public void onUserChanged(GUIUser user) {
		refresh();
	}

	@Override
	public void onUserLogin(String username) {
		// Nothing to do
	}

	@Override
	public void onUserLogout(String username) {
		// Nothing to do
	}

	@Override
	public void destroy() {
		UserController.get().removeObserver(this);
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