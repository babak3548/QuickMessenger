package ir.bilgisoft.toopeto.xmpp.stanzas;

public class IqPacket extends ir.bilgisoft.toopeto.xmpp.stanzas.AbstractStanza {

	public static enum TYPE {
		ERROR,
		SET,
		RESULT,
		GET,
		INVALID
	}

	public IqPacket(final TYPE type) {
		super("iq");
		if (type != TYPE.INVALID) {
			this.setAttribute("type", type.toString().toLowerCase());
		}
	}

	public IqPacket() {
		super("iq");
	}

	public ir.bilgisoft.toopeto.xml.Element query() {
		ir.bilgisoft.toopeto.xml.Element query = findChild("query");
		if (query == null) {
			query = addChild("query");
		}
		return query;
	}

	public ir.bilgisoft.toopeto.xml.Element query(final String xmlns) {
		final ir.bilgisoft.toopeto.xml.Element query = query();
		query.setAttribute("xmlns", xmlns);
		return query();
	}

	public TYPE getType() {
		final String type = getAttribute("type");
		switch (type) {
			case "error":
				return TYPE.ERROR;
			case "result":
				return TYPE.RESULT;
			case "set":
				return TYPE.SET;
			case "get":
				return TYPE.GET;
			default:
				return TYPE.INVALID;
		}
	}

	public IqPacket generateResponse(final TYPE type) {
		final IqPacket packet = new IqPacket(type);
		packet.setTo(this.getFrom());
		packet.setId(this.getId());
		return packet;
	}

}
