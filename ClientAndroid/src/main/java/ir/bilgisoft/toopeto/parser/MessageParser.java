package ir.bilgisoft.toopeto.parser;

import net.java.otr4j.session.Session;
import net.java.otr4j.session.SessionStatus;

import ir.bilgisoft.toopeto.entities.Conversation;
import ir.bilgisoft.toopeto.entities.Message;
import ir.bilgisoft.toopeto.json.Enums;
import ir.bilgisoft.toopeto.json.Json;
import ir.bilgisoft.toopeto.json.MessagePacket;
import ir.bilgisoft.toopeto.json.UserPacket;
import ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException;
import ir.bilgisoft.toopeto.xmpp.jid.Jid;

public class MessageParser extends ir.bilgisoft.toopeto.parser.AbstractParser implements
        ir.bilgisoft.toopeto.xmpp.OnMessagePacketReceived {
    /////////////////////my code ///////////////////////////////
	public MessageParser(ir.bilgisoft.toopeto.services.XmppConnectionService service) {
		super(service);
	}
    @Override
    public void onMessagePacketReceived(ir.bilgisoft.toopeto.entities.Account account, MessagePacket packet) {
        Jid jid= Jid.fromParts(packet.from);
        Conversation conversation =mXmppConnectionService.findOrCreateConversation(account,jid,false);
        Message message =new Message(conversation,packet.data,Message.ENCRYPTION_NONE);
        if (!message.isRead()) {
            mXmppConnectionService.getNotificationService().push(message);
        }
        mXmppConnectionService.updateConversationUi();
        //     try {


        // ir.bilgisoft.toopeto.entities.Message message = null;
        //  this.parseNick(packet, account);
        //if ((packet.type == ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket.TYPE_CHAT || packet.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket.TYPE_NORMAL)) {
		/*if ((packet.receiver == Enums.ReceiverEnum.user.toString())) {
			if ((packet.data != null)
					&& (packet.getBody().startsWith("?OTR"))) {
				message = this.parseOtrChat(packet, account);
				if (message != null) {
					message.markUnread();
				}
			} else*/
       /*    if (packet.hasChild("body")) {
				message = this.parseChat(packet, account);
				if (message != null) {
					message.markUnread();
				}
			} else
            if (packet.hasChild("received", "urn:xmpp:carbons:2")
					|| (packet.hasChild("sent", "urn:xmpp:carbons:2"))) {
				message = this.parseCarbonMessage(packet, account);
				if (message != null) {
					if (message.getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_SEND) {
						account.activateGracePeriod();
						mXmppConnectionService.markRead(message.getConversation());
					} else {
						message.markUnread();
					}
				}
			} else
            //  if (packet.hasChild("result","urn:xmpp:mam:0")) {

            if (packet.type == Enums.MessageTypeEnum.text.toString()) {
                try {
                    message = parseMamMessage(packet, account);
                }
               catch ( InvalidJidException  e){}
                if (message != null) {
                    ir.bilgisoft.toopeto.entities.Conversation conversation = message.getConversation();
                    conversation.add(message);
                    mXmppConnectionService.databaseBackend.createMessage(message);
                    return;
                }


			} else if (packet.hasChild("fin","urn:xmpp:mam:0")) {
				ir.bilgisoft.toopeto.xml.Element fin = packet.findChild("fin","urn:xmpp:mam:0");
				mXmppConnectionService.getMessageArchiveService().processFin(fin);
			} else {
			//	parseNonMessage(packet, account);
			}
		} else if (packet.receiver== Enums.ReceiverEnum.room.toString()) {
			message = this.parseGroupchat(packet, account);
			if (message != null) {
				if (message.getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED) {
					message.markUnread();
				} else {
					mXmppConnectionService.markRead(message.getConversation());
					account.activateGracePeriod();
				}
			}
		} else if (packet.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket.TYPE_ERROR) {
			this.parseError(packet, account);
			return;
		} else if (packet.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket.TYPE_HEADLINE) {
			this.parseHeadline(packet, account);
			return;
		}
		if ((message == null) || (message.getBody() == null)) {
			return;
		}
		if ((mXmppConnectionService.confirmMessages())
				&& ((packet.getId() != null))) {
			if (packet.hasChild("markable", "urn:xmpp:chat-markers:0")) {
				ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket receipt = mXmppConnectionService
						.getMessageGenerator().received(account, packet,
								"urn:xmpp:chat-markers:0");
				mXmppConnectionService.sendMessagePacket(account, receipt);
			}
			if (packet.hasChild("request", "urn:xmpp:receipts")) {
				ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket receipt = mXmppConnectionService
						.getMessageGenerator().received(account, packet,
								"urn:xmpp:receipts");
				mXmppConnectionService.sendMessagePacket(account, receipt);
			}
		}
		ir.bilgisoft.toopeto.entities.Conversation conversation = message.getConversation();
		conversation.add(message);
		if (account.getXmppConnection() != null && account.getXmppConnection().getFeatures().advancedStreamFeaturesLoaded()) {
			if (conversation.setLastMessageTransmitted(System.currentTimeMillis())) {
				mXmppConnectionService.updateConversation(conversation);
			}
		}

		if (message.getStatus() == ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED
				&& conversation.getOtrSession() != null
				&& !conversation.getOtrSession().getSessionID().getUserID()
				.equals(message.getCounterpart().getResourcepart())) {
			conversation.endOtrIfNeeded();
		}

                if (packet.type != Enums.MessageTypeEnum.error.toString()) {
                    //	if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_NONE
                    //	|| mXmppConnectionService.saveEncryptedMessages()) {
                    mXmppConnectionService.databaseBackend.createMessage(message);
                    //}
                }
		if (message.trusted() && message.bodyContainsDownloadable()) {
			this.mXmppConnectionService.getHttpConnectionManager()
					.createNewConnection(message);
		}
                else if (!message.isRead()) {
                    mXmppConnectionService.getNotificationService().push(message);
                }
                mXmppConnectionService.updateConversationUi();
            }
     //   }
      //  catch (Exception e)
        //{}
    }

	private void parseHeadline(ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket packet, ir.bilgisoft.toopeto.entities.Account account) {
		if (packet.hasChild("event", "http://jabber.org/protocol/pubsub#event")) {
			ir.bilgisoft.toopeto.xml.Element event = packet.findChild("event",
					"http://jabber.org/protocol/pubsub#event");
			parseEvent(event, packet.getFrom(), account);
		}
	}

	private void parseNick(MessagePacket packet, ir.bilgisoft.toopeto.entities.Account account) {
	//	ir.bilgisoft.toopeto.xml.Element nick = packet.findChild("nick",
			//	"http://jabber.org/protocol/nick");
		//if (nick != null) {
		if (packet.from != null) {
		//	if (packet.getFrom() != null) {
				ir.bilgisoft.toopeto.entities.Contact contact = account.getRoster().getContact(
						packet.from);
			//	contact.setPresenceName(nick.getContent());
				contact.setPresenceName(packet.from);
		}
		}*/
    }
    ///////////////////////////end my code ////////////////////////////////////////
/*
	private ir.bilgisoft.toopeto.entities.Message parseChat(Json packet, ir.bilgisoft.toopeto.entities.Account account) {

        ir.bilgisoft.toopeto.xmpp.jid.Jid jid;
        try {
            jid =Jid.fromParts(packet.from,Jid.constantDomainpart,null ) ;
        }
    catch (InvalidJidException e)
        { jid =null;}
		if (jid == null) {
			return null;
		}
		ir.bilgisoft.toopeto.entities.Conversation conversation = mXmppConnectionService.findOrCreateConversation(account, jid, false);
		updateLastseen( packet , account, true);
		String pgpBody =null;  //getPgpBody(packet);
		ir.bilgisoft.toopeto.entities.Message finishedMessage;
		if (pgpBody != null) {
			finishedMessage = new ir.bilgisoft.toopeto.entities.Message(conversation,
					pgpBody, ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP, ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED);
		} else {
			finishedMessage = new ir.bilgisoft.toopeto.entities.Message(conversation,
					packet., ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_NONE,
					ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED);
		}
		finishedMessage.setRemoteMsgId(packet.getId());
		finishedMessage.markable = isMarkable(packet);
		if (conversation.getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_MULTI
				&& !jid.isBareJid()) {
			finishedMessage.setType(ir.bilgisoft.toopeto.entities.Message.TYPE_PRIVATE);
			finishedMessage.setTrueCounterpart(conversation.getMucOptions()
					.getTrueCounterpart(jid.getResourcepart()));
			if (conversation.hasDuplicateMessage(finishedMessage)) {
				return null;
			}

		}
		finishedMessage.setCounterpart(jid);
		finishedMessage.setTime(getTimestamp(packet));
		return finishedMessage;

	}
*/
    /*
	private ir.bilgisoft.toopeto.entities.Message parseOtrChat(ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket packet, ir.bilgisoft.toopeto.entities.Account account) {
		final ir.bilgisoft.toopeto.xmpp.jid.Jid to = packet.getTo();
		final ir.bilgisoft.toopeto.xmpp.jid.Jid from = packet.getFrom();
		if (to == null || from == null) {
			return null;
		}
		boolean properlyAddressed = !to.isBareJid() || account.countPresences() == 1;
		ir.bilgisoft.toopeto.entities.Conversation conversation = mXmppConnectionService
				.findOrCreateConversation(account, from.toBareJid(), false);
		String presence;
		if (from.isBareJid()) {
            presence = "";
		} else {
			presence = from.getResourcepart();
		}
		updateLastseen(packet, account, true);
		String body = packet.getBody();
		if (body.matches("^\\?OTRv\\d*\\?")) {
			conversation.endOtrIfNeeded();
		}
		if (!conversation.hasValidOtrSession()) {
			if (properlyAddressed) {
				conversation.startOtrSession(presence,false);
			} else {
				return null;
			}
		} else {
			String foreignPresence = conversation.getOtrSession()
					.getSessionID().getUserID();
			if (!foreignPresence.equals(presence)) {
				conversation.endOtrIfNeeded();
				if (properlyAddressed) {
					conversation.startOtrSession(presence, false);
				} else {
					return null;
				}
			}
		}
		try {
			Session otrSession = conversation.getOtrSession();
			SessionStatus before = otrSession.getSessionStatus();
			body = otrSession.transformReceiving(body);
			SessionStatus after = otrSession.getSessionStatus();
			if ((before != after) && (after == SessionStatus.ENCRYPTED)) {
				mXmppConnectionService.onOtrSessionEstablished(conversation);
			} else if ((before != after) && (after == SessionStatus.FINISHED)) {
				conversation.resetOtrSession();
				mXmppConnectionService.updateConversationUi();
			}
			if ((body == null) || (body.isEmpty())) {
				return null;
			}
			if (body.startsWith(ir.bilgisoft.toopeto.utils.CryptoHelper.FILETRANSFER)) {
				String key = body.substring(ir.bilgisoft.toopeto.utils.CryptoHelper.FILETRANSFER.length());
				conversation.setSymmetricKey(ir.bilgisoft.toopeto.utils.CryptoHelper.hexToBytes(key));
				return null;
			}
			ir.bilgisoft.toopeto.entities.Message finishedMessage = new ir.bilgisoft.toopeto.entities.Message(conversation, body, ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_OTR,
					ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED);
			finishedMessage.setTime(getTimestamp(packet));
			finishedMessage.setRemoteMsgId(packet.getId());
			finishedMessage.markable = isMarkable(packet);
			finishedMessage.setCounterpart(from);
			return finishedMessage;
		} catch (Exception e) {
			conversation.resetOtrSession();
			return null;
		}
	}
*/
   /*
	private ir.bilgisoft.toopeto.entities.Message parseGroupchat(ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket packet, ir.bilgisoft.toopeto.entities.Account account) {
		int status;
        final ir.bilgisoft.toopeto.xmpp.jid.Jid from = packet.getFrom();
		if (from == null) {
			return null;
		}
		if (mXmppConnectionService.find(account.pendingConferenceLeaves,
				account, from.toBareJid()) != null) {
			return null;
		}
		ir.bilgisoft.toopeto.entities.Conversation conversation = mXmppConnectionService
				.findOrCreateConversation(account, from.toBareJid(), true);
		if (packet.hasChild("subject")) {
			conversation.getMucOptions().setSubject(
					packet.findChild("subject").getContent());
			mXmppConnectionService.updateConversationUi();
			return null;
		}
		if (from.isBareJid()) {
			return null;
		}
		if (from.getResourcepart().equals(conversation.getMucOptions().getActualNick())) {
			if (mXmppConnectionService.markMessage(conversation,
					packet.getId(), ir.bilgisoft.toopeto.entities.Message.STATUS_SEND)) {
				return null;
			} else if (packet.getId() == null) {
				ir.bilgisoft.toopeto.entities.Message message = conversation.findSentMessageWithBody(packet.getBody());
				if (message != null) {
					mXmppConnectionService.markMessage(message, ir.bilgisoft.toopeto.entities.Message.STATUS_SEND_RECEIVED);
					return null;
				} else {
					status = ir.bilgisoft.toopeto.entities.Message.STATUS_SEND;
				}
			} else {
				status = ir.bilgisoft.toopeto.entities.Message.STATUS_SEND;
			}
		} else {
			status = ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED;
		}
		String pgpBody = getPgpBody(packet);
		ir.bilgisoft.toopeto.entities.Message finishedMessage;
		if (pgpBody == null) {
			finishedMessage = new ir.bilgisoft.toopeto.entities.Message(conversation,
					packet.getBody(), ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_NONE, status);
		} else {
			finishedMessage = new ir.bilgisoft.toopeto.entities.Message(conversation, pgpBody,
					ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP, status);
		}
		finishedMessage.setRemoteMsgId(packet.getId());
		finishedMessage.markable = isMarkable(packet);
		finishedMessage.setCounterpart(from);
		if (status == ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED) {
			finishedMessage.setTrueCounterpart(conversation.getMucOptions()
					.getTrueCounterpart(from.getResourcepart()));
		}
		if (packet.hasChild("delay")
				&& conversation.hasDuplicateMessage(finishedMessage)) {
			return null;
		}
		finishedMessage.setTime(getTimestamp(packet));
		return finishedMessage;
	}
*/
    /*
	private ir.bilgisoft.toopeto.entities.Message parseCarbonMessage(final ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket packet, final ir.bilgisoft.toopeto.entities.Account account) {
		int status;
		final ir.bilgisoft.toopeto.xmpp.jid.Jid fullJid;
		ir.bilgisoft.toopeto.xml.Element forwarded;
		if (packet.hasChild("received", "urn:xmpp:carbons:2")) {
			forwarded = packet.findChild("received", "urn:xmpp:carbons:2")
					.findChild("forwarded", "urn:xmpp:forward:0");
			status = ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED;
		} else if (packet.hasChild("sent", "urn:xmpp:carbons:2")) {
			forwarded = packet.findChild("sent", "urn:xmpp:carbons:2")
					.findChild("forwarded", "urn:xmpp:forward:0");
			status = ir.bilgisoft.toopeto.entities.Message.STATUS_SEND;
		} else {
			return null;
		}
		if (forwarded == null) {
			return null;
		}
		ir.bilgisoft.toopeto.xml.Element message = forwarded.findChild("message");
		if (message == null) {
			return null;
		}
		if (!message.hasChild("body")) {
			if (status == ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED
					&& message.getAttribute("from") != null) {
				parseNonMessage(message, account);
			} else if (status == ir.bilgisoft.toopeto.entities.Message.STATUS_SEND
					&& message.hasChild("displayed", "urn:xmpp:chat-markers:0")) {
				final ir.bilgisoft.toopeto.xmpp.jid.Jid to = message.getAttributeAsJid("to");
				if (to != null) {
					final ir.bilgisoft.toopeto.entities.Conversation conversation = mXmppConnectionService.find(
							mXmppConnectionService.getConversations(), account,
							to.toBareJid());
					if (conversation != null) {
						mXmppConnectionService.markRead(conversation);
					}
				}
			}
			return null;
		}
		if (status == ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED) {
			fullJid = message.getAttributeAsJid("from");
			if (fullJid == null) {
				return null;
			} else {
				updateLastseen(message, account, true);
			}
		} else {
			fullJid = message.getAttributeAsJid("to");
			if (fullJid == null) {
				return null;
			}
		}
		if (message.hasChild("x","http://jabber.org/protocol/muc#user")
				&& "chat".equals(message.getAttribute("type"))) {
			return null;
		}
		ir.bilgisoft.toopeto.entities.Conversation conversation = mXmppConnectionService
				.findOrCreateConversation(account, fullJid.toBareJid(), false);
		String pgpBody = getPgpBody(message);
		ir.bilgisoft.toopeto.entities.Message finishedMessage;
		if (pgpBody != null) {
			finishedMessage = new ir.bilgisoft.toopeto.entities.Message(conversation, pgpBody,
					ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP, status);
		} else {
			String body = message.findChild("body").getContent();
			finishedMessage = new ir.bilgisoft.toopeto.entities.Message(conversation, body,
					ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_NONE, status);
		}
		finishedMessage.setTime(getTimestamp(message));
		finishedMessage.setRemoteMsgId(message.getAttribute("id"));
		finishedMessage.markable = isMarkable(message);
		finishedMessage.setCounterpart(fullJid);
		if (conversation.getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_MULTI
				&& !fullJid.isBareJid()) {
			finishedMessage.setType(ir.bilgisoft.toopeto.entities.Message.TYPE_PRIVATE);
			finishedMessage.setTrueCounterpart(conversation.getMucOptions()
					.getTrueCounterpart(fullJid.getResourcepart()));
			if (conversation.hasDuplicateMessage(finishedMessage)) {
				return null;
			}
		}
		return finishedMessage;
	}
*/
   /*
	private ir.bilgisoft.toopeto.entities.Message parseMamMessage(MessagePacket packet, final ir.bilgisoft.toopeto.entities.Account account)throws InvalidJidException {
		final ir.bilgisoft.toopeto.xml.Element result = packet.findChild("result","urn:xmpp:mam:0");
		if (result == null ) {
			return null;
		}
		final ir.bilgisoft.toopeto.services.MessageArchiveService.Query query = this.mXmppConnectionService.getMessageArchiveService().findQuery(result.getAttribute("queryid"));
		if (query!=null) {
			query.incrementTotalCount();
		}
		final ir.bilgisoft.toopeto.xml.Element forwarded = result.findChild("forwarded","urn:xmpp:forward:0");
		if (forwarded == null) {
			return null;
		}
		final ir.bilgisoft.toopeto.xml.Element message = forwarded.findChild("message");
		if (message == null) {
			return null;
		}
		final ir.bilgisoft.toopeto.xml.Element body = message.findChild("body");
		if (body == null || message.hasChild("private","urn:xmpp:carbons:2") || message.hasChild("no-copy","urn:xmpp:hints")) {
			return null;
		}
		int encryption;
		String content = getPgpBody(message);
		if (content != null) {
			encryption = ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP;
		} else {
			encryption = ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_NONE;
			content = body.getContent();
		}
		if (content == null) {
			return null;
		}
		//final long timestamp =89;// getTimestamp(forwarded);

     // final ir.bilgisoft.toopeto.xmpp.jid.Jid to = Jid.fromString(to); //message.getAttributeAsJid("to");
      final ir.bilgisoft.toopeto.xmpp.jid.Jid from = Jid.fromString(packet.from);//message.getAttributeAsJid("from");

		ir.bilgisoft.toopeto.xmpp.jid.Jid counterpart;
		int status;
		ir.bilgisoft.toopeto.entities.Conversation conversation;
		if (from!=null && to != null && from.toBareJid().equals(account.getJid().toBareJid())) {
			status = ir.bilgisoft.toopeto.entities.Message.STATUS_SEND;
			conversation = this.mXmppConnectionService.findOrCreateConversation(account,to.toBareJid(),false,null);
			counterpart = to;
		} else if (from !=null && to != null) {
			status = ir.bilgisoft.toopeto.entities.Message.STATUS_RECEIVED;
			conversation = this.mXmppConnectionService.findOrCreateConversation(account,from.toBareJid(),false,null);
			counterpart = from;
		} else {
			return null;
		}
		ir.bilgisoft.toopeto.entities.Message finishedMessage = new ir.bilgisoft.toopeto.entities.Message(conversation,packet.data,0,status);
		finishedMessage.setTime(timestamp);
		finishedMessage.setCounterpart(counterpart);
		finishedMessage.setRemoteMsgId(packet.id);
		finishedMessage.setServerMsgId(packet.id);
		if (conversation.hasDuplicateMessage(finishedMessage)) {
			return null;
		}
	//	if (query!=null) {
		//	query.incrementMessageCount();
	//	}
		return finishedMessage;
	}

	private void parseError(final ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket packet, final ir.bilgisoft.toopeto.entities.Account account) {
		final ir.bilgisoft.toopeto.xmpp.jid.Jid from = packet.getFrom();
		mXmppConnectionService.markMessage(account, from.toBareJid(),
				packet.getId(), ir.bilgisoft.toopeto.entities.Message.STATUS_SEND_FAILED);
	}

	private void parseNonMessage(ir.bilgisoft.toopeto.xml.Element packet, ir.bilgisoft.toopeto.entities.Account account) {
		final ir.bilgisoft.toopeto.xmpp.jid.Jid from = packet.getAttributeAsJid("from");
		if (packet.hasChild("event", "http://jabber.org/protocol/pubsub#event")) {
			ir.bilgisoft.toopeto.xml.Element event = packet.findChild("event",
					"http://jabber.org/protocol/pubsub#event");
			parseEvent(event, from, account);
		} else if (from != null
				&& packet.hasChild("displayed", "urn:xmpp:chat-markers:0")) {
			String id = packet
					.findChild("displayed", "urn:xmpp:chat-markers:0")
					.getAttribute("id");
			updateLastseen(packet, account, true);
			mXmppConnectionService.markMessage(account, from.toBareJid(),
					id, ir.bilgisoft.toopeto.entities.Message.STATUS_SEND_DISPLAYED);
		} else if (from != null
				&& packet.hasChild("received", "urn:xmpp:chat-markers:0")) {
			String id = packet.findChild("received", "urn:xmpp:chat-markers:0")
					.getAttribute("id");
			updateLastseen(packet, account, false);
			mXmppConnectionService.markMessage(account, from.toBareJid(),
					id, ir.bilgisoft.toopeto.entities.Message.STATUS_SEND_RECEIVED);
		} else if (from != null
				&& packet.hasChild("received", "urn:xmpp:receipts")) {
			String id = packet.findChild("received", "urn:xmpp:receipts")
					.getAttribute("id");
			updateLastseen(packet, account, false);
			mXmppConnectionService.markMessage(account, from.toBareJid(),
					id, ir.bilgisoft.toopeto.entities.Message.STATUS_SEND_RECEIVED);
		} else if (packet.hasChild("x", "http://jabber.org/protocol/muc#user")) {
			ir.bilgisoft.toopeto.xml.Element x = packet.findChild("x",
					"http://jabber.org/protocol/muc#user");
			if (x.hasChild("invite")) {
				ir.bilgisoft.toopeto.entities.Conversation conversation = mXmppConnectionService
						.findOrCreateConversation(account,
								packet.getAttributeAsJid("from"), true);
				if (!conversation.getMucOptions().online()) {
					if (x.hasChild("password")) {
						ir.bilgisoft.toopeto.xml.Element password = x.findChild("password");
						conversation.getMucOptions().setPassword(
								password.getContent());
						mXmppConnectionService.databaseBackend
								.updateConversation(conversation);
					}
					mXmppConnectionService.joinMuc(conversation);
					mXmppConnectionService.updateConversationUi();
				}
			}
		} else if (packet.hasChild("x", "jabber:x:conference")) {
			ir.bilgisoft.toopeto.xml.Element x = packet.findChild("x", "jabber:x:conference");
            ir.bilgisoft.toopeto.xmpp.jid.Jid jid = x.getAttributeAsJid("jid");
            String password = x.getAttribute("password");
			if (jid != null) {
				ir.bilgisoft.toopeto.entities.Conversation conversation = mXmppConnectionService
						.findOrCreateConversation(account, jid, true);
				if (!conversation.getMucOptions().online()) {
					if (password != null) {
						conversation.getMucOptions().setPassword(password);
						mXmppConnectionService.databaseBackend
								.updateConversation(conversation);
					}
					mXmppConnectionService.joinMuc(conversation);
					mXmppConnectionService.updateConversationUi();
				}
			}
		}
	}

	private void parseEvent(final ir.bilgisoft.toopeto.xml.Element event, final ir.bilgisoft.toopeto.xmpp.jid.Jid from, final ir.bilgisoft.toopeto.entities.Account account) {
		ir.bilgisoft.toopeto.xml.Element items = event.findChild("items");
		if (items == null) {
			return;
		}
		String node = items.getAttribute("node");
		if (node == null) {
			return;
		}
		if (node.equals("urn:xmpp:avatar:metadata")) {
			ir.bilgisoft.toopeto.xmpp.pep.Avatar avatar = ir.bilgisoft.toopeto.xmpp.pep.Avatar.parseMetadata(items);
			if (avatar != null) {
				avatar.owner = from;
				if (mXmppConnectionService.getFileBackend().isAvatarCached(
						avatar)) {
					if (account.getJid().toBareJid().equals(from)) {
						if (account.setAvatar(avatar.getFilename())) {
							mXmppConnectionService.databaseBackend
									.updateAccount(account);
						}
						mXmppConnectionService.getAvatarService().clear(
								account);
						mXmppConnectionService.updateConversationUi();
						mXmppConnectionService.updateAccountUi();
					} else {
						ir.bilgisoft.toopeto.entities.Contact contact = account.getRoster().getContact(
								from);
						contact.setAvatar(avatar.getFilename());
						mXmppConnectionService.getAvatarService().clear(
								contact);
						mXmppConnectionService.updateConversationUi();
						mXmppConnectionService.updateRosterUi();
					}
				} else {
					mXmppConnectionService.fetchAvatar(account, avatar);
				}
			}
		} else if (node.equals("http://jabber.org/protocol/nick")) {
			ir.bilgisoft.toopeto.xml.Element item = items.findChild("item");
			if (item != null) {
				ir.bilgisoft.toopeto.xml.Element nick = item.findChild("nick",
						"http://jabber.org/protocol/nick");
				if (nick != null) {
					if (from != null) {
						ir.bilgisoft.toopeto.entities.Contact contact = account.getRoster().getContact(
								from);
						contact.setPresenceName(nick.getContent());
						mXmppConnectionService.getAvatarService().clear(account);
						mXmppConnectionService.updateConversationUi();
						mXmppConnectionService.updateAccountUi();
					}
				}
			}
		}
	}

	private String getPgpBody(ir.bilgisoft.toopeto.xml.Element message) {
		ir.bilgisoft.toopeto.xml.Element child = message.findChild("x", "jabber:x:encrypted");
		if (child == null) {
			return null;
		} else {
			return child.getContent();
		}
	}
*/
	private boolean isMarkable(ir.bilgisoft.toopeto.xml.Element message) {
		return message.hasChild("markable", "urn:xmpp:chat-markers:0");
	}


}
