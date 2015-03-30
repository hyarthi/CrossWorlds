package org.openntf.conferenceapp.ui.pages;

import java.text.DateFormatSymbols;
import java.util.Date;
import java.util.Iterator;

import org.openntf.conference.graph.Location;
import org.openntf.conference.graph.Track;
import org.openntf.conferenceapp.service.LocationFactory;
import org.openntf.conferenceapp.service.TrackFactory;
import org.openntf.conferenceapp.ui.ConferenceUI;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class SessionsFilter extends VerticalLayout implements View {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String VIEW_NAME = "SessionsFilter";
	public static final String VIEW_DESC = "Sessions By...";
	private PresentationsContainer presentations;
	private VerticalLayout details;
	private VerticalLayout main = new VerticalLayout();
	MenuBar menubar = new MenuBar();
	MenuItem tracks;
	MenuItem locations;
	MenuItem days;

	public SessionsFilter() {
		main.setSpacing(true);

		menubar.setStyleName(ValoTheme.MENU_SUBTITLE);
		menubar.setWidth(100, Unit.PERCENTAGE);
		menubar.addItem("Sessions By...", null);
		tracks = menubar.addItem("Filter by Track", null, null);
		MenuItem allTracks = tracks.addItem("All", trackFilterCommand);
		allTracks.setStyleName("highlight");
		for (Track track : TrackFactory.getTracksSortedByProperty("")) {
			MenuItem t = tracks.addItem(track.getDescription(), getIcon(track.getTitle()), trackFilterCommand);
		}
		locations = menubar.addItem("Filter by Location", null, null);
		MenuItem allLocs = locations.addItem("All", locationFilterCommand);
		allLocs.setStyleName("highlight");
		for (Location loc : LocationFactory.getLocationsSortedByProperty("")) {
			MenuItem l = locations.addItem(loc.getName(), locationFilterCommand);
		}
		days = menubar.addItem("Filter by Date", null);
		MenuItem allDays = days.addItem("All", dayFilterCommand);
		allDays.setStyleName("highlight");
		days.addItem("30 Mar", dayFilterCommand);
		days.addItem("31 Mar", dayFilterCommand);

		addComponent(menubar);

	}

	public void loadContent() {
		try {

			presentations = new PresentationsContainer();

			loadData();

			main.addComponent(details);
			main.setComponentAlignment(details, Alignment.TOP_CENTER);
			addComponent(main);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadData() {
		String catLabel = "";
		DateFormatSymbols s = new DateFormatSymbols(UI.getCurrent().getLocale());
		String[] days = s.getShortWeekdays();

		details = new VerticalLayout();
		// details.removeAllComponents();

		details.setWidth(95, Unit.PERCENTAGE);
		IndexedContainer table = presentations.getContainer();
		table.sort(new Object[] { "StartTime" }, new boolean[] { true });

		// Iterate over the item identifiers of the table.
		for (Iterator i = table.getItemIds().iterator(); i.hasNext();) {
			// Get the current item identifier, which is an integer.
			int iid = (Integer) i.next();

			// Now get the actual item from the
			Item item = table.getItem(iid);

			Date sTime = (Date) item.getItemProperty("StartTime").getValue();
			Date eTime = (Date) item.getItemProperty("EndTime").getValue();
			String tmpCatLabel = days[sTime.getDay() + 1] + " " + ConferenceUI.TIME_FORMAT.format(sTime) + "-"
					+ ConferenceUI.TIME_FORMAT.format(eTime);
			if (!tmpCatLabel.equals(catLabel)) {
				Label cat = new Label(tmpCatLabel);
				cat.setStyleName(ValoTheme.LABEL_H3);
				details.addComponent(cat);
				catLabel = tmpCatLabel;
			}

			HorizontalLayout sessionDetails = new HorizontalLayout();

			// Add track
			Label trackLabel = new Label();
			trackLabel.setDescription((String) item.getItemProperty("Track").getValue());
			trackLabel.setValue(getTrackHtml((String) item.getItemProperty("Track").getValue()));
			trackLabel.setWidth(25, Unit.PIXELS);
			trackLabel.setContentMode(ContentMode.HTML);
			sessionDetails.addComponent(trackLabel);

			VerticalLayout sessionSummary = new VerticalLayout();
			sessionSummary.setWidth(100, Unit.PERCENTAGE);
			Label title = new Label(item.getItemProperty("SessionID").getValue() + " - " + item.getItemProperty("Title").getValue());
			sessionSummary.addComponent(title);
			sessionSummary.addComponent(new Label(item.getItemProperty("Speakers").getValue() + " (" + item.getItemProperty("Location").getValue()
					+ ")"));
			sessionDetails.addComponent(sessionSummary);
			sessionDetails.setExpandRatio(sessionSummary, 1);
			details.addComponent(sessionDetails);
		}

	}

	public String getTrackHtml(String trackCode) {
		String iconCode;
		if ("Sp".equals(trackCode)) {
			iconCode = "<span class=\"v-icon\" style=\"font-family: " + FontAwesome.STAR.getFontFamily() + ";font-size:20px;color:#FFE118\">&#x"
					+ Integer.toHexString(FontAwesome.STAR.getCodepoint()) + ";</span>";
		} else if ("Str".equals(trackCode)) {
			iconCode = "<span class=\"v-icon\" style=\"font-family: " + FontAwesome.QUESTION_CIRCLE.getFontFamily()
					+ ";font-size:20px;color:#B8CCE4\">&#x" + Integer.toHexString(FontAwesome.QUESTION_CIRCLE.getCodepoint()) + ";</span>";
		} else if ("Bus".equals(trackCode)) {
			iconCode = "<span class=\"v-icon\" style=\"font-family: " + FontAwesome.BRIEFCASE.getFontFamily() + ";font-size:20px;color:#FFC7CE\">&#x"
					+ Integer.toHexString(FontAwesome.BRIEFCASE.getCodepoint()) + ";</span>";
		} else if ("Adm".equals(trackCode)) {
			iconCode = "<span class=\"v-icon\" style=\"font-family: " + FontAwesome.TERMINAL.getFontFamily() + ";font-size:20px;color:#C6EFCE\">&#x"
					+ Integer.toHexString(FontAwesome.TERMINAL.getCodepoint()) + ";</span>";
		} else if ("Dev".equals(trackCode)) {
			iconCode = "<span class=\"v-icon\" style=\"font-family: " + FontAwesome.STACK_OVERFLOW.getFontFamily()
					+ ";font-size:20px;color:#FFEB9C\">&#x" + Integer.toHexString(FontAwesome.STACK_OVERFLOW.getCodepoint()) + ";</span>";
		} else if ("Comm".equals(trackCode)) {
			iconCode = "<span class=\"v-icon\" style=\"font-family: " + FontAwesome.EURO.getFontFamily() + ";font-size:20px;color:#FFEB9C\">&#x"
					+ Integer.toHexString(FontAwesome.EURO.getCodepoint()) + ";</span>";
		} else {
			iconCode = "<span class=\"v-icon\" style=\"font-family: " + FontAwesome.CIRCLE.getFontFamily() + ";font-size:20px;color:#FFFFFF\">&#x"
					+ Integer.toHexString(FontAwesome.CIRCLE.getCodepoint()) + ";</span>";
		}
		return iconCode;
	}

	public Resource getIcon(String trackCode) {
		Resource iconCode;
		if ("Sp".equals(trackCode)) {
			iconCode = FontAwesome.STAR;
		} else if ("Str".equals(trackCode)) {
			iconCode = FontAwesome.QUESTION_CIRCLE;
		} else if ("Bus".equals(trackCode)) {
			iconCode = FontAwesome.BRIEFCASE;
		} else if ("Adm".equals(trackCode)) {
			iconCode = FontAwesome.TERMINAL;
		} else if ("Dev".equals(trackCode)) {
			iconCode = FontAwesome.STACK_OVERFLOW;
		} else if ("Comm".equals(trackCode)) {
			iconCode = FontAwesome.EURO;
		} else {
			iconCode = FontAwesome.CIRCLE;
		}
		return iconCode;
	}

	MenuBar.Command trackFilterCommand = new MenuBar.Command() {
		MenuItem previous = null;

		public void menuSelected(MenuItem selectedItem) {
			String trackCode = "All";
			for (Track t : TrackFactory.getTracksSortedByProperty("")) {
				if (selectedItem.getText().equals(t.getDescription())) {
					trackCode = t.getTitle();
				}
			}

			presentations.filterGrid("Track", trackCode);
			main.removeComponent(details);
			loadData();
			main.addComponent(details);
			main.setComponentAlignment(details, Alignment.TOP_CENTER);

			if (previous == null) {
				previous = locations.getChildren().get(0);
			}

			previous.setStyleName(null);
			selectedItem.setStyleName("highlight");
			previous = selectedItem;
			if ("All".equals(selectedItem.getText())) {
				menubar.getItems().get(1).setStyleName(null);
			} else {
				menubar.getItems().get(1).setStyleName("highlight");
			}
		}
	};

	MenuBar.Command locationFilterCommand = new MenuBar.Command() {
		MenuItem previous = null;

		public void menuSelected(MenuItem selectedItem) {
			presentations.filterGrid("Location", selectedItem.getText());
			main.removeComponent(details);
			loadData();
			main.addComponent(details);
			main.setComponentAlignment(details, Alignment.TOP_CENTER);

			if (previous == null) {
				previous = locations.getChildren().get(0);
			}

			previous.setStyleName(null);
			selectedItem.setStyleName("highlight");
			previous = selectedItem;
			if ("All".equals(selectedItem.getText())) {
				menubar.getItems().get(2).setStyleName(null);
			} else {
				menubar.getItems().get(2).setStyleName("highlight");
			}
		}
	};

	MenuBar.Command dayFilterCommand = new MenuBar.Command() {
		MenuItem previous = null;

		public void menuSelected(MenuItem selectedItem) {
			presentations.filterGrid("Day", selectedItem.getText());
			main.removeComponent(details);
			loadData();
			main.addComponent(details);
			main.setComponentAlignment(details, Alignment.TOP_CENTER);

			if (previous == null) {
				previous = locations.getChildren().get(0);
			}

			previous.setStyleName(null);
			selectedItem.setStyleName("highlight");
			previous = selectedItem;
			if ("All".equals(selectedItem.getText())) {
				menubar.getItems().get(3).setStyleName(null);
			} else {
				menubar.getItems().get(3).setStyleName("highlight");
			}
		}
	};

	public void showError(String msg) {
		Notification.show(msg, Type.ERROR_MESSAGE);
	}

	@Override
	public void enter(ViewChangeEvent event) {
		loadContent();
	}

}
