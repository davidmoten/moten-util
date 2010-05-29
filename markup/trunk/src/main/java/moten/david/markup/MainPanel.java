package moten.david.markup;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import moten.david.markup.events.TagSelectionChanged;
import moten.david.markup.events.TagsChanged;
import moten.david.markup.events.TextTagged;
import moten.david.util.controller.Controller;
import moten.david.util.controller.ControllerListener;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXMultiSplitPane;
import org.jdesktop.swingx.MultiSplitLayout;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * @author dave
 * 
 */
public class MainPanel extends JPanel {

    private static Logger log = Logger.getLogger(MainPanel.class.getName());

    private final JTextPane text;
    private final Controller controller;
    private List<Tag> tags;
    private SelectionMode selectionMode = SelectionMode.SENTENCE;
    private final Set<DocumentTag> documentTags = new HashSet<DocumentTag>();
    private Set<Tag> visibleTags = new HashSet<Tag>();

    private final TagsLoader tagsLoader;

    @Inject
    public MainPanel(Controller controller, TagsLoader tagsLoader) {
        this.controller = controller;
        this.tagsLoader = tagsLoader;
        setLayout(new GridLayout(1, 1));
        text = new JTextPane();
        JScrollPane scroll = new JScrollPane(text);
        add(scroll);
        controller.addListener(TagSelectionChanged.class,
                createTagSelectionChangedListener());
        controller.addListener(TagsChanged.class, createTagsChangedListener());
        init();
    }

    private ControllerListener<TagsChanged> createTagsChangedListener() {
        return new ControllerListener<TagsChanged>() {

            @Override
            public void event(TagsChanged event) {
                tags = event.getTags();
            }
        };
    }

    private ControllerListener<TagSelectionChanged> createTagSelectionChangedListener() {
        return new ControllerListener<TagSelectionChanged>() {

            @Override
            public void event(TagSelectionChanged event) {
                visibleTags = ImmutableSet.of(event.getList().toArray(
                        new Tag[] {}));
                refresh();
            }
        };
    }

    private enum SelectionMode {
        EXACT, WORD, SENTENCE, PARAGRAPH;
    }

    private void refresh() {
        log.info("refreshing");
        StyledDocument doc = text.getStyledDocument();
        doc.setCharacterAttributes(0, doc.getLength(),
                SimpleAttributeSet.EMPTY, true);
        for (DocumentTag documentTag : documentTags) {
            if (visibleTags.contains(documentTag.getTag())) {
                Style style = doc
                        .addStyle(documentTag.getTag().getName(), null);
                StyleConstants.setBackground(style, documentTag.getTag()
                        .getColor());
                doc.setCharacterAttributes(documentTag.getStart(), documentTag
                        .getLength(), doc.getStyle(documentTag.getTag()
                        .getName()), true);
            }
        }
        text.setStyledDocument(doc);
    }

    private MouseListener createTextMouseListener(List<Tag> tags) {
        final JPopupMenu popup = new JPopupMenu();
        for (final Tag tag : tags) {
            JMenuItem code = new JMenuItem(tag.getName());
            popup.add(code);
            code.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int start = text.getSelectionStart();
                    int finish = text.getSelectionEnd();
                    StyledDocument doc = text.getStyledDocument();
                    Element element = doc.getParagraphElement(start);
                    Element element2 = doc.getParagraphElement(finish);

                    if (selectionMode.equals(SelectionMode.PARAGRAPH))
                        documentTags.add(new DocumentTag(tag, element
                                .getStartOffset(), element2.getEndOffset()
                                - element.getStartOffset() + 1));
                    else if (selectionMode.equals(SelectionMode.SENTENCE)) {
                        selectDelimitedBy(doc, ".", start, finish, tag
                                .getName());
                    } else {
                        // exact
                        documentTags.add(new DocumentTag(tag, start, finish
                                - start + 1));
                    }
                    controller.event(new TextTagged(tag));
                    refresh();
                }

                private void selectDelimitedBy(StyledDocument doc,
                        String delimiter, int start, int finish, String style) {
                    Element element = doc.getParagraphElement(start);
                    Element element2 = doc.getParagraphElement(finish);
                    int i = start;
                    try {
                        while (i > element.getStartOffset()
                                && !delimiter.equals(doc.getText(i, 1)))
                            i--;
                        while (delimiter.equals(doc.getText(i, 1))
                                || Character.isWhitespace(doc.getText(i, 1)
                                        .charAt(0)))
                            i++;
                        int j = finish;
                        while (j < element2.getEndOffset()
                                && !delimiter.equals(doc.getText(j, 1)))
                            j++;
                        if (delimiter.equals(doc.getText(j, 1))
                                || Character.isWhitespace(doc.getText(j, 1)
                                        .charAt(0)))
                            j--;
                        documentTags.add(new DocumentTag(tag, i, j - i + 1));
                    } catch (BadLocationException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            });
        }

        return new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    StyledDocument doc = text.getStyledDocument();
                    int i = text.getSelectionStart();
                    Enumeration<?> names = doc.getCharacterElement(i)
                            .getAttributes().getAttributeNames();
                    while (names.hasMoreElements()) {
                        Object attribute = names.nextElement();
                        System.out.println(attribute.getClass().getName() + ":"
                                + attribute);
                    }
                }
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }

        };
    }

    @Override
    public void requestFocus() {
        super.requestFocus();
        text.requestFocus();
    }

    private void init() {
        try {
            String s = IOUtils.toString(getClass().getResourceAsStream(
                    "/example.txt"));
            text.setText(s);
            tagsLoader.load();
            text.addMouseListener(createTextMouseListener(tags));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        {
            JMenu menu = new JMenu("File", true);
            JMenuItem fileOpen = new JMenuItem("Open...");
            menu.add(fileOpen);
            menuBar.add(menu);
        }
        {
            JMenu menu = new JMenu("Selection", true);
            ButtonGroup group = new ButtonGroup();
            for (final SelectionMode mode : SelectionMode.values()) {
                JRadioButtonMenuItem item = new JRadioButtonMenuItem(
                        StringUtils.capitalize(mode.toString().toLowerCase()),
                        mode.equals(selectionMode));
                menu.add(item);
                group.add(item);
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        selectionMode = mode;
                        System.out.println(selectionMode);
                    }
                });
            }
            menuBar.add(menu);
        }

        return menuBar;
    }

    private JXMultiSplitPane createMultiSplitPane(JComponent panel,
            JComponent panel2) {

        JXMultiSplitPane msp = new JXMultiSplitPane();

        String layoutDef = "(COLUMN (ROW weight=0.8 (COLUMN weight=0.25 "
                + "(LEAF name=left.top weight=0.5) (LEAF name=left.middle weight=0.5))"
                + "(LEAF name=editor weight=0.75)) (LEAF name=bottom weight=0.2))";

        layoutDef = "(ROW (LEAF name=left weight=0.25) (LEAF name=right weight=0.75))";

        MultiSplitLayout.Node modelRoot = MultiSplitLayout
                .parseModel(layoutDef);
        msp.getMultiSplitLayout().setModel(modelRoot);

        msp.add(panel, "left");
        msp.add(panel2, "right");

        // ADDING A BORDER TO THE MULTISPLITPANE CAUSES ALL SORTS OF ISSUES
        msp.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        return msp;
    }

    public static void main(String[] args) throws InterruptedException,
            InvocationTargetException, ClassNotFoundException,
            InstantiationException, IllegalAccessException,
            UnsupportedLookAndFeelException {
        UIManager
                .setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                Injector injector = Guice.createInjector(new InjectorModule());
                final JFrame frame = new JFrame("Markup");
                final TagsPanel tagsPanel = injector
                        .getInstance(TagsPanel.class);
                final MainPanel panel = injector.getInstance(MainPanel.class);

                frame.setJMenuBar(panel.createMenuBar());
                frame.setLayout(new GridLayout(1, 1));
                Toolkit tk = Toolkit.getDefaultToolkit();
                Dimension screenSize = tk.getScreenSize();
                int screenHeight = screenSize.height;
                int screenWidth = screenSize.width;
                frame.setSize(screenWidth / 2, screenHeight / 3 * 2);
                frame.setLocationRelativeTo(null);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().add(
                        panel.createMultiSplitPane(tagsPanel, panel));

                frame.setVisible(true);
                panel.requestFocus();
            }
        });

    }

}