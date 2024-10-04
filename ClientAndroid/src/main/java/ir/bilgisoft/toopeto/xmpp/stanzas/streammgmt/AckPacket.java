package ir.bilgisoft.toopeto.xmpp.stanzas.streammgmt;

public class AckPacket extends ir.bilgisoft.toopeto.xmpp.stanzas.AbstractStanza {

	public AckPacket(int sequence, int smVersion) {
		super("a");
		this.setAttribute("xmlns", "urn:xmpp:sm:" + smVersion);
		this.setAttribute("h", Integer.toString(sequence));
	}

}
