package oopsc.streams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import oopsc.parser.Position;

/**
 * Die Klasse repräsentiert einen Datenstrom, in der Assemblercode des
 * auszugebenen Programms geschrieben wird. Da die Klasse von 
 * {@link java.io.PrintStream PrintStream} erbt, können alle Methoden
 * verwendet werden, mit denen man auch auf die Konsole schreiben kann.
 * Zusätzlich kann die Klasse eindeutige Marken für den Assemblerquelltext
 * generieren.
 */
public class CodeStream extends PrintStream {
    /** Das Attribut enthält den gerade gültigen Namensraum (Klasse + Methode). */
    private String namespace;
    
    /** Das Attribut ist ein Zähler zur Generierung eindeutiger Bezeichner. */
    private int counter;

    /**
     * Konstruktor zur Ausgabe auf die Konsole.
     */
    public CodeStream() {
        super(System.out);
    }
    
    /**
     * Konstruktor zur Ausgabe in eine Datei.
     * @param fileName Der Name der Ausgabedatei.
     * @throws FileNotFoundException Die Datei kann nicht erzeugt werden.
     */
    public CodeStream(String fileName) throws FileNotFoundException {
        super(new File(fileName));
    }
    
    /**
     * Die Methode setzt den aktuell gültigen Namensraum.
     * Dieser wird verwendet, um eindeutige Marken zu generieren.
     * Derselbe Namensraum darf nur einmal während der Code-Erzeugung 
     * gesetzt werden.
     * @param namespace Den ab jetzt gültigen Namensraum (Klasse + Methode).
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
        counter = 1;
    }

    /**
     * Die Methode erzeugt eine eindeutige Marke im aktuellen Namensraum.
     * @return Die Marke.
     */
    public String nextLabel() {
        return namespace + "_" + counter++;
    }
    
    /**
     * Die Methode gibt die Zeilennummer aus, wenn sie gesetzt wurde.
     * @param position Die Quelltextposition, die die Zeilennummer enthält.
     *         Darf auch null sein.
     */
    public void println(Position position) {
        if (position != null) {
            println("#" + position.getLine());
        }
    }
    
    public String getEndlabel() {
    	return "end_"+namespace;
    }
}
