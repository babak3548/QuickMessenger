package ir.bilgisoft.toopeto.xmpp;

import ir.bilgisoft.toopeto.json.Json;
import ir.bilgisoft.toopeto.entities.Account;

public interface OnIqPacketReceived extends ir.bilgisoft.toopeto.xmpp.PacketReceived {
	public void onIqPacketReceived(Account account, String  jsonString);
}
