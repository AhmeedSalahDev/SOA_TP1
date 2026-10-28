package tn.fss.soa;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * TP1 - Architecture SOA et services web.
 * Classe principale : (1) validation XML/XSD, (2) interrogation XPath
 * sur le catalogue de la plateforme de streaming CineStream.
 */
public class Main {

    private static final String XSD = "cinestream.xsd";
    private static final String XML_VALIDE = "cinestream.xml";
    private static final String XML_INVALIDE = "cinestream-invalide.xml";

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. Validation XML / XSD ===");
        List<String> erreurs = valider(XML_VALIDE, XSD);
        afficherResultatValidation(XML_VALIDE, erreurs);

        List<String> erreursInv = valider(XML_INVALIDE, XSD);
        afficherResultatValidation(XML_INVALIDE, erreursInv);

        if (!erreurs.isEmpty()) {
            System.out.println("Document principal invalide : arrêt avant l'interrogation XPath.");
            return;
        }

        System.out.println();
        System.out.println("=== 2. Interrogation XPath ===");
        Document doc = charger(XML_VALIDE);
        XPath xp = XPathFactory.newInstance().newXPath();

        afficherListe(xp, doc, "Q1 - Titres de tous les films",
                "/plateforme/films/film/titre");

        afficherListe(xp, doc, "Q2 - Utilisateurs actifs",
                "/plateforme/utilisateurs/utilisateur[@actif='true']/nom");

        afficherListe(xp, doc, "Q3 - Films en promotion (titre + remise)",
                "/plateforme/films/film[prix/@promotion='true']"
                        + "/concat(titre, ' (-', prix/@remise, '%)')");

        afficherListe(xp, doc, "Q4 - Films réalisés par Kaouther Ben Hania (jointure par id)",
                "/plateforme/films/film[@realisateur = "
                        + "/plateforme/realisateurs/realisateur[nom='Kaouther Ben Hania']/@id]/titre");

        afficherListe(xp, doc, "Q5 - Films de science-fiction notés au moins 8.7",
                "/plateforme/films/film[genres/genre='Science-fiction' and note >= 8.7]/titre");

        afficherListe(xp, doc, "Q6 - Films favoris de Youssef Gharbi",
                "/plateforme/films/film[@id = "
                        + "/plateforme/utilisateurs/utilisateur[nom='Youssef Gharbi']/favoris/favori/@film]/titre");

        afficherListe(xp, doc, "Q7 - Utilisateurs Premium actifs",
                "//utilisateur[@actif='true' and abonnement/@type='Premium']/nom");

        afficherValeur(xp, doc, "Q8 - Nombre total de films", "count(//film)");
        afficherValeur(xp, doc, "Q9 - Note moyenne du catalogue", "sum(//film/note) div count(//film)");
        afficherValeur(xp, doc, "Q10 - Titre du film le plus long",
                "//film[not(duree < //film/duree)]/titre");
    }

    /* ------------------------------------------------------------------ */
    /* 1. Validation XML / XSD                                              */
    /* ------------------------------------------------------------------ */

    /**
     * Valide un document XML contre un schéma XSD et renvoie la liste
     * de toutes les erreurs rencontrées (liste vide = document valide).
     */
    public static List<String> valider(String xml, String xsd) throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(ressource(xsd));
        Validator validator = schema.newValidator();

        final List<String> erreurs = new ArrayList<>();
        validator.setErrorHandler(new ErrorHandler() {
            @Override public void warning(SAXParseException e) { erreurs.add("[Avertissement] " + format(e)); }
            @Override public void error(SAXParseException e) { erreurs.add("[Erreur] " + format(e)); }
            @Override public void fatalError(SAXParseException e) throws SAXException {
                erreurs.add("[Erreur fatale] " + format(e));
                throw e;
            }
        });

        try {
            validator.validate(new StreamSource(ressource(xml).openStream(), ressource(xml).toString()));
        } catch (SAXException e) {
            // erreur fatale déjà enregistrée par l'ErrorHandler
        }
        return erreurs;
    }

    private static String format(SAXParseException e) {
        return "ligne " + e.getLineNumber() + ", col " + e.getColumnNumber() + " : " + e.getMessage();
    }

    private static void afficherResultatValidation(String xml, List<String> erreurs) {
        if (erreurs.isEmpty()) {
            System.out.println("[OK] " + xml + " est valide selon " + XSD);
        } else {
            System.out.println("[KO] " + xml + " est invalide (" + erreurs.size() + " erreur(s)) :");
            for (String err : erreurs) System.out.println("     " + err);
        }
    }

    /* ------------------------------------------------------------------ */
    /* 2. Interrogation XPath                                               */
    /* ------------------------------------------------------------------ */

    public static Document charger(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        try (InputStream in = ressource(xml).openStream()) {
            return builder.parse(in);
        }
    }

    /** Évalue une expression XPath renvoyant un ensemble de nœuds. */
    public static List<String> requeteListe(XPath xp, Document doc, String expression) throws Exception {
        List<String> res = new ArrayList<>();
        // XPath 1.0 (JAXP) ne permet pas concat() en fin de chemin : on le gère
        // en évaluant concat() relativement à chaque nœud sélectionné.
        int idx = expression.indexOf("/concat(");
        if (idx > 0) {
            XPathExpression noeuds = xp.compile(expression.substring(0, idx));
            XPathExpression texte = xp.compile(expression.substring(idx + 1));
            NodeList nl = (NodeList) noeuds.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) res.add(texte.evaluate(nl.item(i)));
            return res;
        }
        XPathExpression expr = xp.compile(expression);
        NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < nl.getLength(); i++) res.add(nl.item(i).getTextContent().trim());
        return res;
    }

    private static void afficherListe(XPath xp, Document doc, String titre, String expression) throws Exception {
        System.out.println();
        System.out.println(titre);
        System.out.println("  XPath : " + expression);
        List<String> res = requeteListe(xp, doc, expression);
        for (String s : res) System.out.println("   - " + s);
        System.out.println("  (" + res.size() + " résultat(s))");
    }

    private static void afficherValeur(XPath xp, Document doc, String titre, String expression) throws Exception {
        XPathExpression expr = xp.compile(expression);
        String valeur = expr.evaluate(doc);
        if (expression.startsWith("sum(") || expression.startsWith("count(")) {
            double d = (Double) expr.evaluate(doc, XPathConstants.NUMBER);
            valeur = (d == Math.floor(d)) ? String.valueOf((long) d) : String.format("%.2f", d);
        }
        System.out.println();
        System.out.println(titre);
        System.out.println("  XPath : " + expression);
        System.out.println("   = " + valeur);
    }

    private static URL ressource(String nom) {
        URL url = Main.class.getClassLoader().getResource(nom);
        if (url == null) throw new IllegalArgumentException("Ressource introuvable : " + nom);
        return url;
    }
}
