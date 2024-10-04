package ir.bilgisoft.toopeto.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class EventReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent mIntentForService = new Intent(context,
				ir.bilgisoft.toopeto.services.XmppConnectionService.class);
		if (intent.getAction() != null) {
			mIntentForService.setAction(intent.getAction());
		} else {
			mIntentForService.setAction("other");
		}
		if (intent.getAction().equals("ui")
				|| ir.bilgisoft.toopeto.persistance.DatabaseBackend.getInstance(context).hasEnabledAccounts()) {
			context.startService(mIntentForService);
		}
	}

}
