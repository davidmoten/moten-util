package moten.david.markup;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import moten.david.markup.events.DocumentSelectionChanged;
import moten.david.markup.xml.study.Document;
import moten.david.util.controller.Controller;

import com.google.inject.Inject;

public class DocumentsPanel extends JPanel {

    private static final long serialVersionUID = -7045040788786395194L;
    private final Controller controller;

    @Inject
    public DocumentsPanel(Controller controller, CurrentStudy study) {
        this.controller = controller;
        List<DocumentWrapper> documents = new ArrayList<DocumentWrapper>();
        for (Document document : study.get().getDocument()) {
            documents.add(new DocumentWrapper(document));
        }
        JList list = new JList(documents.toArray());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setLayout(new GridLayout(1, 1));
        add(list);
        list.setSelectedIndex(0);
        list.addListSelectionListener(createListSelectionListener(list));
    }

    private ListSelectionListener createListSelectionListener(final JList list) {
        return new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                controller.event(new DocumentSelectionChanged(list
                        .getSelectedIndex()));
            }

        };
    }
}
