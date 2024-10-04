package ir.bilgisoft.toopeto.xmpp.jingle;

public interface OnJinglePacketReceived extends ir.bilgisoft.toopeto.xmpp.PacketReceived {
	public void onJinglePacketReceived(ir.bilgisoft.toopeto.entities.Account account, ir.bilgisoft.toopeto.xmpp.jingle.stanzas.JinglePacket packet);
}
