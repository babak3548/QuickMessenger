package ir.bilgisoft.toopeto.parser;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;

import ir.bilgisoft.toopeto.entities.Account;
import ir.bilgisoft.toopeto.json.Enums;
import ir.bilgisoft.toopeto.json.Json;
import ir.bilgisoft.toopeto.json.ListTransferPacket;
import ir.bilgisoft.toopeto.json.UserPacket;
import ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket;

public class IqParser extends AbstractParser implements ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived {
///////////////my code ////////////////
	public IqParser(final ir.bilgisoft.toopeto.services.XmppConnectionService service) {
		super(service);
	}
    @Override
    //list block shodehha connection jingo ya http roster ya list goftegoha  bayad dorostr gardd
    public void onIqPacketReceived(final Account account, final  String jsonString) {
        Json packet= Json.GetJson(jsonString);
        if(packet.type.equals(Enums.ListTransferEnum.getContact.toString())){
            ListTransferPacket listTransferPacket=  ListTransferPacket.GetListTransferPacket(jsonString);
            this.rosterItems(account, listTransferPacket);
        }
        else {
            Log.d("error :","Iqparser class cannot parse");
        }
        /*
		if (packet.hasChild("query", ir.bilgisoft.toopeto.utils.Xmlns.ROSTER) && packet.fromServer(account)) {
			final ir.bilgisoft.toopeto.xml.Element query = packet.findChild("query");
			// If this is in response to a query for the whole roster:
			if (packet.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.RESULT) {
				account.getRoster().markAllAsNotInRoster();
			}
			this.rosterItems(account, query);
		} else if ((packet.hasChild("block", ir.bilgisoft.toopeto.utils.Xmlns.BLOCKING) || packet.hasChild("blocklist", ir.bilgisoft.toopeto.utils.Xmlns.BLOCKING)) &&
				packet.fromServer(account)) {
			// Block list or block push.
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "Received blocklist update from server");
			final ir.bilgisoft.toopeto.xml.Element blocklist = packet.findChild("blocklist", ir.bilgisoft.toopeto.utils.Xmlns.BLOCKING);
			final ir.bilgisoft.toopeto.xml.Element block = packet.findChild("block", ir.bilgisoft.toopeto.utils.Xmlns.BLOCKING);
			final Collection<ir.bilgisoft.toopeto.xml.Element> items = blocklist != null ? blocklist.getChildren() :
				(block != null ? block.getChildren() : null);
			// If this is a response to a blocklist query, clear the block list and replace with the new one.
			// Otherwise, just update the existing blocklist.
			if (packet.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.RESULT) {
				account.clearBlocklist();
				account.getXmppConnection().getFeatures().setBlockListRequested(true);
			}
			if (items != null) {
				final Collection<ir.bilgisoft.toopeto.xmpp.jid.Jid> jids = new ArrayList<>(items.size());
				// Create a collection of Jids from the packet
				for (final ir.bilgisoft.toopeto.xml.Element item : items) {
					if (item.getName().equals("item")) {
						final ir.bilgisoft.toopeto.xmpp.jid.Jid jid = item.getAttributeAsJid("jid");
						if (jid != null) {
							jids.add(jid);
						}
					}
				}
				account.getBlocklist().addAll(jids);
			}
			// Update the UI
			mXmppConnectionService.updateBlocklistUi(ir.bilgisoft.toopeto.xmpp.OnUpdateBlocklist.Status.BLOCKED);
		} else if (packet.hasChild("unblock", ir.bilgisoft.toopeto.utils.Xmlns.BLOCKING) &&
				packet.fromServer(account) && packet.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.SET) {
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "Received unblock update from server");
			final Collection<ir.bilgisoft.toopeto.xml.Element> items = packet.findChild("unblock", ir.bilgisoft.toopeto.utils.Xmlns.BLOCKING).getChildren();
			if (items.size() == 0) {
				// No children to unblock == unblock all
				account.getBlocklist().clear();
			} else {
				final Collection<ir.bilgisoft.toopeto.xmpp.jid.Jid> jids = new ArrayList<>(items.size());
				for (final ir.bilgisoft.toopeto.xml.Element item : items) {
					if (item.getName().equals("item")) {
						final ir.bilgisoft.toopeto.xmpp.jid.Jid jid = item.getAttributeAsJid("jid");
						if (jid != null) {
							jids.add(jid);
						}
					}
				}
				account.getBlocklist().removeAll(jids);
			}
			mXmppConnectionService.updateBlocklistUi(ir.bilgisoft.toopeto.xmpp.OnUpdateBlocklist.Status.UNBLOCKED);
		} else if (packet.hasChild("open", "http://jabber.org/protocol/ibb")
				|| packet.hasChild("data", "http://jabber.org/protocol/ibb")) {
			mXmppConnectionService.getJingleConnectionManager()
				.deliverIbbPacket(account, packet);
		} else if (packet.hasChild("query", "http://jabber.org/protocol/disco#info")) {
			final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket response = mXmppConnectionService.getIqGenerator()
				.discoResponse(packet);
		//	account.getXmppConnection().sendIqPacket(response, null);
		} else if (packet.hasChild("ping", "urn:xmpp:ping")) {
			final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket response = packet.generateResponse(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.RESULT);
			mXmppConnectionService.sendIqPacket(account, response, null);
		} else {
			if ((packet.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.GET)
					|| (packet.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.SET)) {
				final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket response = packet.generateResponse(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.ERROR);
				final ir.bilgisoft.toopeto.xml.Element error = response.addChild("error");
				error.setAttribute("type", "cancel");
				error.addChild("feature-not-implemented",
						"urn:ietf:params:xml:ns:xmpp-stanzas");
			//	account.getXmppConnection().sendIqPacket(response, null);
					}
		}
        */
    }

	private void rosterItems(final Account account, final ListTransferPacket transferPacket) {
	//	final String version = query.getAttribute("ver");
		//if (version != null) {
	   //account.getRoster().setVersion(version);
	//	}
		for (final String userPacketStr :transferPacket.list) {
            UserPacket userPacket=UserPacket.GetUserPacket(userPacketStr);
			//if (item.getName().equals("item")) {
			//	final ir.bilgisoft.toopeto.xmpp.jid.Jid jid = item.getAttributeAsJid("jid");
			//	if (jid == null) {
			//		continue;
			//	}
			//	final String name = item.getAttribute("name");
				//final String subscription = item.getAttribute("subscription");
				final ir.bilgisoft.toopeto.entities.Contact contact = account.getRoster().getContact(userPacket.userName);
			/*	if (!contact.getOption(ir.bilgisoft.toopeto.entities.Contact.Options.DIRTY_PUSH)) {
					contact.setServerName(name);
					contact.parseGroupsFromElement(item);
				} */
				/*if (subscription != null) {
					if (subscription.equals("remove")) {
						contact.resetOption(ir.bilgisoft.toopeto.entities.Contact.Options.IN_ROSTER);
						contact.resetOption(ir.bilgisoft.toopeto.entities.Contact.Options.DIRTY_DELETE);
						contact.resetOption(ir.bilgisoft.toopeto.entities.Contact.Options.PREEMPTIVE_GRANT);
					} else {
						contact.setOption(ir.bilgisoft.toopeto.entities.Contact.Options.IN_ROSTER);
						contact.resetOption(ir.bilgisoft.toopeto.entities.Contact.Options.DIRTY_PUSH);
						contact.parseSubscriptionFromElement(item);
					}
				}*/
			// in khat bayad ezafeh gardd
				mXmppConnectionService.getAvatarService().clear(contact);
			}
		//}
		mXmppConnectionService.updateConversationUi();
		mXmppConnectionService.updateRosterUi();
	}
//////////////////////end my code///////////////////
	public String avatarData(final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet) {
		final ir.bilgisoft.toopeto.xml.Element pubsub = packet.findChild("pubsub",
				"http://jabber.org/protocol/pubsub");
		if (pubsub == null) {
			return null;
		}
		final ir.bilgisoft.toopeto.xml.Element items = pubsub.findChild("items");
		if (items == null) {
			return null;
		}
		return super.avatarData(items);
	}

    public ir.bilgisoft.toopeto.xml.Element searchItemToMucItem(final ir.bilgisoft.toopeto.xml.Element searchItem) {
        String nameRoom;
        String jidRoom;
        final ir.bilgisoft.toopeto.xml.Element mucItem = new ir.bilgisoft.toopeto.xml.Element("item");

        ir.bilgisoft.toopeto.xml.Element searchFieldName  =searchItem.findChild("field","var","name");
        ir.bilgisoft.toopeto.xml.Element valueName=  searchFieldName.findChild("value");
        nameRoom=valueName.getContent();

        ir.bilgisoft.toopeto.xml.Element searchFieldJid  =searchItem.findChild("field","var","jid");
        ir.bilgisoft.toopeto.xml.Element valueJid=  searchFieldJid.findChild("value");
        jidRoom=valueJid.getContent();

        mucItem.setAttribute("xmlns","http://jabber.org/protocol/disco#items");
        mucItem.setAttribute("name",nameRoom);
        mucItem.setAttribute("jid",jidRoom);

        return mucItem;
    }
//eslah shavad
    public ir.bilgisoft.toopeto.xml.Element searchItemToUser(final ir.bilgisoft.toopeto.xml.Element searchItem) {
        String nameRoom;
        String jidRoom;
        final ir.bilgisoft.toopeto.xml.Element mucItem = new ir.bilgisoft.toopeto.xml.Element("item");

        ir.bilgisoft.toopeto.xml.Element searchFieldName  =searchItem.findChild("field","var","name");
        ir.bilgisoft.toopeto.xml.Element valueName=  searchFieldName.findChild("value");
        nameRoom=valueName.getContent();

        ir.bilgisoft.toopeto.xml.Element searchFieldJid  =searchItem.findChild("field","var","jid");
        ir.bilgisoft.toopeto.xml.Element valueJid=  searchFieldJid.findChild("value");
        jidRoom=valueJid.getContent();

        mucItem.setAttribute("xmlns","http://jabber.org/protocol/disco#items");
        mucItem.setAttribute("name",nameRoom);
        mucItem.setAttribute("jid",jidRoom);

        return mucItem;
    }


}
