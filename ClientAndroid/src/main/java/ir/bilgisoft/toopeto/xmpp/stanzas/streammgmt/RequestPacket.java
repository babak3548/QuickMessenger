package ir.bilgisoft.toopeto.xmpp.stanzas.streammgmt;

import ir.bilgisoft.toopeto.xmpp.stanzas.AbstractStanza;

public class RequestPacket extends AbstractStanza {

	public RequestPacket(int smVersion) {
		super("r");
		this.setAttribute("xmlns", "urn:xmpp:sm:" + smVersion);
	}

}
