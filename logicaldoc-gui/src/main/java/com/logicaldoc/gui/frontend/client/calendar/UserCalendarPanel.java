package com.logicaldoc.gui.frontend.client.calendar;

import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.logicaldoc.gui.common.client.Feature;
import com.logicaldoc.gui.common.client.Menu;
import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.log.GuiLog;
import com.logicaldoc.gui.common.client.util.LD;
import com.logicaldoc.gui.frontend.client.google.GoogleApiAuthorization;
import com.logicaldoc.gui.frontend.client.google.GoogleService;
import com.logicaldoc.gui.frontend.client.google.GoogleUtil;
import com.smartgwt.client.types.ViewName;
import com.smartgwt.client.widgets.calendar.Calendar;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;

/**
 * Calendar dashboard that displays the events in which the user is involved
 * into.
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 6.7
 */
public class UserCalendarPanel extends VLayout {

	protected Calendar calendar = null;

	private static UserCalendarPanel instance;

	private Date choosenDate = null;

	private ViewName choosenView = null;

	public static UserCalendarPanel get() {
		if (instance == null)
			instance = new UserCalendarPanel();
		return instance;
	}

	public UserCalendarPanel() {
		setWidth100();
		setHeight100();
		setMembersMargin(3);
	}

	@Override
	public void onDraw() {
		ToolStrip toolStrip = new ToolStrip();
		toolStrip.setHeight(20);
		toolStrip.setWidth100();
		toolStrip.addSpacer(2);

		ToolStripButton refresh = new ToolStripButton();
		refresh.setTitle(I18N.message("refresh"));
		refresh.addClickHandler(click -> refresh());
		toolStrip.addButton(refresh);

		if (Feature.enabled(Feature.GOOGLE_CALENDAR) && Menu.enabled(Menu.GOOGLE_CALENDAR)) {
			ToolStripButton synchronize = new ToolStripButton();
			synchronize.setTitle(I18N.message("synchronize"));
			synchronize.addClickHandler(click -> synchronize());
			toolStrip.addButton(synchronize);

			ToolStripButton authorize = new ToolStripButton();
			authorize.setTitle(I18N.message("authorize"));
			authorize.addClickHandler(click -> GoogleApiAuthorization.get().show());
			toolStrip.addButton(authorize);
		}

		toolStrip.addFill();
		addMember(toolStrip);

		initCalendar();
	}

	private void initCalendar() {
		calendar = new EventsCalendar(null, choosenDate, new AsyncCallback<>() {

			@Override
			public void onFailure(Throwable caught) {
				GuiLog.serverError(caught);
			}

			@Override
			public void onSuccess(Void arg0) {
				refresh();
			}
		});
		calendar.setChosenDate(choosenDate);
		calendar.setCurrentViewName(choosenView);
		addMember(calendar);
	}

	public void synchronize() {
		LD.contactingServer();
		GoogleService.Instance.get().synchronizeCalendar(new AsyncCallback<Void>() {
			@Override
			public void onFailure(Throwable caught) {
				GoogleUtil.handleGoogleServiceError(caught);
			}

			@Override
			public void onSuccess(Void arg0) {
				LD.clearPrompt();
				refresh();
			}
		});
	}

	public void refresh() {
		if (calendar != null) {
			removeMember(calendar);
			choosenDate = calendar.getChosenDate();
			choosenView = calendar.getCurrentViewName();
		}
		initCalendar();
	}
}