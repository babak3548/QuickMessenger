package ir.bilgisoft.toopeto.parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ir.bilgisoft.toopeto.json.Json;
import ir.bilgisoft.toopeto.json.MessagePacket;
import ir.bilgisoft.toopeto.json.UserPacket;

public abstract class AbstractParser {

	protected ir.bilgisoft.toopeto.services.XmppConnectionService mXmppConnectionService;

	protected AbstractParser(ir.bilgisoft.toopeto.services.XmppConnectionService service) {
		this.mXmppConnectionService = service;
	}

	protected long getTimestamp(ir.bilgisoft.toopeto.xml.Element packet) {
		long now = System.currentTimeMillis();
		ir.bilgisoft.toopeto.xml.Element delay = packet.findChild("delay");
		if (delay == null) {
			return now;
		}
		String stamp = delay.getAttribute("stamp");
		if (stamp == null) {
			return now;
		}
		try {
			long time = parseTimestamp(stamp).getTime();
			return now < time ? now : time;
		} catch (ParseException e) {
			return now;
		}
	}

	public static Date parseTimestamp(String timestamp) throws ParseException {
		timestamp = timestamp.replace("Z", "+0000");
		SimpleDateFormat dateFormat;
		if (timestamp.contains(".")) {
			dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
		} else {
			dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ",Locale.US);
		}
		return dateFormat.parse(timestamp);
	}

	protected void updateLastseen(final Json packet, final ir.bilgisoft.toopeto.entities.Account account,
			final boolean presenceOverwrite) {
		final String from = packet.from;
		//final String presence = from == null || from.isBareJid() ? "" : from.getResourcepart();
		final String presence = from == null  ? "" : "mobile";
		final ir.bilgisoft.toopeto.entities.Contact contact = account.getRoster().getContact(from);
	//	final long timestamp = getTimestamp(packet);
		//if (timestamp >= contact.lastseen.time) {
			//contact.lastseen.time = timestamp;
			//if (!presence.isEmpty() && presenceOverwrite) {
				contact.lastseen.presence = presence;
			//}
	//	}
	}

	protected String avatarData(ir.bilgisoft.toopeto.xml.Element items) {
		ir.bilgisoft.toopeto.xml.Element item = items.findChild("item");
		if (item == null) {
			return null;
		}
		ir.bilgisoft.toopeto.xml.Element data = item.findChild("data", "urn:xmpp:avatar:data");
		if (data == null) {
			return null;
		}
		return data.getContent();
	}
}
