package ir.bilgisoft.toopeto.xmpp;

import ir.bilgisoft.toopeto.json.UserPacket;

public interface OnPresencePacketReceived extends ir.bilgisoft.toopeto.xmpp.PacketReceived {
	public void onPresencePacketReceived(ir.bilgisoft.toopeto.entities.Account account, UserPacket packet);
}
