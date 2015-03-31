package org.openntf.conferenceapp.ui.pages;

import java.util.Date;

import org.openntf.conference.graph.Attendee;
import org.openntf.conference.graph.Location;
import org.openntf.conference.graph.Presentation;
import org.openntf.conference.graph.TimeSlot;
import org.openntf.conference.graph.Track;
import org.openntf.conferenceapp.service.ConferenceGraphFactory;

import com.tinkerpop.frames.FramedGraph;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;

public class PresentationsContainer {

	// Create the container
	public IndexedContainer container;
	public Boolean removeDesc = true;

	public PresentationsContainer() {

		loadData();
	}

	public PresentationsContainer(boolean removeDesc) {

		setRemoveDesc(false);
		loadData();
	}

	private Boolean getRemoveDesc() {
		return removeDesc;
	}

	private void setRemoveDesc(Boolean removeDesc) {
		this.removeDesc = removeDesc;
	}

	private void loadData() {
		// Define the properties (columns)
		container = new IndexedContainer();
		container.addContainerProperty("SessionID", String.class, "");
		container.addContainerProperty("Title", String.class, "");
		container.addContainerProperty("Track", String.class, "");
		container.addContainerProperty("Speakers", String.class, "");
		container.addContainerProperty("Day", String.class, "");
		container.addContainerProperty("StartTime", Date.class, "");
		container.addContainerProperty("EndTime", Date.class, "");
		container.addContainerProperty("Location", String.class, "");
		container.addContainerProperty("Description", String.class, "");

		FramedGraph graph = ConferenceGraphFactory.getGraph("engage");
		Iterable<Presentation> presentations = graph.getVertices(null, null, Presentation.class);
		for (Presentation pres : presentations) {
			Item newItem = container.getItem(container.addItem());
			newItem.getItemProperty("SessionID").setValue(pres.getSessionId());
			newItem.getItemProperty("Title").setValue(pres.getTitle());
			Track track = pres.getIncludedInTracks().iterator().next();
			newItem.getItemProperty("Track").setValue(track.getTitle());
			Iterable<Attendee> speakers = pres.getPresentingAttendees();
			String speakerNames = "";
			for (Attendee att : speakers) {
				if ("".equals(speakerNames)) {
					speakerNames = att.getFirstName() + " " + att.getLastName();
				} else {
					speakerNames += ", " + att.getFirstName() + " " + att.getLastName();
				}
			}
			newItem.getItemProperty("Speakers").setValue(speakerNames);
			TimeSlot ts = pres.getTimes().iterator().next();
			newItem.getItemProperty("Day").setValue(ts.getDay());
			newItem.getItemProperty("StartTime").setValue(ts.getStartTime().getTime());
			newItem.getItemProperty("EndTime").setValue(ts.getEndTime().getTime());
			Location loc = pres.getLocations().iterator().next();
			newItem.getItemProperty("Location").setValue(loc.getName());
			newItem.getItemProperty("Description").setValue(pres.getDescription());
		}

		if (getRemoveDesc()) {
			container.removeContainerProperty("Description");
		}
	}

	public void filterGrid(String type, String value) {
		if ("All".equals(value)) {
			container.removeContainerFilters(type);
		} else {
			container.removeContainerFilters(type);
			container.addContainerFilter(type, value, false, false);
		}
	}

	public IndexedContainer getContainer() {
		return container;
	}

	public void setContainer(IndexedContainer container) {
		this.container = container;
	}

}
