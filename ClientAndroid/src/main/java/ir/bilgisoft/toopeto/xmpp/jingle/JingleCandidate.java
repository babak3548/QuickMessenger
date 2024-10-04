package ir.bilgisoft.toopeto.xmpp.jingle;

import java.util.ArrayList;
import java.util.List;

public class JingleCandidate {

	public static int TYPE_UNKNOWN;
	public static int TYPE_DIRECT = 0;
	public static int TYPE_PROXY = 1;

	private boolean ours;
	private boolean usedByCounterpart = false;
	private String cid;
	private String host;
	private int port;
	private int type;
	private ir.bilgisoft.toopeto.xmpp.jid.Jid jid;
	private int priority;

	public JingleCandidate(String cid, boolean ours) {
		this.ours = ours;
		this.cid = cid;
	}

	public String getCid() {
		return cid;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return this.host;
	}

	public void setJid(final ir.bilgisoft.toopeto.xmpp.jid.Jid jid) {
		this.jid = jid;
	}

	public ir.bilgisoft.toopeto.xmpp.jid.Jid getJid() {
		return this.jid;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return this.port;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setType(String type) {
        switch (type) {
            case "proxy":
                this.type = TYPE_PROXY;
                break;
            case "direct":
                this.type = TYPE_DIRECT;
                break;
            default:
                this.type = TYPE_UNKNOWN;
                break;
        }
	}

	public void setPriority(int i) {
		this.priority = i;
	}

	public int getPriority() {
		return this.priority;
	}

	public boolean equals(JingleCandidate other) {
		return this.getCid().equals(other.getCid());
	}

	public boolean equalValues(JingleCandidate other) {
		return other.getHost().equals(this.getHost())
				&& (other.getPort() == this.getPort());
	}

	public boolean isOurs() {
		return ours;
	}

	public int getType() {
		return this.type;
	}

	public static List<JingleCandidate> parse(List<ir.bilgisoft.toopeto.xml.Element> canditates) {
		List<JingleCandidate> parsedCandidates = new ArrayList<>();
		for (ir.bilgisoft.toopeto.xml.Element c : canditates) {
			parsedCandidates.add(JingleCandidate.parse(c));
		}
		return parsedCandidates;
	}

	public static JingleCandidate parse(ir.bilgisoft.toopeto.xml.Element candidate) {
		JingleCandidate parsedCandidate = new JingleCandidate(
				candidate.getAttribute("cid"), false);
		parsedCandidate.setHost(candidate.getAttribute("host"));
		parsedCandidate.setJid(candidate.getAttributeAsJid("jid"));
		parsedCandidate.setType(candidate.getAttribute("type"));
		parsedCandidate.setPriority(Integer.parseInt(candidate
				.getAttribute("priority")));
		parsedCandidate
				.setPort(Integer.parseInt(candidate.getAttribute("port")));
		return parsedCandidate;
	}

	public ir.bilgisoft.toopeto.xml.Element toElement() {
		ir.bilgisoft.toopeto.xml.Element element = new ir.bilgisoft.toopeto.xml.Element("candidate");
		element.setAttribute("cid", this.getCid());
		element.setAttribute("host", this.getHost());
		element.setAttribute("port", Integer.toString(this.getPort()));
		element.setAttribute("jid", this.getJid().toString());
		element.setAttribute("priority", Integer.toString(this.getPriority()));
		if (this.getType() == TYPE_DIRECT) {
			element.setAttribute("type", "direct");
		} else if (this.getType() == TYPE_PROXY) {
			element.setAttribute("type", "proxy");
		}
		return element;
	}

	public void flagAsUsedByCounterpart() {
		this.usedByCounterpart = true;
	}

	public boolean isUsedByCounterpart() {
		return this.usedByCounterpart;
	}

	public String toString() {
		return this.getHost() + ":" + this.getPort() + " (prio="
				+ this.getPriority() + ")";
	}
}
