package ir.bilgisoft.toopeto.xmpp.jingle.stanzas;

public class Content extends ir.bilgisoft.toopeto.xml.Element {

	private String transportId;

	private Content(String name) {
		super(name);
	}

	public Content() {
		super("content");
	}

	public Content(String creator, String name) {
		super("content");
		this.setAttribute("creator", creator);
		this.setAttribute("name", name);
	}

	public void setTransportId(String sid) {
		this.transportId = sid;
	}

	public void setFileOffer(ir.bilgisoft.toopeto.entities.DownloadableFile actualFile, boolean otr) {
		ir.bilgisoft.toopeto.xml.Element description = this.addChild("description",
				"urn:xmpp:jingle:apps:file-transfer:3");
		ir.bilgisoft.toopeto.xml.Element offer = description.addChild("offer");
		ir.bilgisoft.toopeto.xml.Element file = offer.addChild("file");
		file.addChild("size").setContent(Long.toString(actualFile.getSize()));
		if (otr) {
			file.addChild("name").setContent(actualFile.getName() + ".otr");
		} else {
			file.addChild("name").setContent(actualFile.getName());
		}
	}

	public ir.bilgisoft.toopeto.xml.Element getFileOffer() {
		ir.bilgisoft.toopeto.xml.Element description = this.findChild("description",
				"urn:xmpp:jingle:apps:file-transfer:3");
		if (description == null) {
			return null;
		}
		ir.bilgisoft.toopeto.xml.Element offer = description.findChild("offer");
		if (offer == null) {
			return null;
		}
		return offer.findChild("file");
	}

	public void setFileOffer(ir.bilgisoft.toopeto.xml.Element fileOffer) {
		ir.bilgisoft.toopeto.xml.Element description = this.findChild("description",
				"urn:xmpp:jingle:apps:file-transfer:3");
		if (description == null) {
			description = this.addChild("description",
					"urn:xmpp:jingle:apps:file-transfer:3");
		}
		description.addChild(fileOffer);
	}

	public String getTransportId() {
		if (hasSocks5Transport()) {
			this.transportId = socks5transport().getAttribute("sid");
		} else if (hasIbbTransport()) {
			this.transportId = ibbTransport().getAttribute("sid");
		}
		return this.transportId;
	}

	public ir.bilgisoft.toopeto.xml.Element socks5transport() {
		ir.bilgisoft.toopeto.xml.Element transport = this.findChild("transport",
				"urn:xmpp:jingle:transports:s5b:1");
		if (transport == null) {
			transport = this.addChild("transport",
					"urn:xmpp:jingle:transports:s5b:1");
			transport.setAttribute("sid", this.transportId);
		}
		return transport;
	}

	public ir.bilgisoft.toopeto.xml.Element ibbTransport() {
		ir.bilgisoft.toopeto.xml.Element transport = this.findChild("transport",
				"urn:xmpp:jingle:transports:ibb:1");
		if (transport == null) {
			transport = this.addChild("transport",
					"urn:xmpp:jingle:transports:ibb:1");
			transport.setAttribute("sid", this.transportId);
		}
		return transport;
	}

	public boolean hasSocks5Transport() {
		return this.hasChild("transport", "urn:xmpp:jingle:transports:s5b:1");
	}

	public boolean hasIbbTransport() {
		return this.hasChild("transport", "urn:xmpp:jingle:transports:ibb:1");
	}
}
