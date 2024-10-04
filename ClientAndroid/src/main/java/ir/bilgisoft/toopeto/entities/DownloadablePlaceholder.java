package ir.bilgisoft.toopeto.entities;

public class DownloadablePlaceholder implements ir.bilgisoft.toopeto.entities.Downloadable {

	private int status;

	public DownloadablePlaceholder(int status) {
		this.status = status;
	}
	@Override
	public boolean start() {
		return false;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public long getFileSize() {
		return 0;
	}

	@Override
	public int getProgress() {
		return 0;
	}

	@Override
	public String getMimeType() {
		return "";
	}

	@Override
	public void cancel() {

	}
}
