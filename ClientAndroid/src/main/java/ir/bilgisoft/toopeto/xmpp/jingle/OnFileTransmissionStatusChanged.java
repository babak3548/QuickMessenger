package ir.bilgisoft.toopeto.xmpp.jingle;

public interface OnFileTransmissionStatusChanged {
	public void onFileTransmitted(ir.bilgisoft.toopeto.entities.DownloadableFile file);

	public void onFileTransferAborted();
}
