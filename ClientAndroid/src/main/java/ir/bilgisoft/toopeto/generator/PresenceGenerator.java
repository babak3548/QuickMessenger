package ir.bilgisoft.toopeto.generator;

import ir.bilgisoft.toopeto.json.Enums;
import ir.bilgisoft.toopeto.json.UserPacket;

public class PresenceGenerator extends ir.bilgisoft.toopeto.generator.AbstractGenerator {

	public PresenceGenerator(ir.bilgisoft.toopeto.services.XmppConnectionService service) {
		super(service);
	}

	private UserPacket subscription(String type, ir.bilgisoft.toopeto.entities.Contact contact) {
		UserPacket packet = new UserPacket();
        packet.to=contact.getJid().getLocalpart();
        packet.type= Enums.UserTypeEnum.presence.toString();
		return packet;
	}

	public UserPacket requestPresenceUpdatesFrom(ir.bilgisoft.toopeto.entities.Contact contact) {
		return subscription("subscribe", contact);
	}

	public UserPacket stopPresenceUpdatesFrom(ir.bilgisoft.toopeto.entities.Contact contact) {
		return subscription("unsubscribe", contact);
	}

	public UserPacket stopPresenceUpdatesTo(ir.bilgisoft.toopeto.entities.Contact contact) {
		return subscription("unsubscribed", contact);
	}

	public UserPacket sendPresenceUpdatesTo(ir.bilgisoft.toopeto.entities.Contact contact) {
		return subscription("subscribed", contact);
	}

	public UserPacket sendPresence(ir.bilgisoft.toopeto.entities.Account account) {
		UserPacket packet = new UserPacket();
		packet.to= account.getJid().getLocalpart();
		String sig = account.getPgpSignature();
		if (sig != null) {
			packet.Signature=sig;
		}
        /*
		String capHash = getCapHash();
		if (capHash != null) {
			ir.bilgisoft.toopeto.xml.Element cap = packet.addChild("c",
					"http://jabber.org/protocol/caps");
			cap.setAttribute("hash", "sha-1");
			cap.setAttribute("node", "http://conversions.siacs.eu");
			cap.setAttribute("ver", capHash);
		} */
		return packet;
	}
}