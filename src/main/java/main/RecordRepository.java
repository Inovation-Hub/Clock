package main;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecordRepository {

    private static final String FILE_NAME = "clock_records.xml";
    private static final String ROOT_TAG  = "records";
    private static final String ENTRY_TAG = "record";

    private final File file;

    public RecordRepository() {
        this(new File(System.getProperty("user.home"), FILE_NAME));
    }

    public RecordRepository(File file) {
        this.file = file;
    }

    public List<TimeRecord> loadAll() {
        List<TimeRecord> list = new ArrayList<>();
        if (!file.exists()) return list;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = factory.newDocumentBuilder().parse(file);
            doc.getDocumentElement().normalize();

            NodeList nodes = doc.getElementsByTagName(ENTRY_TAG);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                list.add(new TimeRecord(
                    text(el, "timestamp"),
                    text(el, "duration"),
                    Long.parseLong(text(el, "totalSeconds")),
                    text(el, "description")
                ));
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar registros: " + e.getMessage());
        }
        return list;
    }

    public void append(TimeRecord record) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc;
            Element root;

            if (file.exists()) {
                doc  = builder.parse(file);
                root = doc.getDocumentElement();
            } else {
                doc  = builder.newDocument();
                root = doc.createElement(ROOT_TAG);
                doc.appendChild(root);
            }

            Element entry = doc.createElement(ENTRY_TAG);
            addChild(doc, entry, "timestamp",    record.timestamp);
            addChild(doc, entry, "duration",     record.duration);
            addChild(doc, entry, "totalSeconds", String.valueOf(record.totalSeconds));
            addChild(doc, entry, "description",  record.description);
            root.appendChild(entry);

            Transformer tf = TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tf.transform(new DOMSource(doc), new StreamResult(file));

        } catch (Exception e) {
            System.err.println("Erro ao salvar registro: " + e.getMessage());
        }
    }

    public void saveAll(List<TimeRecord> records) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = factory.newDocumentBuilder().newDocument();
            Element root = doc.createElement(ROOT_TAG);
            doc.appendChild(root);

            for (TimeRecord record : records) {
                Element entry = doc.createElement(ENTRY_TAG);
                addChild(doc, entry, "timestamp",    record.timestamp);
                addChild(doc, entry, "duration",     record.duration);
                addChild(doc, entry, "totalSeconds", String.valueOf(record.totalSeconds));
                addChild(doc, entry, "description",  record.description);
                root.appendChild(entry);
            }

            Transformer tf = TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tf.transform(new DOMSource(doc), new StreamResult(file));
        } catch (Exception e) {
            System.err.println("Erro ao salvar registros: " + e.getMessage());
        }
    }

    private static String text(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        return nl.getLength() > 0 ? nl.item(0).getTextContent() : "";
    }

    private static void addChild(Document doc, Element parent, String tag, String value) {
        Element child = doc.createElement(tag);
        child.setTextContent(value);
        parent.appendChild(child);
    }
}
