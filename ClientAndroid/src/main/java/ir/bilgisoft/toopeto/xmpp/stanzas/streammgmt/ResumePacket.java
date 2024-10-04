package ir.bilgisoft.toopeto.xmpp.stanzas.streammgmt;

public class ResumePacket extends ir.bilgisoft.toopeto.xmpp.stanzas.AbstractStanza {

	public ResumePacket(String id, int sequence, int smVersion) {
		super("resume");
		this.setAttribute("xmlns", "urn:xmpp:sm:" + smVersion);
		this.setAttribute("previd", id);
		this.setAttribute("h", Integer.toString(sequence));
	}

}
