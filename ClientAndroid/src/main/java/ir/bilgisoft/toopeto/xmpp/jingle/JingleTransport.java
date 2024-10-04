package ir.bilgisoft.toopeto.xmpp.jingle;

public abstract class JingleTransport {
	public abstract void connect(final ir.bilgisoft.toopeto.xmpp.jingle.OnTransportConnected callback);

	public abstract void receive(final ir.bilgisoft.toopeto.entities.DownloadableFile file,
			final ir.bilgisoft.toopeto.xmpp.jingle.OnFileTransmissionStatusChanged callback);

	public abstract void send(final ir.bilgisoft.toopeto.entities.DownloadableFile file,
			final ir.bilgisoft.toopeto.xmpp.jingle.OnFileTransmissionStatusChanged callback);

	public abstract void disconnect();
}
