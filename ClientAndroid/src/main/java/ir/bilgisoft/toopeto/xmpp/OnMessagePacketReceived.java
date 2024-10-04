package ir.bilgisoft.toopeto.xmpp;

import ir.bilgisoft.toopeto.json.MessagePacket;

public interface OnMessagePacketReceived extends ir.bilgisoft.toopeto.xmpp.PacketReceived {
	public void onMessagePacketReceived(ir.bilgisoft.toopeto.entities.Account account, MessagePacket packet);
}
