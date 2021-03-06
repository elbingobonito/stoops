package oopsc.expressions;

import oopsc.CompileException;
import oopsc.declarations.ClassDeclaration;
import oopsc.declarations.Declarations;
import oopsc.parser.Position;
import oopsc.streams.CodeStream;
import oopsc.streams.TreeStream;

/**
 * Die abstrakte Basisklasse für alle Ausdrücke im Syntaxbaum.
 * Zusätzlich zur Standardschnittstelle für Ausdrücke definiert sie auch 
 * Methoden zur Erzeugung neuer Ausdrücke für das Boxing und Unboxing von
 * Ausdrücken sowie das Dereferenzieren.
 */
public abstract class Expression {
    /** Der Typ dieses Ausdrucks. Solange er nicht bekannt ist, ist dieser Eintrag null. */
    private ClassDeclaration type;

    /** Die Quelltextposition, an der dieser Ausdruck beginnt. */
    private Position position;
    
    /**
     * Konstruktor.
     * @param position Die Quelltextposition, an der dieser Ausdruck beginnt.
     */
    Expression(Position position) {
        this.position = position;
    }
    
    /**
     * Setzt den Typ dieses Ausdrucks.  
     * @param type Der Type des Ausdrucks.
     */
    void setType(ClassDeclaration type) {
        this.type = type;
    }
    
    /**
     * Der Typ dieses Ausdrucks.
     * @return Der Typ des Ausdrucks oder null, wenn er noch nicht gesetzt wurde.
     */
    public ClassDeclaration getType() {
        return type;
    }
    
    /**
     * Ist dieser Ausdruck ein L-Wert, d.h. eine Referenz auf eine Variable?
     * @return Standardmäßig nein, denn die meisten Ausdrücke sind keine L-Werte.
     */
    public boolean isLValue() {
        return false;
    }
    
    /**
     * Liefert die Quelltextstelle, an der dieser Ausdruck begonnen hat.
     * @return Die Position.
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Die Methode führt die Kontextanalyse für diesen Ausdruck durch.
     * Sie ist nicht abstrakt, da es einige abgeleitete Klassen gibt,
     * die sie nicht implementieren, weil sie dort nicht benötigt wird.
     * Da im Rahmen der Kontextanalyse auch neue Ausdrücke erzeugt werden 
     * können, sollte diese Methode immer in der Form "a = a.contextAnalysis(...)"
     * aufgerufen werden, damit ein neuer Ausdruck auch im Baum gespeichert wird.
     * @param declarations Die an dieser Stelle gültigen Deklarationen.
     * @return Dieser Ausdruck oder ein neuer Ausdruck, falls ein Boxing,
     *         Unboxing oder eine Dereferenzierung in den Baum eingefügt
     *         wurden.
     * @throws CompileException Während der Kontextanylyse wurde ein Fehler
     *         gefunden.
     */
    public Expression contextAnalysis(Declarations declarations) throws CompileException {
        return this;
    }

    /**
     * Die Methode gibt diesen Ausdruck in einer Baumstruktur aus.
     * Wenn der Typ des Ausdrucks bereits ermittelt wurde, wird er auch ausgegeben.
     * @param tree Der Strom, in den die Ausgabe erfolgt.
     */
    public abstract void print(TreeStream tree);
    
    /**
     * Die Methode generiert den Assembler-Code für diesen Ausdruck. Sie geht 
     * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
     * @param code Der Strom, in den die Ausgabe erfolgt.
     */
    public abstract void generateCode(CodeStream code);
    
    /**
     * Die Methode prüft, ob dieser Ausdruck "geboxt" oder dereferenziert werden muss.
     * Ist dies der Fall, wird ein entsprechender Ausdruck erzeugt, von dem dieser
     * dann der Operand ist. Dieser neue Ausdruck wird zurückgegeben. Daher sollte diese
     * Methode immer in der Form "a = a.box(...)" aufgerufen werden.
     * "Boxing" ist das Verpacken eines Basisdatentyps in ein Objekt. Dereferenzieren ist
     * das Auslesen eines Werts, dessen Adresse angegeben wurde.
     * @param declarations Die an dieser Stelle gültigen Deklarationen.
     * @return Dieser Ausdruck oder ein neuer Ausdruck, falls ein Boxing oder eine 
     *         Dereferenzierung eingefügt wurde.
     * @throws CompileException Während der Kontextanylyse wurde ein Fehler
     *         gefunden.
     */
    public Expression box(Declarations declarations) throws CompileException {
        if (type.isA(ClassDeclaration.INT_TYPE) || type.isA(ClassDeclaration.BOOL_TYPE)) {
            return new BoxExpression(this, declarations);
        } else if (isLValue()) {
            return new DeRefExpression(this);
        } else {
            return this;
        }
    }

    /**
     * Die Methode prüft, ob dieser Ausdruck dereferenziert, "entboxt" oder beides 
     * werden muss.
     * Ist dies der Fall, wird ein entsprechender Ausdruck erzeugt, von dem dieser
     * dann der Operand ist. Dieser neue Ausdruck wird zurückgegeben. Daher sollte diese
     * Methode immer in der Form "a = a.unBox(...)" aufgerufen werden.
     * "Unboxing" ist das Auspacken eines Objekts zu einem Basisdatentyp. Dereferenzieren ist
     * das Auslesen eines Werts, dessen Adresse angegeben wurde.
     * @return Dieser Ausdruck oder ein neuer Ausdruck, falls ein Unboxing und/oder eine 
     *         Dereferenzierung eingefügt wurde(n).
     */
    public Expression unBox() {
        if (isLValue()) {
            return new DeRefExpression(this).unBox();
        } else if (type != ClassDeclaration.NULL_TYPE && (type.isA(ClassDeclaration.INT_CLASS) || type.isA(ClassDeclaration.BOOL_CLASS) ) ) {
            return new UnBoxExpression(this);
        } else {
            return this;
        }
    }

    /**
     * Durchläuft den Syntaxbaum und wertet konstante Ausdrücke aus 
     * und wendet ein paar Transformationen an.
     * @return Der optimierte Ausdruck.
     */
	public Expression optimize() {
		return this;
	}
}
