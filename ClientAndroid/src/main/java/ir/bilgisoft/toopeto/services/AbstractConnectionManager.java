package ir.bilgisoft.toopeto.services;

public class AbstractConnectionManager {
	protected ir.bilgisoft.toopeto.services.XmppConnectionService mXmppConnectionService;

	public AbstractConnectionManager(ir.bilgisoft.toopeto.services.XmppConnectionService service) {
		this.mXmppConnectionService = service;
	}

	public ir.bilgisoft.toopeto.services.XmppConnectionService getXmppConnectionService() {
		return this.mXmppConnectionService;
	}

	public long getAutoAcceptFileSize() {
		String config = this.mXmppConnectionService.getPreferences().getString(
				"auto_accept_file_size", "524288");
		try {
			return Long.parseLong(config);
		} catch (NumberFormatException e) {
			return 524288;
		}
	}
}
