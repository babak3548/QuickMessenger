package ir.bilgisoft.toopeto.xmpp.jingle;

public interface OnPrimaryCandidateFound {
	public void onPrimaryCandidateFound(boolean success,
                                        JingleCandidate canditate);
}
