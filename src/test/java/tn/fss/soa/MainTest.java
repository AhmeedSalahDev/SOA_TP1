package tn.fss.soa;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void documentValideEstAccepte() throws Exception {
        assertTrue(Main.valider("cinestream.xml", "cinestream.xsd").isEmpty());
    }

    @Test
    void documentInvalideEstRejete() throws Exception {
        assertFalse(Main.valider("cinestream-invalide.xml", "cinestream.xsd").isEmpty());
    }

    @Test
    void utilisateursActifs() throws Exception {
        Document doc = Main.charger("cinestream.xml");
        XPath xp = XPathFactory.newInstance().newXPath();
        List<String> actifs = Main.requeteListe(xp, doc, "//utilisateur[@actif='true']/nom");
        assertEquals(Arrays.asList("Ahmed Ben Salah", "Youssef Gharbi", "Mariem Karray"), actifs);
    }

    @Test
    void filmsEnPromotion() throws Exception {
        Document doc = Main.charger("cinestream.xml");
        XPath xp = XPathFactory.newInstance().newXPath();
        List<String> promo = Main.requeteListe(xp, doc, "//film[prix/@promotion='true']/titre");
        assertEquals(Arrays.asList("Interstellar", "Parasite", "La Belle et la Meute"), promo);
    }
}
