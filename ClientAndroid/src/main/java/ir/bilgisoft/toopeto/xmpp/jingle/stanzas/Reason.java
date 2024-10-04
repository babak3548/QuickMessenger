package ir.bilgisoft.toopeto.xmpp.jingle.stanzas;

public class Reason extends ir.bilgisoft.toopeto.xml.Element {
	private Reason(String name) {
		super(name);
	}

	public Reason() {
		super("reason");
	}
}
