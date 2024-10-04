package ir.bilgisoft.toopeto.xmpp.stanzas.csi;

public class ActivePacket extends ir.bilgisoft.toopeto.xmpp.stanzas.AbstractStanza {
	public ActivePacket() {
		super("active");
		setAttribute("xmlns", "urn:xmpp:csi:0");
	}
}
