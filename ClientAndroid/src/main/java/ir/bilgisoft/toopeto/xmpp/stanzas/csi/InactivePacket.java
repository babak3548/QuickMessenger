package ir.bilgisoft.toopeto.xmpp.stanzas.csi;

public class InactivePacket extends ir.bilgisoft.toopeto.xmpp.stanzas.AbstractStanza {
	public InactivePacket() {
		super("inactive");
		setAttribute("xmlns", "urn:xmpp:csi:0");
	}
}
