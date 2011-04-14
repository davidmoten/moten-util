package org.moten.david.util.xsd.form.client;

import java.util.Date;
import java.util.List;

import org.moten.david.util.xsd.simplified.Choice;
import org.moten.david.util.xsd.simplified.ComplexType;
import org.moten.david.util.xsd.simplified.Element;
import org.moten.david.util.xsd.simplified.Group;
import org.moten.david.util.xsd.simplified.Particle;
import org.moten.david.util.xsd.simplified.Restriction;
import org.moten.david.util.xsd.simplified.Schema;
import org.moten.david.util.xsd.simplified.Sequence;
import org.moten.david.util.xsd.simplified.SimpleType;
import org.moten.david.util.xsd.simplified.Type;
import org.moten.david.util.xsd.simplified.XsdType;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DatePicker;

public class SchemaPanel extends VerticalPanel {

	private final Schema schema;

	public SchemaPanel(Schema schema) {
		this.schema = schema;
		Label namespace = new Label(schema.getNamespace());
		// add(namespace);
		for (Element element : schema.getElements()) {
			add(createElementPanel(element));
		}
		Button submit = new Button("Submit");
		add(submit);
	}

	private Widget createElementPanel(Element element) {
		// use a parent so we can add multiple elements if required
		final VerticalPanel parent = new VerticalPanel();
		parent.add(createElementPanel(parent, element));
		return decorate(parent);
	}

	private Label createLabel(String name) {
		Label label = new Label(name);
		label.addStyleName("label");
		return label;
	}

	private Widget border(Widget w) {
		VerticalPanel outer = new VerticalPanel();
		outer.add(new HTML("<hr/>"));
		outer.add(w);
		return outer;
	}

	private Widget createElementPanel(final VerticalPanel parent,
			final Element element) {
		final VerticalPanel p = new VerticalPanel();
		if (element.getBefore() != null) {
			HTML html = new HTML(element.getBefore());
			p.add(html);
			html.addStyleName("before");
		}
		Type t = getType(schema, element);
		if (t instanceof ComplexType) {
			p.add(createComplexTypePanel(element.getDisplayName(),
					(ComplexType) t));
		} else if (t instanceof SimpleType)
			p.add(createSimpleType(element.getDisplayName(),
					element.getDescription(), element.getValidation(),
					element.getLines(), element.getCols(), (SimpleType) t,
					element.getMinOccurs()));
		else
			throw new RuntimeException("could not find type: "
					+ element.getType());
		if (element.getMaxOccurs() != null
				&& (element.getMaxOccurs().isUnbounded() || element
						.getMaxOccurs().getMaxOccurs() > 1)) {
			final HorizontalPanel h = new HorizontalPanel();
			h.addStyleName("add");
			final Button add = new Button("Add");
			h.add(add);
			final Button remove = new Button("Remove");
			h.add(remove);
			p.add(h);
			add.addClickHandler(new ClickHandler() {

				public void onClick(ClickEvent event) {
					// h.setVisible(false);
					parent.add(createElementPanel(parent, element));
				}
			});
			remove.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					p.setVisible(false);
				}
			});
		}
		if (element.getAfter() != null) {
			HTML html = new HTML(element.getAfter());
			p.add(html);
			html.addStyleName("after");
		}
		return border(decorate(p));
	}

	private Widget createSimpleType(String name, String description,
			final String validationMessage, Integer lines, Integer cols,
			final SimpleType t, final int minOccurs) {

		HorizontalPanel p = new HorizontalPanel();
		if (t.getRestriction() != null) {
			addRestrictionWidget(p, t, name, description, validationMessage);
		} else if (t.getName().getLocalPart().equals("boolean")) {
			// checkboxes
			CheckBox c = new CheckBox(name);
			c.addStyleName("item");
			p.add(addDescription(c, description));
		} else if (t.getName().getLocalPart().equals("date")) {
			addDateWidget(p, name);
		} else if (t.getName().getLocalPart().equals("dateTime")) {
			p.add(new Label("TODO dateTime"));
		} else {
			addTextWidget(p, name, description, lines, cols, minOccurs);
		}
		return decorate(p);
	}

	private void addTextWidget(HorizontalPanel p, String name,
			String description, Integer lines, Integer cols, final int minOccurs) {
		// plain text box
		p.add(createLabel(name));
		final TextBoxBase text;
		if (lines != null && lines > 1) {
			TextArea textArea = new TextArea();
			textArea.setVisibleLines(lines);
			if (cols != null && cols > 0)
				textArea.setCharacterWidth(cols);
			else
				textArea.setCharacterWidth(50);
			text = textArea;
			text.addStyleName("textArea");
		} else {
			text = new TextBox();
			text.addStyleName("item");
		}
		final Label validation = new Label();
		validation.setVisible(false);
		validation.addStyleName("validation");
		text.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				boolean isValid = (text.getText() != null && text.getText()
						.trim().length() > 0)
						|| minOccurs == 0;
				updateValidation(isValid, text, validation, "mandatory");
			}
		});

		VerticalPanel vp = new VerticalPanel();
		vp.add(text);
		vp.add(addDescription(validation, description));
		p.add(vp);
	}

	private void addDateWidget(HorizontalPanel p, String name) {
		final Label label = createLabel(name);
		p.add(label);
		final TextBox text = new TextBox();
		text.setReadOnly(true);
		text.addStyleName("item");
		p.add(text);

		// Create a date picker
		DatePicker datePicker = new DatePicker();

		// Set the value in the text box when the user selects a date
		datePicker.addValueChangeHandler(new ValueChangeHandler<Date>() {
			public void onValueChange(ValueChangeEvent<Date> event) {
				Date date = event.getValue();
				String dateString = DateTimeFormat.getFormat(
						PredefinedFormat.DATE_FULL).format(date);
				text.setText(dateString);
			}
		});

		// Set the default value
		datePicker.setValue(new Date(), true);
		datePicker.addStyleName("item");
		DisclosurePanel d = new DisclosurePanel("");
		d.setContent(datePicker);
		p.add(d);

	}

	private void addRestrictionWidget(HorizontalPanel p, SimpleType t,
			String name, String description, String validationMessage) {
		// list boxes
		p.add(createLabel(name));
		List<XsdType<?>> xsdTypes = t.getRestriction().getEnumerations();
		if (xsdTypes.size() > 0) {
			ListBox listBox = new ListBox();
			for (XsdType<?> x : xsdTypes) {
				listBox.addItem(x.getValue().toString());
			}
			listBox.addStyleName("item");
			p.add(addDescription(listBox, description));
		} else if (t.getRestriction().getPattern() != null) {
			// patterns
			Widget w = createPatternWidget(t.getRestriction().getPattern(),
					description, validationMessage);
			p.add(w);
		} else if (t.getRestriction().getBase() != null
				&& t.getRestriction().getBase().getLocalPart()
						.equals("integer")) {
			final IntegerBox text = new IntegerBox();
			text.setText("");
			text.addStyleName("item");

			final Label validation = new Label(validationMessage);
			validation.setVisible(false);
			validation.addStyleName("validation");
			text.addChangeHandler(createIntegerChangeHandler(
					t.getRestriction(), text, validation));

			VerticalPanel vp = new VerticalPanel();
			vp.add(text);
			vp.add(addDescription(validation, description));

			p.add(vp);
		} else {
			// plain text box
			p.add(createLabel(name));
			TextBox text = new TextBox();
			text.setText(t.getName().getLocalPart() + "unsupported restriction");
			text.addStyleName("item");
			p.add(addDescription(text, description));
		}

	}

	private Widget createPatternWidget(String pattern, String description,
			String validationMessage) {
		final TextBox text = new TextBox();
		text.setText("");
		text.addStyleName("item");

		final Label validation = new Label(validationMessage);
		validation.setVisible(false);
		validation.addStyleName("validation");

		text.addChangeHandler(createPatternChangeHandler(pattern, text,
				validationMessage, validation));

		VerticalPanel vp = new VerticalPanel();
		vp.add(text);
		vp.add(addDescription(validation, description));
		return vp;
	}

	private ChangeHandler createIntegerChangeHandler(
			final Restriction restriction, final IntegerBox item,
			final Label validation) {
		return new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				boolean isValid = true;
				int i = 0;
				try {
					i = Integer.parseInt(item.getText());
				} catch (NumberFormatException e) {
					isValid = false;
				}
				if (restriction.getMinInclusive() != null
						&& i < restriction.getMinInclusive().doubleValue())
					isValid = false;
				if (restriction.getMinExclusive() != null
						&& i <= restriction.getMinExclusive().doubleValue())
					isValid = false;
				if (restriction.getMaxInclusive() != null
						&& i > restriction.getMaxInclusive().doubleValue())
					isValid = false;
				if (restriction.getMaxExclusive() != null
						&& i >= restriction.getMaxExclusive().doubleValue())
					isValid = false;
				updateValidation(isValid, item, validation, "invalid");
			}
		};
	}

	private void updateValidation(boolean isValid, Widget item,
			Label validation, String defaultValidationMessage) {
		if (!isValid) {
			if (validation.getText() == null
					|| validation.getText().trim().length() == 0)
				validation.setText(defaultValidationMessage);
			validation.setVisible(true);
			item.addStyleName("invalidItem");
		} else {
			validation.setVisible(false);
			item.removeStyleName("invalidItem");
		}
	}

	private ChangeHandler createPatternChangeHandler(final String pattern,
			final TextBoxBase text, final String validationMessage,
			final Label validation) {
		return new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				RegExp regex = RegExp.compile("^" + pattern + "$");
				boolean isValid = regex.test(text.getText());
				updateValidation(isValid, text, validation, "invalid format");
			}
		};
	}

	private Widget addDescription(Widget widget, String description) {
		VerticalPanel vp = new VerticalPanel();
		vp.add(widget);
		if (description != null) {
			Label label = new Label(description);
			vp.add(label);
			label.addStyleName("description");
		}
		return vp;
	}

	private Widget createComplexTypePanel(String displayName, ComplexType t) {
		VerticalPanel p = new VerticalPanel();
		p.add(new Label(displayName));
		for (Particle particle : t.getParticles()) {
			p.add(createParticle(particle));
		}
		return decorate(p);
	}

	private Widget createParticle(Particle particle) {
		VerticalPanel p = new VerticalPanel();
		if (particle instanceof Element)
			p.add(createElementPanel((Element) particle));
		else if (particle instanceof SimpleType)
			p.add(createSimpleType(particle.getClass().getName(), null, null,
					null, null, (SimpleType) particle, 1));
		else if (particle instanceof Group)
			p.add(createGroup((Group) particle));
		else
			throw new RuntimeException("unknown particle:" + particle);
		return decorate(p);
	}

	private Widget decorate(Widget w) {
		VerticalPanel p = new VerticalPanel();
		p.add(w);
		p.addStyleName("box");
		return p;
	}

	private int groupCount = 1;

	private synchronized int nextGroup() {
		return groupCount++;
	}

	private Widget createGroup(Group group) {
		VerticalPanel p = new VerticalPanel();
		if (group instanceof Choice) {
			String groupName = "group" + nextGroup();
			boolean first = true;
			final Widget[] lastChecked = new Widget[] { null };
			int count = 1;
			for (Particle particle : group.getParticles()) {
				RadioButton rb = new RadioButton(groupName, "option " + count);
				count++;
				p.add(rb);
				final Widget particlePanel = createParticle(particle);
				particlePanel.addStyleName("uncheckedRadioButtonContent");
				rb.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent event) {
						if (lastChecked[0] != null) {
							lastChecked[0]
									.addStyleName("uncheckedRadioButtonContent");
						}
						particlePanel
								.removeStyleName("uncheckedRadioButtonContent");
						particlePanel.addStyleName("checkedRadioButtonContent");
						lastChecked[0] = particlePanel;
					}
				});
				p.add(particlePanel);
				if (first) {
					rb.setValue(true);
					particlePanel.removeStyleName("checkedRadioButtonContent");
					lastChecked[0] = particlePanel;
				}
				first = false;
			}
		} else if (group instanceof Sequence) {
			p.add(new Label("sequence"));
			for (Particle particle : group.getParticles()) {
				final Widget particlePanel = createParticle(particle);
				p.add(particlePanel);
			}
		}
		return decorate(p);
	}

	private Type getType(Schema schema, Element element) {
		for (ComplexType t : schema.getComplexTypes())
			if (element.getType().equals(t.getQName())) {
				return t;
			}
		for (SimpleType t : schema.getSimpleTypes())
			if (element.getType().equals(t.getQName())) {
				return t;
			}
		// otherwise assume is an xsd simpleType
		return new SimpleType(element.getType());

	}
}