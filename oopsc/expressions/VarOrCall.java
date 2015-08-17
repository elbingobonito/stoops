package oopsc.expressions;

import oopsc.CompileException;
import oopsc.declarations.ClassDeclaration;
import oopsc.declarations.Declarations;
import oopsc.declarations.MethodDeclaration;
import oopsc.declarations.VarDeclaration;
import oopsc.parser.ResolvableIdentifier;
import oopsc.streams.CodeStream;
import oopsc.streams.TreeStream;

/**
 * Die Klasse repräsentiert einen Ausdruck im Syntaxbaum, der dem Zugriff auf eine 
 * Variable oder ein Attribut bzw. einem Methodenaufruf entspricht.
 */
public class VarOrCall extends Expression {
    /** Der Name des Attributs, der Variablen oder der Methode. */
    private final ResolvableIdentifier identifier;
    
    /**
     * Konstruktor.
     * @param identifier Der Name des Attributs, der Variablen oder der Methode.
     */
    public VarOrCall(ResolvableIdentifier identifier) {
        super(identifier.getPosition());
        this.identifier = identifier;
    }

    /**
     * Ist dieser Ausdruck ein L-Wert, d.h. eine Referenz auf eine Variable?
     * @return Wenn der Bezeichner eine Variable ist, dann ja.
     */
    public boolean isLValue() {
        return identifier.getDeclaration() instanceof VarDeclaration;
    }
    
   /**
     * Die Methode führt die Kontextanalyse für diesen Ausdruck durch.
     * Dabei wird ein Zugriff über SELF in den Syntaxbaum eingefügt,
     * wenn dieser Ausdruck ein Attribut oder eine Methode bezeichnet.
     * Diese Methode wird niemals für Ausdrücke aufgerufen, die rechts
     * vom Objekt-Zugriffsoperator stehen.
     * @param declarations Die an dieser Stelle gültigen Deklarationen.
     * @return Dieser Ausdruck oder ein neuer Ausdruck, falls ein Boxing 
     *         oder der Zugriff über SELF eingefügt wurde.
     * @throws CompileException Während der Kontextanylyse wurde ein Fehler
     *         gefunden.
     */
    public Expression contextAnalysis(Declarations declarations) throws CompileException {
        return contextAnalysis(declarations, true);
    }

    /**
     * Die Methode führt die Kontextanalyse für diesen Ausdruck durch.
     * Dabei wird optional ein Zugriff über SELF in den Syntaxbaum eingefügt,
     * wenn dieser Ausdruck ein Attribut oder eine Methode bezeichnet.
     * Diese Methode wird direkt für Ausdrücke aufgerufen, die rechts
     * vom Objekt-Zugriffsoperator stehen, wobei dann das Einfügen von SELF
     * untersagt wird.
     * @param declarations Die an dieser Stelle gültigen Deklarationen.
     * @param addSelf Füge SELF ein, wenn dieser Ausdruck eine Methode oder ein
     *         Attribut bezeichnet.
     * @return Dieser Ausdruck oder ein neuer Ausdruck, falls ein Boxing 
     *         oder der Zugriff über SELF eingefügt wurde.
     * @throws CompileException Während der Kontextanylyse wurde ein Fehler
     *         gefunden.
     */
    Expression contextAnalysis(Declarations declarations, boolean addSelf) throws CompileException {
        declarations.resolveVarOrMethod(identifier);
        if (addSelf && (identifier.getDeclaration() instanceof MethodDeclaration || 
                identifier.getDeclaration() instanceof VarDeclaration && ((VarDeclaration) identifier.getDeclaration()).isAttribute())) {
            return new AccessExpression(new VarOrCall(new ResolvableIdentifier("_self", getPosition())), this)
                    .contextAnalysis(declarations);
        } else if (identifier.getDeclaration() instanceof VarDeclaration) {
            setType((ClassDeclaration) ((VarDeclaration) identifier.getDeclaration()).getType().getDeclaration());
        } else if (identifier.getDeclaration() instanceof MethodDeclaration) {
            setType(ClassDeclaration.VOID_TYPE);
        } else {
            assert false;
        }
        return this;
    }
    
    /**
     * Die Methode gibt diesen Ausdruck in einer Baumstruktur aus.
     * Wenn der Typ des Ausdrucks bereits ermittelt wurde, wird er auch ausgegeben.
     * @param tree Der Strom, in den die Ausgabe erfolgt.
     */
    public void print(TreeStream tree) {
        tree.println(identifier.getName() + (getType() == null ? "" : " : " + 
                (isLValue() ? "REF " : "") + getType().getIdentifier().getName()));
    }

    /**
     * Die Methode generiert den Assembler-Code für diesen Ausdruck. Sie geht 
     * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
     * @param code Der Strom, in den die Ausgabe erfolgt.
     */
    public void generateCode(CodeStream code) {
        code.println(getPosition());
        if (identifier.getDeclaration() instanceof VarDeclaration) {
            VarDeclaration v = (VarDeclaration) identifier.getDeclaration();
            if (v.isAttribute()) {
                code.println("; Referenz auf Attribut " + identifier.getName());
                code.println("MRM R5, (R2)");
                code.println("MRI R6, " + v.getOffset());
                code.println("ADD R5, R6");
                code.println("MMR (R2), R5");
            } else {
                code.println("; Referenz auf Variable " + identifier.getName());
                code.println("MRI R5, " + v.getOffset());
                code.println("ADD R5, R3");
                code.println("ADD R2, R1");
                code.println("MMR (R2), R5");
            }
        } else if (identifier.getDeclaration() instanceof MethodDeclaration) {
            MethodDeclaration m = (MethodDeclaration) identifier.getDeclaration();
            String returnLabel = code.nextLabel();
            code.println("MRI R5, " + returnLabel);
            code.println("ADD R2, R1");
            code.println("MMR (R2), R5 ; Rücksprungadresse auf den Stapel");
            code.println("; Statischer Aufruf von " + identifier.getName());
            code.println("MRI R0, " + m.getSelfType().getIdentifier().getName() + "_" + m.getIdentifier().getName());
            code.println(returnLabel + ":");
        } else {
            assert false;
        }
    }
}