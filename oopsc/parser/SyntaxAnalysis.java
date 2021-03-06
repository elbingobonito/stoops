package oopsc.parser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

import oopsc.CompileException;
import oopsc.Program;
import oopsc.declarations.ClassDeclaration;
import oopsc.declarations.MethodDeclaration;
import oopsc.declarations.VarDeclaration;
import oopsc.expressions.AccessExpression;
import oopsc.expressions.BinaryExpression;
import oopsc.expressions.Expression;
import oopsc.expressions.LiteralExpression;
import oopsc.expressions.NewExpression;
import oopsc.expressions.UnaryExpression;
import oopsc.expressions.VarOrCall;
import oopsc.statements.Assignment;
import oopsc.statements.CallStatement;
import oopsc.statements.IfStatement;
import oopsc.statements.ReadStatement;
import oopsc.statements.ReturnStatement;
import oopsc.statements.Statement;
import oopsc.statements.WhileStatement;
import oopsc.statements.WriteStatement;

/**
 * Die Klasse realisiert die syntaktische Analyse für die folgende Grammatik. 
 * Terminale stehen dabei in Hochkommata oder sind groß geschrieben:
 * <pre>
 * program      ::= { classdecl }
 *
 * classdecl    ::= CLASS identifier [ EXTENDS identifier ] IS
 *                  { memberdecl } 
 *                  END CLASS
 *
 * memberdecl   ::= [PRIVATE|PROTECTED|PUBLIC] ( vardecl ';'
 *                | METHOD identifier [ '(' vardecl { ';' vardecl } ')' ]
 *                 [':' Identifier ] IS methodbody )
 * 
 * vardecl      ::= identifier { ',' identifier } ':' identifier
 * 
 * methodbody   ::= { vardecl ';' }
 *                  BEGIN statements
 *                  END METHOD
 * 
 * statements   ::= { statement }
 * 
 * statement    ::= READ memberaccess ';'
 *                | WRITE expression ';'
 *                | IF predicateSC 
 *                  THEN statements 
 *                  {ELSEIF predicateSC THEN statements }
 *                  [ELSE statements]
 *                  END IF
 *                | WHILE predicateSC 
 *                  DO statements 
 *                  END WHILE
 *                | memberaccess [ ':=' predicateSC ] ';'
 *                | RETURN [ predicateSC ] ';'
 * 
 * predicateSC ::= conjuctionSC { OR ELSE conjunctionSC }
 * 
 * conjunctionSC ::= predicate { AND THEN predicate }
 * 
 * predicate ::= conjunction { OR conjunction }
 * 
 * conjunction ::= relation { AND relation }
 * 
 * relation     ::= expression [ ( '=' | '#' | '<' | '>' | '<=' | '>=' ) expression ]
 * 
 * expression   ::= term { ( '+' | '-' ) term }
 * 
 * term         ::= factor { ( '*' | '/' | MOD ) factor }
 * 
 * factor       ::= '-' factor
 * 				  | NOT factor
 *                | memberaccess
 * 
 * memberaccess ::= literal { '.' varorcall }
 * 
 * literal    ::= number
 *                | character
 *                | NULL
 *                | SELF
 *                | NEW identifier
 *                | '(' predicateSC ')'
 *                | varorcall
 *                | TRUE 
 *                | FALSE
 * 
 * varorcall    ::= identifier [ '(' predicateSC { ',' predicateSC } ')' ]
 * </pre>
 * Daraus wird der Syntaxbaum aufgebaut, dessen Wurzel die Klasse
 * {@link Program Program} ist.
 */
public class SyntaxAnalysis {
    /** Die lexikalische Analyse, die den Symbolstrom erzeugt. */
    private final LexicalAnalysis lexer;
    
    /**
     * Die Methode erzeugt einen "Unerwartetes Symbol"-Fehler.
     * @throws CompileException Die entsprechende Fehlermeldung.
     */
    private void unexpectedSymbol() throws CompileException {
        throw new CompileException("Unerwartetes Symbol " + lexer.getSymbol().getId().toString(), 
                lexer.getSymbol().getPosition());
    }
    
    /**
     * Die Methode überprüft, ob das aktuelle Symbol das erwartete ist. Ist dem so,
     * wird das nächste Symbol gelesen, ansonsten wird eine Fehlermeldung erzeugt.
     * @param id Das erwartete Symbol.
     * @throws CompileException Ein unerwartetes Symbol wurde gelesen.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private void expectSymbol(Symbol.Id id) throws CompileException, IOException {
        if (id != lexer.getSymbol().getId()) {
            unexpectedSymbol();
        }
        lexer.nextSymbol();
    }
    
    /**
     * Die Methode überprüft, ob das aktuelle Symbol ein Bezeichner ist. Ist dem so,
     * wird er zurückgeliefert, ansonsten wird eine Fehlermeldung erzeugt.
     * @throws CompileException Ein unerwartetes Symbol wurde gelesen.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private Identifier expectIdent() throws CompileException, IOException {
        if (lexer.getSymbol().getId() != Symbol.Id.IDENT) {
            unexpectedSymbol();
        }
        Identifier i = new Identifier(lexer.getSymbol().getIdent(), lexer.getSymbol().getPosition());
        lexer.nextSymbol();
        return i;
    }
    
    /**
     * Die Methode überprüft, ob das aktuelle Symbol ein Bezeichner ist. Ist dem so,
     * wird er in Form eines Bezeichners mit noch aufzulösender Vereinbarung
     * zurückgeliefert, ansonsten wird eine Fehlermeldung erzeugt.
     * @throws CompileException Ein unerwartetes Symbol wurde gelesen.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private ResolvableIdentifier expectResolvableIdent() throws CompileException, IOException {
        if (lexer.getSymbol().getId() != Symbol.Id.IDENT) {
            unexpectedSymbol();
        }
        ResolvableIdentifier r = new ResolvableIdentifier(lexer.getSymbol().getIdent(), lexer.getSymbol().getPosition());
        lexer.nextSymbol();
        return r;
    }
    
    /**
     * Die Methode parsiert eine oder mehrere Klassendeklarationen entsprechend der oben angegebenen
     * Syntax und liefert diese zurück.
     * @return Die Klassendeklarationen.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private LinkedList<ClassDeclaration> classdecl() throws CompileException, IOException {
        LinkedList<ClassDeclaration> classdecls = new LinkedList<ClassDeclaration>();
    	while (lexer.getSymbol().getId() == Symbol.Id.CLASS) {
    		lexer.nextSymbol();
	        Identifier name = expectIdent();
	        ResolvableIdentifier baseType = null;
	        if (lexer.getSymbol().getId() == Symbol.Id.EXTENDS) {
	        	lexer.nextSymbol();
	        	baseType = expectResolvableIdent();
	        } else {
	        	baseType = new ResolvableIdentifier("Object", null);
	        }
	        expectSymbol(Symbol.Id.IS);
	        LinkedList<VarDeclaration> attributes = new LinkedList<VarDeclaration>();
	        LinkedList<MethodDeclaration> methods = new LinkedList<MethodDeclaration>();
	        while (lexer.getSymbol().getId() != Symbol.Id.END) {
	            memberdecl(attributes, methods);
	        }
	        lexer.nextSymbol();
	        expectSymbol(Symbol.Id.CLASS);
	        classdecls.add(new ClassDeclaration(name, baseType, attributes, methods));
    	}
        return classdecls;
    }
    
    /**
     * Die Methode parsiert die Deklaration eines Attributs bzw. einer Methode
     * entsprechend der oben angegebenen Syntax und hängt sie an eine von
     * zwei Listen an.
     * @param attributes Die Liste der Attributdeklarationen der aktuellen Klasse.
     * @param methods Die Liste der Methodendeklarationen der aktuellen Klasse.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private void memberdecl(LinkedList<VarDeclaration> attributes, 
            LinkedList<MethodDeclaration> methods)
            throws CompileException, IOException {
    	Symbol.Id accessRight = Symbol.Id.PUBLIC;
    	if(lexer.getSymbol().getId() == Symbol.Id.PRIVATE || lexer.getSymbol().getId() == Symbol.Id.PROTECTED || lexer.getSymbol().getId() == Symbol.Id.PUBLIC) {
    		accessRight = lexer.getSymbol().getId();
    		lexer.nextSymbol();
    	}
        if (lexer.getSymbol().getId() == Symbol.Id.METHOD) {
            lexer.nextSymbol();
            LinkedList<VarDeclaration> params = new LinkedList<VarDeclaration>();
            ResolvableIdentifier returnIdent = null;
            Identifier name = expectIdent();
            
            /** Parse eventuell vorhandene Parameter */
            if (lexer.getSymbol().getId() ==  Symbol.Id.LPAREN) {
            	lexer.nextSymbol();
            	vardecl(params, false, Symbol.Id.PUBLIC);
            	while (lexer.getSymbol().getId() == Symbol.Id.SEMICOLON) {
            		lexer.nextSymbol();
            		vardecl(params, false, Symbol.Id.PUBLIC);
            	}
            	expectSymbol(Symbol.Id.RPAREN);
            }
            
            /** Parse eventuell vorhandenen Rückgabetyp */
            if (lexer.getSymbol().getId() == Symbol.Id.COLON) {
            	lexer.nextSymbol();
            	returnIdent = expectResolvableIdent();
            }
            		
            expectSymbol(Symbol.Id.IS);
            LinkedList<VarDeclaration> vars = new LinkedList<VarDeclaration>();
            LinkedList<Statement> statements = new LinkedList<Statement>();
            Position end = methodbody(vars, statements);
            methods.add(new MethodDeclaration(name, params, returnIdent, vars, statements, end, accessRight));
        } else {
            vardecl(attributes, true, accessRight);
            expectSymbol(Symbol.Id.SEMICOLON);
        }
    }

    /**
     * Die Methode parsiert die Deklaration eines Attributs bzw. einer Variablen
     * entsprechend der oben angegebenen Syntax und hängt sie an eine Liste an.
     * @param vars Die Liste der Attributdeklarationen der aktuellen Klasse oder 
     *         der Variablen der aktuellen Methode.
     * @param isAttribute Ist die Variable ein Attribut?.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private void vardecl(LinkedList<VarDeclaration> vars, boolean isAttribute, Symbol.Id accessRight) throws CompileException, IOException {
        LinkedList<Identifier> names = new LinkedList<Identifier>();
        names.add(expectIdent());
        while (lexer.getSymbol().getId() == Symbol.Id.COMMA) {
            lexer.nextSymbol();
            names.add(expectIdent());
        }
        expectSymbol(Symbol.Id.COLON);
        ResolvableIdentifier ident = expectResolvableIdent();
        for (Identifier name : names) {
            vars.add(new VarDeclaration(name, ident, isAttribute, accessRight));
        }
    }
    
    /**
     * Die Methode parsiert die Deklaration eines Methodenrumpfes entsprechend der 
     * oben angegebenen Syntax. Lokale Variablendeklarationen und Anweisungen werden
     * an die entsprechenden Listen angehängt.
     * @param vars Die Liste der lokalen Variablendeklarationen der aktuellen Methode.
     * @param statements Die Liste der Anweisungen der aktuellen Methode.
     * @return Die Position von END METHOD im Quelltext.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private Position methodbody(LinkedList<VarDeclaration> vars, LinkedList<Statement> statements) 
            throws CompileException, IOException {
        while (lexer.getSymbol().getId() != Symbol.Id.BEGIN) {
            vardecl(vars, false, Symbol.Id.PUBLIC);
            expectSymbol(Symbol.Id.SEMICOLON);
        }
        lexer.nextSymbol();
        statements(statements);
        Position position = lexer.getSymbol().getPosition();
        expectSymbol(Symbol.Id.END);
        expectSymbol(Symbol.Id.METHOD);
        return position;
    }
    
    /**
     * Die Methode parsiert eine Folge von Anweisungen entsprechend der 
     * oben angegebenen Syntax und hängt sie an eine Liste an.
     * @param statements Die Liste der Anweisungen.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private void statements(LinkedList<Statement> statements) throws CompileException, IOException {
        while (lexer.getSymbol().getId() != Symbol.Id.END && lexer.getSymbol().getId() != Symbol.Id.ELSE && lexer.getSymbol().getId() != Symbol.Id.ELSEIF ) {
            statement(statements);
        }
    }
    
    /**
     * Die Methode parsiert eine Anweisung entsprechend der oben angegebenen
     * Syntax und hängt sie an eine Liste an.
     * @param statements Die Liste der Anweisungen.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private void statement(LinkedList<Statement> statements) throws CompileException, IOException {
        switch (lexer.getSymbol().getId()) {
        case READ:
            lexer.nextSymbol();
            statements.add(new ReadStatement(memberAccess()));
            expectSymbol(Symbol.Id.SEMICOLON);
            break;
        case WRITE:
            lexer.nextSymbol();
            statements.add(new WriteStatement(expression()));
            expectSymbol(Symbol.Id.SEMICOLON);
            break;
        case ELSEIF:
        case IF: 
    		boolean isIf = lexer.getSymbol().getId() == Symbol.Id.IF;
        	lexer.nextSymbol();
            Expression ifCondition = predicateSC();
            expectSymbol(Symbol.Id.THEN);
            LinkedList<Statement> thenStatements = new LinkedList<Statement>();
            LinkedList<Statement> elseStatements = new LinkedList<Statement>();
            statements(thenStatements);	
            if(lexer.getSymbol().getId() == Symbol.Id.ELSEIF) {
            	statement(elseStatements);
            } else if(lexer.getSymbol().getId() == Symbol.Id.ELSE) {
            	lexer.nextSymbol();
            	statements(elseStatements);
            } 
            if(isIf) {
            	expectSymbol(Symbol.Id.END);
            	expectSymbol(Symbol.Id.IF);
            }
            statements.add(new IfStatement(ifCondition, thenStatements, elseStatements));
            break;	
        case WHILE:
            lexer.nextSymbol();
            Expression whileCondition = predicateSC();
            expectSymbol(Symbol.Id.DO);
            LinkedList<Statement> whileStatements = new LinkedList<Statement>();
            statements(whileStatements);
            expectSymbol(Symbol.Id.END);
            expectSymbol(Symbol.Id.WHILE);
            statements.add(new WhileStatement(whileCondition, whileStatements));
            break;
        case RETURN:
        	lexer.nextSymbol();
        	ReturnStatement stmt;
        	if (lexer.getSymbol().getId() == Symbol.Id.SEMICOLON) {
        		stmt = new ReturnStatement(null,lexer.getSymbol().getPosition());
        		statements.add(stmt);
        		lexer.nextSymbol();
        	} else {
        		stmt = new ReturnStatement(predicateSC(), lexer.getSymbol().getPosition());
        		statements.add(stmt);
        		expectSymbol(Symbol.Id.SEMICOLON);
        	}
        	break;
        default:
            Expression e = memberAccess();
            if (lexer.getSymbol().getId() == Symbol.Id.BECOMES) {
                lexer.nextSymbol();
                statements.add(new Assignment(e, predicateSC()));
            } else {
                statements.add(new CallStatement(e));
            }
            expectSymbol(Symbol.Id.SEMICOLON);
        }
    }
    
    /**
     * Die Methode parsiert eine Relation entsprechend der oben angegebenen
     * Syntax und liefert den Ausdruck zurück.
     * @return Der Ausdruck.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private Expression relation() throws CompileException, IOException {
        Expression e = expression();
        switch (lexer.getSymbol().getId()) {
        case EQ:
        case NEQ:
        case GT:
        case GTEQ:
        case LT:
        case LTEQ:
            Symbol.Id operator = lexer.getSymbol().getId();
            lexer.nextSymbol();
            return new BinaryExpression(e, operator, expression());
        default:
            return e;
        }
    }
    
    /**
     * Die Methode parsiert eine Kurzschlussdisjunktion entsprechend der oben angegebenen 
     * Syntax und liefert den Ausdruck zurück.
     * @return Der Ausdruck.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private Expression predicateSC() throws CompileException, IOException {
    	Expression e = conjunctionSC();
    	while (lexer.getSymbol().getId() == Symbol.Id.OR) {
    		Symbol.Id operator = lexer.getSymbol().getId();
    		lexer.nextSymbol();
    		e = new BinaryExpression(e, operator, conjunctionSC());
    	}
    	return e;
    }
    
    /**
     * Die Methode parsiert eine Kurzschlusskonjunktion entsprechend der oben angegebenen 
     * Syntax und liefert den Ausdruck zurück.
     * @return Der Ausdruck.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private Expression conjunctionSC() throws CompileException, IOException {
    	Expression e = predicate();
    	while (lexer.getSymbol().getId() == Symbol.Id.AND_THEN) {
    		Symbol.Id operator = lexer.getSymbol().getId();
    		lexer.nextSymbol();
    		e = new BinaryExpression(e, operator, predicate());
    	}
    	return e;
    }
    
    /**
     * Die Methode parsiert ein Prädikat entsprechend der oben angegebenen 
     * Syntax und liefert den Ausdruck zurück.
     * @return Der Ausdruck.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private Expression predicate() throws CompileException, IOException {
    	Expression e = conjunction();
    	while (lexer.getSymbol().getId() == Symbol.Id.OR_ELSE) {
    		Symbol.Id operator = lexer.getSymbol().getId();
    		lexer.nextSymbol();
    		e = new BinaryExpression(e, operator, conjunction());
    	}
    	return e;
    }
    
    /**
     * Die Methode parsiert eine Konjunktion entsprechend der oben angegebenen 
     * Syntax und liefert den Ausdruck zurück.
     * @return Der Ausdruck.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private Expression conjunction() throws CompileException, IOException {
    	Expression e = relation();
    	while (lexer.getSymbol().getId() == Symbol.Id.AND) {
    		Symbol.Id operator = lexer.getSymbol().getId();
    		lexer.nextSymbol();
    		e = new BinaryExpression(e, operator, relation());
    	}
    	return e;
    }
    
    

    /**
     * Die Methode parsiert einen Ausdruck entsprechend der oben angegebenen
     * Syntax und liefert ihn zurück.
     * @return Der Ausdruck.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private Expression expression() throws CompileException, IOException {
        Expression e = term();
        while (lexer.getSymbol().getId() == Symbol.Id.PLUS || lexer.getSymbol().getId() == Symbol.Id.MINUS) {
            Symbol.Id operator = lexer.getSymbol().getId();
            lexer.nextSymbol();
            e = new BinaryExpression(e, operator, term());
        }
        return e;
    }

    /**
     * Die Methode parsiert einen Term entsprechend der oben angegebenen
     * Syntax und liefert den Ausdruck zurück.
     * @return Der Ausdruck.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private Expression term() throws CompileException, IOException {
        Expression e = factor();
        while (lexer.getSymbol().getId() == Symbol.Id.TIMES || lexer.getSymbol().getId() == Symbol.Id.DIV ||
                lexer.getSymbol().getId() == Symbol.Id.MOD) {
            Symbol.Id operator = lexer.getSymbol().getId();
            lexer.nextSymbol();
            e = new BinaryExpression(e, operator, factor());
        }
        return e;
    }

    /**
     * Die Methode parsiert einen Faktor entsprechend der oben angegebenen
     * Syntax und liefert den Ausdruck zurück.
     * @return Der Ausdruck.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private Expression factor() throws CompileException, IOException {
        switch (lexer.getSymbol().getId()) {
        case NOT:
        case MINUS:
            Symbol.Id operator = lexer.getSymbol().getId();
            Position position = lexer.getSymbol().getPosition();
            lexer.nextSymbol();
            return new UnaryExpression(operator, factor(), position);
        default:
            return memberAccess();
        }
    }
    
    /**
     * Die Methode parsiert den Zugriff auf ein Objektattribut bzw. eine 
     * Objektmethode entsprechend der oben angegebenen Syntax und liefert 
     * den Ausdruck zurück.
     * @return Der Ausdruck.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private Expression memberAccess() throws CompileException, IOException {
        Expression e = literal();
        while (lexer.getSymbol().getId() == Symbol.Id.PERIOD) {
            lexer.nextSymbol();
            e = new AccessExpression(e, (VarOrCall) varorcall());
        }
        return e;
    }

    /**
     * Die Methode parsiert ein Literal, die Erzeugung eines Objekts, einen
     * geklammerten Ausdruck oder einen einzelnen Zugriff auf eine Variable,
     * ein Attribut oder eine Methode entsprechend der oben angegebenen 
     * Syntax und liefert den Ausdruck zurück.
     * @return Der Ausdruck.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private Expression literal() throws CompileException, IOException {
        Expression e = null;
        switch (lexer.getSymbol().getId()) {
        case NUMBER:
            e = new LiteralExpression(lexer.getSymbol().getNumber(), ClassDeclaration.INT_TYPE, 
                    lexer.getSymbol().getPosition());
            lexer.nextSymbol();
            break;
        case TRUE:
        	e = new LiteralExpression(1, ClassDeclaration.BOOL_TYPE, lexer.getSymbol().getPosition());
        	lexer.nextSymbol();
        	break;
        case FALSE:
        	e = new LiteralExpression(0, ClassDeclaration.BOOL_TYPE, lexer.getSymbol().getPosition());
        	lexer.nextSymbol();
        	break;
        case NULL:
            e = new LiteralExpression(0, ClassDeclaration.NULL_TYPE, lexer.getSymbol().getPosition());
            lexer.nextSymbol();
            break;
        case SELF:
            e = new VarOrCall(new ResolvableIdentifier("_self", lexer.getSymbol().getPosition()), new LinkedList<Expression>());
            lexer.nextSymbol();
            break;
        case BASE:
        	e = new VarOrCall(new ResolvableIdentifier("_base", lexer.getSymbol().getPosition()), new LinkedList<Expression>());
        	lexer.nextSymbol();
        	break;
        case NEW:
            Position position = lexer.getSymbol().getPosition();
            lexer.nextSymbol();
            e = new NewExpression(expectResolvableIdent(), position);
            break;
        case LPAREN:
            lexer.nextSymbol();
            e = predicateSC();
            expectSymbol(Symbol.Id.RPAREN);
            break;
        case IDENT:
            e = varorcall();
            break;
        default:
            unexpectedSymbol();
        }
        return e;
    }
    
    /**
     * Die Methode parsiert eine Variable oder einen Methodenaufruf
     * entsprechend der oben angegebenen 
     * Syntax und liefert den Ausdruck zurück.
     * @return Der Ausdruck.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private Expression varorcall() throws CompileException, IOException {
    	ResolvableIdentifier ident = expectResolvableIdent();
    	LinkedList<Expression> args = new LinkedList<Expression>();
        if (lexer.getSymbol().getId() ==  Symbol.Id.LPAREN) {
        	lexer.nextSymbol();
        	args.add(predicateSC());
        	while (lexer.getSymbol().getId() == Symbol.Id.COMMA) {
        		lexer.nextSymbol();
        		args.add(predicateSC());
        	}
        	expectSymbol(Symbol.Id.RPAREN);
        }
        return new VarOrCall(ident, args);
    }

    /**
     * Konstruktor.
     * @param fileName Der Name des Quelltexts.
     * @param printSymbols Die lexikalische Analyse gibt die erkannten
     *         Symbole auf der Konsole aus.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws FileNotFoundException Der Quelltext wurde nicht gefunden.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    public SyntaxAnalysis(String fileName, boolean printSymbols) 
            throws CompileException, FileNotFoundException, IOException {
        lexer = new LexicalAnalysis(fileName, printSymbols);
    }

    /**
     * Die Methode parsiert den Quelltext und liefert die Wurzel des 
     * Syntaxbaums zurück.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    public Program parse() throws CompileException, IOException {
        lexer.nextSymbol();
        Program p = new Program(classdecl());
        expectSymbol(Symbol.Id.EOF);
        return p;
    }
}
