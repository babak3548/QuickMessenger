package ir.bilgisoft.toopeto.parser;

import ir.bilgisoft.toopeto.entities.Conversation;
import ir.bilgisoft.toopeto.json.Enums;
import ir.bilgisoft.toopeto.json.UserPacket;

public class PresenceParser extends ir.bilgisoft.toopeto.parser.AbstractParser implements
        ir.bilgisoft.toopeto.xmpp.OnPresencePacketReceived {

	public PresenceParser(ir.bilgisoft.toopeto.services.XmppConnectionService service) {
		super(service);
	}

	public void parseConferencePresence(UserPacket packet, ir.bilgisoft.toopeto.entities.Account account) {
		//ir.bilgisoft.toopeto.crypto.PgpEngine mPgpEngine = mXmppConnectionService.getPgpEngine();
		final Conversation conversation = packet.from== null ? null : mXmppConnectionService.find(account, packet.from);
		if (conversation != null) {
			final ir.bilgisoft.toopeto.entities.MucOptions mucOptions = conversation.getMucOptions();
			boolean before = mucOptions.online();
			int count = mucOptions.getUsers().size();
		//	mucOptions.processPacket(packet, mPgpEngine);
			mucOptions.processPacket(packet, null);
			mXmppConnectionService.getAvatarService().clear(conversation);
			if (before != mucOptions.online() || (mucOptions.online() && count != mucOptions.getUsers().size())) {
				mXmppConnectionService.updateConversationUi();
			} else if (mucOptions.online()) {
				mXmppConnectionService.updateMucRosterUi();
			}
		}
	}

	//public void parseContactPresence(ir.bilgisoft.toopeto.xmpp.stanzas.PresencePacket packet, ir.bilgisoft.toopeto.entities.Account account) {
	public void parseContactPresence(UserPacket packet, ir.bilgisoft.toopeto.entities.Account account) {
        ir.bilgisoft.toopeto.generator.PresenceGenerator mPresenceGenerator = mXmppConnectionService
                .getPresenceGenerator();
        if (packet.from == null) {
            return;
        }
        final String from = packet.from;
        String type = packet.type;
        ir.bilgisoft.toopeto.entities.Contact contact = account.getRoster().getContact(packet.from);
        if (type == null) {
            String presence;
            if (from != null) {
                presence = "mobile";//from.getResourcepart();
            } else {
                presence = "";
            }
            int sizeBefore = contact.getPresences().size();
            contact.updatePresence(presence,
                    ir.bilgisoft.toopeto.entities.Presences.parseShow(null));
            //ir.bilgisoft.toopeto.entities.Presences.parseShow(packet.findChild("show")));
			/*	ir.bilgisoft.toopeto.crypto.PgpEngine pgp = mXmppConnectionService.getPgpEngine();
		if (pgp != null) {
				ir.bilgisoft.toopeto.xml.Element x = packet.findChild("x", "jabber:x:signed");
				if (x != null) {
					ir.bilgisoft.toopeto.xml.Element status = packet.findChild("status");
					String msg;
					if (status != null) {
						msg = status.getContent();
					} else {
						msg = "";
					}
					contact.setPgpKeyId(pgp.fetchKeyId(account, msg,
							x.getContent()));
				}
			}*/
            boolean online = true;//sizeBefore < contact.getPresences().size();
            updateLastseen(packet, account, false);
            mXmppConnectionService.onContactStatusChanged.onContactStatusChanged(contact, online);
        } else if (type.equals("unavailable")) {
            //if (from.isBareJid()) {
            if (true) {
                contact.clearPresences();
            } else {
                contact.removePresence("moblie");
            }
            mXmppConnectionService.onContactStatusChanged
                    .onContactStatusChanged(contact, false);
            mXmppConnectionService.sendPresencePacket(account);
		/*} else if (type.equals("subscribe")) {
			if (contact.getOption(ir.bilgisoft.toopeto.entities.Contact.Options.PREEMPTIVE_GRANT)) {
				mXmppConnectionService.sendPresencePacket(account,
						mPresenceGenerator.sendPresenceUpdatesTo(contact));
			} else {
				contact.setOption(ir.bilgisoft.toopeto.entities.Contact.Options.PENDING_SUBSCRIPTION_REQUEST);
			}
		}*/
	/*	ir.bilgisoft.toopeto.xml.Element nick = packet.findChild("nick",
				"http://jabber.org/protocol/nick");
		if (nick != null) {
			contact.setPresenceName(nick.getContent());
		}*/
            mXmppConnectionService.updateRosterUi();
        }
    }
	@Override
	public void onPresencePacketReceived(ir.bilgisoft.toopeto.entities.Account account, UserPacket packet) {
		if (packet.receiver == Enums.ReceiverEnum.room.toString()) {
			this.parseConferencePresence(packet, account);
		} else {
			this.parseContactPresence(packet, account);
		}
	}

}
