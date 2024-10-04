package ir.bilgisoft.toopeto.generator;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import net.java.otr4j.OtrException;
import net.java.otr4j.session.Session;

import ir.bilgisoft.toopeto.json.Enums;
import ir.bilgisoft.toopeto.json.MessagePacket;

public class MessageGenerator extends ir.bilgisoft.toopeto.generator.AbstractGenerator {
	public MessageGenerator(ir.bilgisoft.toopeto.services.XmppConnectionService service) {
		super(service);
	}

	private MessagePacket preparePacket(ir.bilgisoft.toopeto.entities.Message message, boolean addDelay) {
        /*
		ir.bilgisoft.toopeto.entities.Conversation conversation = message.getConversation();
		ir.bilgisoft.toopeto.entities.Account account = conversation.getAccount();
		ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket packet = new ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket();
		if (conversation.getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_SINGLE) {
			packet.setTo(message.getCounterpart());
			packet.setType(ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket.TYPE_CHAT);
			packet.addChild("markable", "urn:xmpp:chat-markers:0");
			if (this.mXmppConnectionService.indicateReceived()) {
				packet.addChild("request", "urn:xmpp:receipts");
			}
		} else if (message.getType() == ir.bilgisoft.toopeto.entities.Message.TYPE_PRIVATE) {
			packet.setTo(message.getCounterpart());
			packet.setType(ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket.TYPE_CHAT);
		} else {
			packet.setTo(message.getCounterpart().getLocalpart());
			packet.setType(ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket.TYPE_GROUPCHAT);
		}
		packet.setFrom(account.getJid());
		packet.setId(message.getUuid());
		if (addDelay) {
			addDelay(packet, message.getTimeSent());
		}*/
		return new MessagePacket();//bayad pyadeh sazi shavad
	}

	private void addDelay(ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket packet, long timestamp) {
		final SimpleDateFormat mDateFormat = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
		mDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		ir.bilgisoft.toopeto.xml.Element delay = packet.addChild("delay", "urn:xmpp:delay");
		Date date = new Date(timestamp);
		delay.setAttribute("stamp", mDateFormat.format(date));
	}

	public MessagePacket generateOtrChat(ir.bilgisoft.toopeto.entities.Message message) {
		return generateOtrChat(message, false);
	}

	public MessagePacket generateOtrChat(ir.bilgisoft.toopeto.entities.Message message, boolean addDelay) {
	/*	Session otrSession = message.getConversation().getOtrSession();
		if (otrSession == null) {
			return null;
		}
		ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket packet = preparePacket(message, addDelay);
		packet.addChild("private", "urn:xmpp:carbons:2");
		packet.addChild("no-copy", "urn:xmpp:hints");
		try {
			packet.setBody(otrSession.transformSending(message.getBody()));
			return packet;
		} catch (OtrException e) {
			return null;
		} */
        Log.d("error :","errsal otr nadarim bayad az secnario hazf garrdad");
        return  null;
	}

	public MessagePacket generateChat(ir.bilgisoft.toopeto.entities.Message message) {
		return generateChat(message, false);
	}

	public MessagePacket generateChat(ir.bilgisoft.toopeto.entities.Message message, boolean addDelay) {
		MessagePacket packet = preparePacket(message, addDelay);
        packet.data=message.getBody();
		//packet.setBody(message.getBody());
		return packet;
	}

	public ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket generatePgpChat(ir.bilgisoft.toopeto.entities.Message message) {
		return generatePgpChat(message, false);
	}

	public ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket generatePgpChat(ir.bilgisoft.toopeto.entities.Message message, boolean addDelay) {
		/*ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket packet = preparePacket(message, addDelay);
		packet.setBody("This is an XEP-0027 encryted message");
		if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_DECRYPTED) {
			packet.addChild("x", "jabber:x:encrypted").setContent(
					message.getEncryptedBody());
		} else if (message.getEncryption() == ir.bilgisoft.toopeto.entities.Message.ENCRYPTION_PGP) {
			packet.addChild("x", "jabber:x:encrypted").setContent(
					message.getBody());
		}
		return packet; */
        return  null;
	}

	public ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket generateNotAcceptable(ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket origin) {
		ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket packet = generateError(origin);
		ir.bilgisoft.toopeto.xml.Element error = packet.addChild("error");
		error.setAttribute("type", "modify");
		error.setAttribute("code", "406");
		error.addChild("not-acceptable");
		return packet;
	}

	private ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket generateError(ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket origin) {
		ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket packet = new ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket();
		packet.setId(origin.getId());
		packet.setTo(origin.getFrom());
		packet.setBody(origin.getBody());
		packet.setType(ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket.TYPE_ERROR);
		return packet;
	}

	public MessagePacket confirm(final ir.bilgisoft.toopeto.entities.Account account, final ir.bilgisoft.toopeto.xmpp.jid.Jid to, final String id) {
		MessagePacket packet = new MessagePacket();
        packet.id=id;
        packet.type=Enums.MessageTypeEnum.read.toString();
        packet.to=to.getLocalpart();
		//packet.setType(ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket.TYPE_NORMAL);
		//packet.setTo(to);
		//packet.setFrom(account.getJid());
		//ir.bilgisoft.toopeto.xml.Element received = packet.addChild("displayed","urn:xmpp:chat-markers:0");
	//	received.setAttribute("id", id);
		return packet;
	}

	public MessagePacket conferenceSubject(ir.bilgisoft.toopeto.entities.Conversation conversation,
			String subject) {
	/*	ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket packet = new ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket();
		packet.setType(ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket.TYPE_GROUPCHAT);
		packet.setTo(conversation.getJid().toBareJid());
		ir.bilgisoft.toopeto.xml.Element subjectChild = new ir.bilgisoft.toopeto.xml.Element("subject");
		subjectChild.setContent(subject);
		packet.addChild(subjectChild);
		packet.setFrom(conversation.getAccount().getJid().toBareJid());*/
        MessagePacket messagePacket=new MessagePacket();
        messagePacket.data=subject;
        messagePacket.to=conversation.getJid().getLocalpart();
        messagePacket.type= Enums.MessageTypeEnum.notifySubject.toString();

		return messagePacket;
	}

	public ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket directInvite(final ir.bilgisoft.toopeto.entities.Conversation conversation, final ir.bilgisoft.toopeto.xmpp.jid.Jid contact) {
		ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket packet = new ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket();
		packet.setType(ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket.TYPE_NORMAL);
		packet.setTo(contact);
		packet.setFrom(conversation.getAccount().getJid());
		ir.bilgisoft.toopeto.xml.Element x = packet.addChild("x", "jabber:x:conference");
		x.setAttribute("jid", conversation.getJid().toBareJid().toString());
		return packet;
	}

	public MessagePacket invite(ir.bilgisoft.toopeto.entities.Conversation conversation, ir.bilgisoft.toopeto.xmpp.jid.Jid contact) {
		MessagePacket packet = new MessagePacket();
		/*packet.setTo(conversation.getJid().toBareJid());
		packet.setFrom(conversation.getAccount().getJid());
		ir.bilgisoft.toopeto.xml.Element x = new ir.bilgisoft.toopeto.xml.Element("x");
		x.setAttribute("xmlns", "http://jabber.org/protocol/muc#user");
		ir.bilgisoft.toopeto.xml.Element invite = new ir.bilgisoft.toopeto.xml.Element("invite");
		invite.setAttribute("to", contact.toBareJid().toString());
		x.addChild(invite);
		packet.addChild(x);*/
        packet.to=conversation.getJid().getLocalpart();
        packet.type=Enums.MessageTypeEnum.ping.toString();
        packet.receiver=conversation.getModeEnum().toString();
		return packet;
	}

	public ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket received(ir.bilgisoft.toopeto.entities.Account account,
			ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket originalMessage, String namespace) {
		ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket receivedPacket = new ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket();
		receivedPacket.setType(ir.bilgisoft.toopeto.xmpp.stanzas.MessagePacket.TYPE_NORMAL);
		receivedPacket.setTo(originalMessage.getFrom());
		receivedPacket.setFrom(account.getJid());
		ir.bilgisoft.toopeto.xml.Element received = receivedPacket.addChild("received", namespace);
		received.setAttribute("id", originalMessage.getId());
		return receivedPacket;
	}


}
