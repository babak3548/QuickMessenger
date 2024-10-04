package ir.bilgisoft.toopeto.xmpp.forms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Data extends ir.bilgisoft.toopeto.xml.Element {

	public Data() {
		super("x");
		this.setAttribute("xmlns","jabber:x:data");
	}

	public List<ir.bilgisoft.toopeto.xmpp.forms.Field> getFields() {
		ArrayList<ir.bilgisoft.toopeto.xmpp.forms.Field> fields = new ArrayList<ir.bilgisoft.toopeto.xmpp.forms.Field>();
		for(ir.bilgisoft.toopeto.xml.Element child : getChildren()) {
			if (child.getName().equals("field")) {
				fields.add(ir.bilgisoft.toopeto.xmpp.forms.Field.parse(child));
			}
		}
		return fields;
	}

	public ir.bilgisoft.toopeto.xmpp.forms.Field getFieldByName(String needle) {
		for(ir.bilgisoft.toopeto.xml.Element child : getChildren()) {
			if (child.getName().equals("field") && needle.equals(child.getAttribute("var"))) {
				return ir.bilgisoft.toopeto.xmpp.forms.Field.parse(child);
			}
		}
		return null;
	}

	public void put(String name, String value) {
		ir.bilgisoft.toopeto.xmpp.forms.Field field = getFieldByName(name);
		if (field == null) {
			field = new ir.bilgisoft.toopeto.xmpp.forms.Field(name);
			this.addChild(field);
		}
		field.setValue(value);
	}

	public void put(String name, Collection<String> values) {
		ir.bilgisoft.toopeto.xmpp.forms.Field field = getFieldByName(name);
		if (field == null) {
			field = new ir.bilgisoft.toopeto.xmpp.forms.Field(name);
			this.addChild(field);
		}
		field.setValues(values);
	}

	public void submit() {
		this.setAttribute("type","submit");
		removeNonFieldChildren();
		for(ir.bilgisoft.toopeto.xmpp.forms.Field field : getFields()) {
			field.removeNonValueChildren();
		}
	}

	private void removeNonFieldChildren() {
		for(Iterator<ir.bilgisoft.toopeto.xml.Element> iterator = this.children.iterator(); iterator.hasNext();) {
			ir.bilgisoft.toopeto.xml.Element element = iterator.next();
			if (!element.getName().equals("field")) {
				iterator.remove();
			}
		}
	}

	public static Data parse(ir.bilgisoft.toopeto.xml.Element element) {
		Data data = new Data();
		data.setAttributes(element.getAttributes());
		data.setChildren(element.getChildren());
		return data;
	}

	public void setFormType(String formType) {
		this.put("FORM_TYPE",formType);
	}

	public String getFormType() {
		return this.getAttribute("FORM_TYPE");
	}
}
