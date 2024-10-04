package ir.bilgisoft.toopeto.services;

import android.util.Log;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import ir.bilgisoft.toopeto.R;

public class MessageArchiveService implements ir.bilgisoft.toopeto.xmpp.OnAdvancedStreamFeaturesLoaded {

	private final ir.bilgisoft.toopeto.services.XmppConnectionService mXmppConnectionService;

	private final HashSet<Query> queries = new HashSet<Query>();
	private final ArrayList<Query> pendingQueries = new ArrayList<Query>();

	public enum PagingOrder {
		NORMAL,
		REVERSE
	};

	public MessageArchiveService(final ir.bilgisoft.toopeto.services.XmppConnectionService service) {
		this.mXmppConnectionService = service;
	}

	public void catchup(final ir.bilgisoft.toopeto.entities.Account account) {
		long startCatchup = getLastMessageTransmitted(account);
		long endCatchup = account.getXmppConnection().getLastSessionEstablished();
		if (startCatchup == 0) {
			return;
		} else if (endCatchup - startCatchup >= ir.bilgisoft.toopeto.Config.MAM_MAX_CATCHUP) {
			startCatchup = endCatchup - ir.bilgisoft.toopeto.Config.MAM_MAX_CATCHUP;
			List<ir.bilgisoft.toopeto.entities.Conversation> conversations = mXmppConnectionService.getConversations();
			for (ir.bilgisoft.toopeto.entities.Conversation conversation : conversations) {
				if (conversation.getMode() == ir.bilgisoft.toopeto.entities.Conversation.MODE_SINGLE &&
                        conversation.getAccount() == account
                        && startCatchup > conversation.getLastMessageTransmitted()) {
					this.query(conversation,startCatchup);
				}
			}
		}
		final Query query = new Query(account, startCatchup, endCatchup);
		this.queries.add(query);
		this.execute(query);
	}

	private long getLastMessageTransmitted(final ir.bilgisoft.toopeto.entities.Account account) {
		long timestamp = 0;
		for(final ir.bilgisoft.toopeto.entities.Conversation conversation : mXmppConnectionService.getConversations()) {
			if (conversation.getAccount() == account) {
				long tmp = conversation.getLastMessageTransmitted();
				if (tmp > timestamp) {
					timestamp = tmp;
				}
			}
		}
		return timestamp;
	}

	public Query query(final ir.bilgisoft.toopeto.entities.Conversation conversation) {
		return query(conversation,conversation.getAccount().getXmppConnection().getLastSessionEstablished());
	}

	public Query query(final ir.bilgisoft.toopeto.entities.Conversation conversation, long end) {
		return this.query(conversation,conversation.getLastMessageTransmitted(),end);
	}

	public Query query(ir.bilgisoft.toopeto.entities.Conversation conversation, long start, long end) {
		synchronized (this.queries) {
			if (start > end) {
				return null;
			}
			final Query query = new Query(conversation, start, end, PagingOrder.REVERSE);
			this.queries.add(query);
			this.execute(query);
			return query;
		}
	}

	public void executePendingQueries(final ir.bilgisoft.toopeto.entities.Account account) {
		List<Query> pending = new ArrayList<>();
		synchronized(this.pendingQueries) {
			for(Iterator<Query> iterator = this.pendingQueries.iterator(); iterator.hasNext();) {
				Query query = iterator.next();
				if (query.getAccount() == account) {
					pending.add(query);
					iterator.remove();
				}
			}
		}
		for(Query query : pending) {
			this.execute(query);
		}
	}
//dar n ghesmat htemalan list messagehayee zakhiree shode ra daryaft mikonad
	private void execute(final Query query) {

        /*
		final ir.bilgisoft.toopeto.entities.Account account=  query.getAccount();
		if (account.getStatus() == ir.bilgisoft.toopeto.entities.Account.State.ONLINE) {
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid().toString() + ": running mam query " + query.toString());
			ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet = this.mXmppConnectionService.getIqGenerator().queryMessageArchiveManagement(query);
			this.mXmppConnectionService.sendIqPacket(account, packet, new ir.bilgisoft.toopeto.xmpp.OnIqPacketReceived() {
				@Override
				public void onIqPacketReceived(ir.bilgisoft.toopeto.entities.Account account, String packet) {
					if (packet.getType() == ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.ERROR) {
						Log.d(ir.bilgisoft.toopeto.Config.LOGTAG, account.getJid().toBareJid().toString() + ": error executing mam: " + packet.toString());
						finalizeQuery(query);
					}
				}
			});
		} else {
			synchronized (this.pendingQueries) {
				this.pendingQueries.add(query);
			}
		}
        */
	}

	private void finalizeQuery(Query query) {
		synchronized (this.queries) {
			this.queries.remove(query);
		}
		final ir.bilgisoft.toopeto.entities.Conversation conversation = query.getConversation();
		if (conversation != null) {
			conversation.sort();
			if (conversation.setLastMessageTransmitted(query.getEnd())) {
				this.mXmppConnectionService.databaseBackend.updateConversation(conversation);
			}
			conversation.setHasMessagesLeftOnServer(query.getMessageCount() > 0);
			if (query.hasCallback()) {
				query.callback();
			} else {
				this.mXmppConnectionService.updateConversationUi();
			}
		} else {
			for(ir.bilgisoft.toopeto.entities.Conversation tmp : this.mXmppConnectionService.getConversations()) {
				if (tmp.getAccount() == query.getAccount()) {
					tmp.sort();
					if (tmp.setLastMessageTransmitted(query.getEnd())) {
						this.mXmppConnectionService.databaseBackend.updateConversation(tmp);
					}
				}
			}
		}
	}

	public boolean queryInProgress(ir.bilgisoft.toopeto.entities.Conversation conversation, ir.bilgisoft.toopeto.services.XmppConnectionService.OnMoreMessagesLoaded callback) {
		synchronized (this.queries) {
			for(Query query : queries) {
				if (query.conversation == conversation) {
					if (!query.hasCallback() && callback != null) {
						query.setCallback(callback);
					}
					return true;
				}
			}
			return false;
		}
	}

	public void processFin(ir.bilgisoft.toopeto.xml.Element fin) {
		if (fin == null) {
			return;
		}
		Query query = findQuery(fin.getAttribute("queryid"));
		if (query == null) {
			return;
		}
		boolean complete = fin.getAttributeAsBoolean("complete");
		ir.bilgisoft.toopeto.xml.Element set = fin.findChild("set","http://jabber.org/protocol/rsm");
		ir.bilgisoft.toopeto.xml.Element last = set == null ? null : set.findChild("last");
		ir.bilgisoft.toopeto.xml.Element first = set == null ? null : set.findChild("first");
		ir.bilgisoft.toopeto.xml.Element relevant = query.getPagingOrder() == PagingOrder.NORMAL ? last : first;
		boolean abort = (query.getStart() == 0 && query.getTotalCount() >= ir.bilgisoft.toopeto.Config.PAGE_SIZE) || query.getTotalCount() >= ir.bilgisoft.toopeto.Config.MAM_MAX_MESSAGES;
		if (complete || relevant == null || abort) {
			this.finalizeQuery(query);
			Log.d(ir.bilgisoft.toopeto.Config.LOGTAG,query.getAccount().getJid().toBareJid().toString()+": finished mam after "+query.getTotalCount()+" messages");
		} else {
			final Query nextQuery;
			if (query.getPagingOrder() == PagingOrder.NORMAL) {
				nextQuery = query.next(last == null ? null : last.getContent());
			} else {
				nextQuery = query.prev(first == null ? null : first.getContent());
			}
			this.execute(nextQuery);
			this.finalizeQuery(query);
			synchronized (this.queries) {
				this.queries.remove(query);
				this.queries.add(nextQuery);
			}
		}
	}

	public Query findQuery(String id) {
		if (id == null) {
			return null;
		}
		synchronized (this.queries) {
			for(Query query : this.queries) {
				if (query.getQueryId().equals(id)) {
					return query;
				}
			}
			return null;
		}
	}

	@Override
    //karbord an kamel meshakhs nashode
	public void onAdvancedStreamFeaturesAvailable(ir.bilgisoft.toopeto.entities.Account account) {
		if (account.getXmppConnection() != null && account.getXmppConnection().getFeatures().mam()) {
			this.catchup(account);
		}
	}

	public class Query {
		private int totalCount = 0;
		private int messageCount = 0;
		private long start;
		private long end;
		private ir.bilgisoft.toopeto.xmpp.jid.Jid with = null;
		private String queryId;
		private String reference = null;
		private ir.bilgisoft.toopeto.entities.Account account;
		private ir.bilgisoft.toopeto.entities.Conversation conversation;
		private PagingOrder pagingOrder = PagingOrder.NORMAL;
		private ir.bilgisoft.toopeto.services.XmppConnectionService.OnMoreMessagesLoaded callback = null;


		public Query(ir.bilgisoft.toopeto.entities.Conversation conversation, long start, long end) {
			this(conversation.getAccount(), start, end);
			this.conversation = conversation;
			this.with = conversation.getJid();
		}

		public Query(ir.bilgisoft.toopeto.entities.Conversation conversation, long start, long end, PagingOrder order) {
			this(conversation,start,end);
			this.pagingOrder = order;
		}

		public Query(ir.bilgisoft.toopeto.entities.Account account, long start, long end) {
			this.account = account;
			this.start = start;
			this.end = end;
			this.queryId = new BigInteger(50, mXmppConnectionService.getRNG()).toString(32);
		}

		private Query page(String reference) {
			Query query = new Query(this.account,this.start,this.end);
			query.reference = reference;
			query.conversation = conversation;
			query.with = with;
			query.totalCount = totalCount;
			query.callback = callback;
			return query;
		}

		public Query next(String reference) {
			Query query = page(reference);
			query.pagingOrder = PagingOrder.NORMAL;
			return query;
		}

		public Query prev(String reference) {
			Query query = page(reference);
			query.pagingOrder = PagingOrder.REVERSE;
			return query;
		}

		public String getReference() {
			return reference;
		}

		public PagingOrder getPagingOrder() {
			return this.pagingOrder;
		}

		public String getQueryId() {
			return queryId;
		}

		public ir.bilgisoft.toopeto.xmpp.jid.Jid getWith() {
			return with;
		}

		public long getStart() {
			return start;
		}

		public void setCallback(ir.bilgisoft.toopeto.services.XmppConnectionService.OnMoreMessagesLoaded callback) {
			this.callback = callback;
		}

		public void callback() {
			if (this.callback != null) {
				this.callback.onMoreMessagesLoaded(messageCount,conversation);
				if (messageCount == 0) {
					this.callback.informUser(R.string.no_more_history_on_server);
				}
			}
		}

		public long getEnd() {
			return end;
		}

		public ir.bilgisoft.toopeto.entities.Conversation getConversation() {
			return conversation;
		}

		public ir.bilgisoft.toopeto.entities.Account getAccount() {
			return this.account;
		}

		public void incrementTotalCount() {
			this.totalCount++;
		}

		public void incrementMessageCount() {
			this.messageCount++;
		}

		public int getTotalCount() {
			return this.totalCount;
		}

		public int getMessageCount() {
			return this.messageCount;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("with=");
			if (this.with==null) {
				builder.append("*");
			} else {
				builder.append(with.toString());
			}
			builder.append(", start=");
			builder.append(ir.bilgisoft.toopeto.generator.AbstractGenerator.getTimestamp(this.start));
			builder.append(", end=");
			builder.append(ir.bilgisoft.toopeto.generator.AbstractGenerator.getTimestamp(this.end));
			if (this.reference!=null) {
				if (this.pagingOrder == PagingOrder.NORMAL) {
					builder.append(", after=");
				} else {
					builder.append(", before=");
				}
				builder.append(this.reference);
			}
			return builder.toString();
		}

		public boolean hasCallback() {
			return this.callback != null;
		}
	}
}
