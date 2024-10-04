package ir.bilgisoft.toopeto.generator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IqGenerator extends ir.bilgisoft.toopeto.generator.AbstractGenerator {

	public IqGenerator(final ir.bilgisoft.toopeto.services.XmppConnectionService service) {
		super(service);
	}

	public ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket discoResponse(final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket request) {
		final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.RESULT);
		packet.setId(request.getId());
		packet.setTo(request.getFrom());
		final ir.bilgisoft.toopeto.xml.Element query = packet.addChild("query",
				"http://jabber.org/protocol/disco#info");
		query.setAttribute("node", request.query().getAttribute("node"));
		final ir.bilgisoft.toopeto.xml.Element identity = query.addChild("identity");
		identity.setAttribute("category", "client");
		identity.setAttribute("type", this.IDENTITY_TYPE);
		identity.setAttribute("name", IDENTITY_NAME);
		final List<String> features = Arrays.asList(FEATURES);
		Collections.sort(features);
		for (final String feature : features) {
			query.addChild("feature").setAttribute("var", feature);
		}
		return packet;
	}

	protected ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket publish(final String node, final ir.bilgisoft.toopeto.xml.Element item) {
		final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.SET);
		final ir.bilgisoft.toopeto.xml.Element pubsub = packet.addChild("pubsub",
				"http://jabber.org/protocol/pubsub");
		final ir.bilgisoft.toopeto.xml.Element publish = pubsub.addChild("publish");
		publish.setAttribute("node", node);
		publish.addChild(item);
		return packet;
	}

	protected ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket retrieve(String node, ir.bilgisoft.toopeto.xml.Element item) {
		final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.GET);
		final ir.bilgisoft.toopeto.xml.Element pubsub = packet.addChild("pubsub",
				"http://jabber.org/protocol/pubsub");
		final ir.bilgisoft.toopeto.xml.Element items = pubsub.addChild("items");
		items.setAttribute("node", node);
		if (item != null) {
			items.addChild(item);
		}
		return packet;
	}

	public ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket publishAvatar(ir.bilgisoft.toopeto.xmpp.pep.Avatar avatar) {
		final ir.bilgisoft.toopeto.xml.Element item = new ir.bilgisoft.toopeto.xml.Element("item");
		item.setAttribute("id", avatar.sha1sum);
		final ir.bilgisoft.toopeto.xml.Element data = item.addChild("data", "urn:xmpp:avatar:data");
		data.setContent(avatar.image);
		return publish("urn:xmpp:avatar:data", item);
	}

	public ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket publishAvatarMetadata(final ir.bilgisoft.toopeto.xmpp.pep.Avatar avatar) {
		final ir.bilgisoft.toopeto.xml.Element item = new ir.bilgisoft.toopeto.xml.Element("item");
		item.setAttribute("id", avatar.sha1sum);
		final ir.bilgisoft.toopeto.xml.Element metadata = item
			.addChild("metadata", "urn:xmpp:avatar:metadata");
		final ir.bilgisoft.toopeto.xml.Element info = metadata.addChild("info");
		info.setAttribute("bytes", avatar.size);
		info.setAttribute("id", avatar.sha1sum);
		info.setAttribute("height", avatar.height);
		info.setAttribute("width", avatar.height);
		info.setAttribute("type", avatar.type);
		return publish("urn:xmpp:avatar:metadata", item);
	}

	public ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket retrieveAvatar(final ir.bilgisoft.toopeto.xmpp.pep.Avatar avatar) {
		final ir.bilgisoft.toopeto.xml.Element item = new ir.bilgisoft.toopeto.xml.Element("item");
		item.setAttribute("id", avatar.sha1sum);
		final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet = retrieve("urn:xmpp:avatar:data", item);
		packet.setTo(avatar.owner);
		return packet;
	}

	public ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket retrieveAvatarMetaData(final ir.bilgisoft.toopeto.xmpp.jid.Jid to) {
		final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet = retrieve("urn:xmpp:avatar:metadata", null);
		if (to != null) {
			packet.setTo(to);
		}
		return packet;
	}

	public ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket queryMessageArchiveManagement(final ir.bilgisoft.toopeto.services.MessageArchiveService.Query mam) {
		final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.SET);
		final ir.bilgisoft.toopeto.xml.Element query = packet.query("urn:xmpp:mam:0");
		query.setAttribute("queryid",mam.getQueryId());
		final ir.bilgisoft.toopeto.xmpp.forms.Data data = new ir.bilgisoft.toopeto.xmpp.forms.Data();
		data.setFormType("urn:xmpp:mam:0");
		if (mam.getWith()!=null) {
			data.put("with", mam.getWith().toString());
		}
		data.put("start",getTimestamp(mam.getStart()));
		data.put("end",getTimestamp(mam.getEnd()));
		query.addChild(data);
		if (mam.getPagingOrder() == ir.bilgisoft.toopeto.services.MessageArchiveService.PagingOrder.REVERSE) {
			query.addChild("set", "http://jabber.org/protocol/rsm").addChild("before").setContent(mam.getReference());
		} else if (mam.getReference() != null) {
			query.addChild("set", "http://jabber.org/protocol/rsm").addChild("after").setContent(mam.getReference());
		}
		return packet;
	}
	public ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket generateGetBlockList() {
		final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iq = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.GET);
		iq.addChild("blocklist", ir.bilgisoft.toopeto.utils.Xmlns.BLOCKING);

		return iq;
	}

	public ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket generateSetBlockRequest(final ir.bilgisoft.toopeto.xmpp.jid.Jid jid) {
		final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iq = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.SET);
		final ir.bilgisoft.toopeto.xml.Element block = iq.addChild("block", ir.bilgisoft.toopeto.utils.Xmlns.BLOCKING);
		block.addChild("item").setAttribute("jid", jid.toBareJid().toString());
		return iq;
	}

	public ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket generateSetUnblockRequest(final ir.bilgisoft.toopeto.xmpp.jid.Jid jid) {

		final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iq = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.SET);
		final ir.bilgisoft.toopeto.xml.Element block = iq.addChild("unblock", ir.bilgisoft.toopeto.utils.Xmlns.BLOCKING);
		block.addChild("item").setAttribute("jid", jid.toBareJid().toString());
		return iq;

	}

	public ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket generateSetPassword(final ir.bilgisoft.toopeto.entities.Account account, final String newPassword) {
		final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket packet = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.SET);
		packet.setTo(account.getServer());
		final ir.bilgisoft.toopeto.xml.Element query = packet.addChild("query", ir.bilgisoft.toopeto.utils.Xmlns.REGISTER);
		final ir.bilgisoft.toopeto.xmpp.jid.Jid jid = account.getJid();
		query.addChild("username").setContent(jid.getLocalpart());
		query.addChild("password").setContent(newPassword);
		return packet;
	}
    public ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket generateSearchRooms(final String fieldNameValue,int lastIndex,String serverName) {
        final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iqPacket = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.GET);

        iqPacket.setAttribute("to", "conference."+ serverName);

        //final Element query = iqPacket.query("http://jabber.org/protocol/disco#items");
        final ir.bilgisoft.toopeto.xml.Element query = iqPacket.query( "jabber:iq:search");

        final ir.bilgisoft.toopeto.xml.Element maxCount = new ir.bilgisoft.toopeto.xml.Element("max");
        maxCount.setContent("10");

        final ir.bilgisoft.toopeto.xml.Element lastIndexElm = new ir.bilgisoft.toopeto.xml.Element("index");
        lastIndexElm.setContent(Integer.toString(lastIndex));

        final ir.bilgisoft.toopeto.xml.Element setItem = new ir.bilgisoft.toopeto.xml.Element("set");
        setItem.setAttribute("xmlns", "http://jabber.org/protocol/rsm");
        setItem.addChild(maxCount);
        setItem.addChild(lastIndexElm);
        //  if (lastRoom != "") {
        //    setItem.addChild(lastItem);
      // }

        query.addChild(setItem);


        final ir.bilgisoft.toopeto.xml.Element x = new ir.bilgisoft.toopeto.xml.Element("x");
        x.setAttribute("type","get");
        x.setAttribute("xmlns","jabber:x:data");

        final ir.bilgisoft.toopeto.xml.Element fieldName= new ir.bilgisoft.toopeto.xml.Element("field");
        fieldName.setAttribute("var","name");
        ir.bilgisoft.toopeto.xml.Element valueName=new ir.bilgisoft.toopeto.xml.Element("value");
        valueName.setContent(fieldNameValue);
        fieldName.addChild(valueName);
        x.addChild(fieldName);

        query.addChild(x);
        return iqPacket;
    }
// bayd dorost shavad
    /*<iq type='set'  to='users.jabber.org' id='limit1'>
  <query xmlns='jabber:iq:search'>
    <name>ba</name>
    <set xmlns='http://jabber.org/protocol/rsm'>
      <max>10</max>
    </set>
  </query>
</iq>*/
    public ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket generateSearchUsers(final String fieldNameValue,int lastIndex,String serverName ) {
        final ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket iqPacket = new ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket(ir.bilgisoft.toopeto.xmpp.stanzas.IqPacket.TYPE.GET);

        iqPacket.setAttribute("to","characters."+serverName);// "plugin.search."+serverName);

     // final Element query = iqPacket.query("http://jabber.org/protocol/disco#items");
       final ir.bilgisoft.toopeto.xml.Element query = iqPacket.query( "jabber:iq:search");
  //  final Element query = iqPacket.query( "jabber:iq:search");

       final ir.bilgisoft.toopeto.xml.Element maxCount = new ir.bilgisoft.toopeto.xml.Element("name");
       maxCount.setContent(fieldNameValue);
       query.addChild(maxCount);
      //  maxCount.setContent("10");

       // final Element lastIndexElm = new Element("index");
      //  lastIndexElm.setContent(Integer.toString(lastIndex));

      //  final Element setItem = new Element("set");
       // setItem.setAttribute("xmlns", "http://jabber.org/protocol/rsm");
       // setItem.addChild(maxCount);
      //  setItem.addChild(lastIndexElm);

     //   query.addChild(setItem);


       // final Element name = new Element("name");
      //  name.setContent("ba");
       // x.setAttribute("type","get");
       // x.setAttribute("xmlns","jabber:x:data");

       // final Element fieldName= new Element("field");
      //  fieldName.setAttribute("var","name");
       // Element valueName=new Element("value");
       // valueName.setContent(fieldNameValue);
       // fieldName.addChild(valueName);
      //  x.addChild(fieldName);

       // query.addChild(name);
     //   iqPacket.addChild(maxCount);
        return iqPacket;
    }
}
