package ir.bilgisoft.toopeto.xmpp.jingle;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import android.annotation.SuppressLint;
import android.util.Log;

public class JingleConnectionManager extends ir.bilgisoft.toopeto.services.AbstractConnectionManager {
	private List<ir.bilgisoft.toopeto.xmpp.jingle.JingleConnection> connections = new CopyOnWriteArrayList<>();

	private HashMap<ir.bilgisoft.toopeto.xmpp.jid.Jid, ir.bilgisoft.toopeto.xmpp.jingle.JingleCandidate> primaryCandidates = new HashMap<>();

	@SuppressLint("TrulyRandom")
	private SecureRandom random = new SecureRandom();

	public JingleConnectionManager(ir.bilgisoft.toopeto.services.XmppConnectionService service) {
		super(service);
	}

	public void deliverPacket(ir.bilgisoft.toopeto.entities.Account account, ir.bilgisoft.toopeto.xmpp.jingle.stanzas.JinglePacket packet) {
		if (packet.isAction("session-initiate")) {
			ir.bilgisoft.toopeto.xmpp.jingle.JingleConnection connection = new ir.bilgisoft.toopeto.xmpp.jingle.JingleConnection(this);
			connection.init(account, packet);
			connections.add(connection);
		} else {
			for (ir.bilgisoft.toopeto.xmpp.jingle.JingleConnection connection : connections) {
				if (connection.getAccount() == account
						&& connection.getSessionId().equals(
								packet.getSessionId())
						&& connection.getCounterPart().equals(packet.getFrom())) {
					connection.deliverPacket(packet);
					return;
				}
			}
			ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket response = packet.generateResponse(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.ERROR);
			ir.bilgisoft.toopeto.xml.Element error = response.addChild("error");
			error.setAttribute("type", "cancel");
			error.addChild("item-not-found",
					"urn:ietf:params:xml:ns:xmpp-stanzas");
			error.addChild("unknown-session", "urn:xmpp:jingle:errors:1");
		//	account.getXmppConnection().sendIqPacket(response, null);
		}
	}

	public ir.bilgisoft.toopeto.xmpp.jingle.JingleConnection createNewConnection(ir.bilgisoft.toopeto.entities.Message message) {
		ir.bilgisoft.toopeto.xmpp.jingle.JingleConnection connection = new ir.bilgisoft.toopeto.xmpp.jingle.JingleConnection(this);
		connection.init(message);
		this.connections.add(connection);
		return connection;
	}

	public ir.bilgisoft.toopeto.xmpp.jingle.JingleConnection createNewConnection(final ir.bilgisoft.toopeto.xmpp.jingle.stanzas.JinglePacket packet) {
		ir.bilgisoft.toopeto.xmpp.jingle.JingleConnection connection = new ir.bilgisoft.toopeto.xmpp.jingle.JingleConnection(this);
		this.connections.add(connection);
		return connection;
	}

	public void finishConnection(ir.bilgisoft.toopeto.xmpp.jingle.JingleConnection connection) {
		this.connections.remove(connection);
	}

	public void getPrimaryCandidate(ir.bilgisoft.toopeto.entities.Account account,
			final ir.bilgisoft.toopeto.xmpp.jingle.OnPrimaryCandidateFound listener) {
		if (ir.bilgisoft.toopeto.Config.NO_PROXY_LOOKUP) {
			listener.onPrimaryCandidateFound(false, null);
			return;
		}
		if (!this.primaryCandidates.containsKey(account.getJid().toBareJid())) {
			String xmlns = "http://jabber.org/protocol/bytestreams";
			final String proxy = account.getXmppConnection()
					.findDiscoItemByFeature(xmlns);
			if (proxy != null) {
				ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iq = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.GET);
				iq.setAttribute("to", proxy);
				iq.query(xmlns);
                /*
				account.getXmppConnection().sendIqPacket(iq,
						new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {

							@Override
							public void onIqPacketReceived(ir.bilgisoft.toopeto.entities.Account account,
									ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet) {
								ir.bilgisoft.toopeto.xml.Element streamhost = packet
										.query()
										.findChild("streamhost",
												"http://jabber.org/protocol/bytestreams");
								if (streamhost != null) {
									ir.bilgisoft.toopeto.xmpp.jingle.JingleCandidate candidate = new ir.bilgisoft.toopeto.xmpp.jingle.JingleCandidate(
											nextRandomId(), true);
									candidate.setHost(streamhost
											.getAttribute("host"));
									candidate.setPort(Integer
											.parseInt(streamhost
													.getAttribute("port")));
									candidate
											.setType(ir.bilgisoft.toopeto.xmpp.jingle.JingleCandidate.TYPE_PROXY);
                                    try {
                                        candidate.setJid(ir.bilgisoft.toopeto.xmpp.jid.Jid.fromString(proxy));
                                    } catch (final ir.bilgisoft.toopeto.xmpp.jid.InvalidJidException e) {
                                        candidate.setJid(null);
                                    }
                                    candidate.setPriority(655360 + 65535);
									primaryCandidates.put(account.getJid().toBareJid(),
											candidate);
									listener.onPrimaryCandidateFound(true,
											candidate);
								} else {
									listener.onPrimaryCandidateFound(false,
											null);
								}
							}
						});*/
			} else {
				listener.onPrimaryCandidateFound(false, null);
			}

		} else {
			listener.onPrimaryCandidateFound(true,
					this.primaryCandidates.get(account.getJid().toBareJid()));
		}
	}

	public String nextRandomId() {
		return new BigInteger(50, random).toString(32);
	}

	public void deliverIbbPacket(ir.bilgisoft.toopeto.entities.Account account, ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet) {
		String sid = null;
		ir.bilgisoft.toopeto.xml.Element payload = null;
		if (packet.hasChild("open", "http://jabber.org/protocol/ibb")) {
			payload = packet
					.findChild("open", "http://jabber.org/protocol/ibb");
			sid = payload.getAttribute("sid");
		} else if (packet.hasChild("data", "http://jabber.org/protocol/ibb")) {
			payload = packet
					.findChild("data", "http://jabber.org/protocol/ibb");
			sid = payload.getAttribute("sid");
		}
		if (sid != null) {
			for (ir.bilgisoft.toopeto.xmpp.jingle.JingleConnection connection : connections) {
				if (connection.getAccount() == account
						&& connection.hasTransportId(sid)) {
					ir.bilgisoft.toopeto.xmpp.jingle.JingleTransport transport = connection.getTransport();
					if (transport instanceof ir.bilgisoft.toopeto.xmpp.jingle.JingleInbandTransport) {
						ir.bilgisoft.toopeto.xmpp.jingle.JingleInbandTransport inbandTransport = (ir.bilgisoft.toopeto.xmpp.jingle.JingleInbandTransport) transport;
						inbandTransport.deliverPayload(packet, payload);
						return;
					}
				}
			}
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,
					"couldnt deliver payload: " + payload.toString());
		} else {
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, "no sid found in incomming ibb packet");
		}
	}

	public void cancelInTransmission() {
		for (ir.bilgisoft.toopeto.xmpp.jingle.JingleConnection connection : this.connections) {
			if (connection.getJingleStatus() == ir.bilgisoft.toopeto.xmpp.jingle.JingleConnection.JINGLE_STATUS_TRANSMITTING) {
				connection.cancel();
			}
		}
	}
}
