package oopsc.parser;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.PushbackReader;
import java.util.Arrays;
import java.util.HashMap;

import oopsc.CompileException;

/**
 * Die Klasse führt die lexikalische Analyse durch. Es werden alle
 * Terminale der bei {@link SyntaxAnalysis SyntaxAnalysis} beschriebenen
 * Grammaktik erkannt. Bezeichner und Zahlen folgen dieser Syntax:
 * <pre>
 * identifier   ::= letter { letter | digit }
 * 
 * number       ::= digit { digit }
 * 
 * letter       ::= 'A' .. 'Z' | 'a' .. 'z'
 * 
 * digit        ::= '0' .. '9'
 *
 * character    ::= ''' ( Sichtbares-UTF-8-Zeichen-kein-\ 
 *                      | '\' ( 'n' | 't' | '\' ) ) ''' 
 * </pre>
 * Kommentare zwischen geschweiften Klammern ('{' ... '}') bzw. hinter
 * senkrechten Strichen ('|') werden ignoriert.
 */
class LexicalAnalysis {
    /** Die Menge aller Schlüsselworte mit ihren zugeordneten Symbolen. */
    private HashMap<String, Symbol.Id> keywords;

    /** Der Datenstrom, aus dem der Quelltext gelesen wird. */
    private PushbackReader reader;

    /** Sollen die erkannten Symbole auf der Konsole ausgegeben werden? */
    private boolean printSymbols;
    
    /** Die aktuelle Position im Quelltext. */
    private Position position;
    
    private Position offset; 
    
    /** Das zuletzt gelesene Zeichen. */
    private int c;
    
    /** Das zuletzt erkannte Symbol. */
    private Symbol symbol;
    
    /** Das zuvor erkannte Symbol. */
    private Symbol previousSymbol;

    /** 
     * Die Methode liest das nächste Zeichen aus dem Quelltext.
     * Dieses wird im Attribut {@link #c c} bereitgestellt.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private void nextChar() throws IOException {
        position.next((char) c);
        c = reader.read();
    }
    
    private void nextTmpChar() throws IOException {
        offset.next((char) c);
        c = reader.read();
    }
    
    /**
     * Konstruktor.
     * @param fileName Der Name des Quelltexts.
     * @param printSymbols Sollen die erkannten Symbole auf der Konsole 
     *         ausgegeben werden?
     * @throws FileNotFoundException Der Quelltext wurde nicht gefunden.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    LexicalAnalysis(String fileName, boolean printSymbols) 
            throws FileNotFoundException, IOException {
        reader = new PushbackReader(new InputStreamReader(new FileInputStream(fileName),"UTF-8"), 5);
        this.printSymbols = printSymbols;

        keywords = new HashMap<String, Symbol.Id>();
        keywords.put("BEGIN", Symbol.Id.BEGIN);
        keywords.put("END", Symbol.Id.END);
        keywords.put("CLASS", Symbol.Id.CLASS);
        keywords.put("IS", Symbol.Id.IS);
        keywords.put("METHOD", Symbol.Id.METHOD);
        keywords.put("READ", Symbol.Id.READ);
        keywords.put("WRITE", Symbol.Id.WRITE);
        keywords.put("IF", Symbol.Id.IF);
        keywords.put("THEN", Symbol.Id.THEN);
        keywords.put("ELSE", Symbol.Id.ELSE);
        keywords.put("ELSEIF", Symbol.Id.ELSEIF);
        keywords.put("WHILE", Symbol.Id.WHILE);
        keywords.put("DO", Symbol.Id.DO);
        keywords.put("MOD", Symbol.Id.MOD);
        keywords.put("NEW", Symbol.Id.NEW);
        keywords.put("SELF", Symbol.Id.SELF);
        keywords.put("NULL", Symbol.Id.NULL);
        keywords.put("TRUE", Symbol.Id.TRUE);
        keywords.put("FALSE", Symbol.Id.FALSE);
        keywords.put("AND", Symbol.Id.AND);
        keywords.put("OR", Symbol.Id.OR);
        keywords.put("NOT", Symbol.Id.NOT);
        keywords.put("RETURN", Symbol.Id.RETURN);
        keywords.put("EXTENDS", Symbol.Id.EXTENDS);
        keywords.put("BASE", Symbol.Id.BASE);
        keywords.put("PRIVATE", Symbol.Id.PRIVATE);
        keywords.put("PROTECTED", Symbol.Id.PROTECTED);
        keywords.put("PUBLIC", Symbol.Id.PUBLIC);
        keywords.put("AND THEN", Symbol.Id.AND_THEN);
        keywords.put("OR ELSE", Symbol.Id.OR_ELSE);
        
        position = new Position(1, 0);
        offset = new Position(0, 0);
        nextChar();
    }
    
    /**
     * Die Methode liest das nächste Symbol. Dieses wird im Attribut
     * {@link #symbol symbol} bereitgestellt.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    void nextSymbol() throws CompileException, IOException {
    	previousSymbol = symbol;
        for(;;) {
            // Leerraum ignorieren
            while (c != -1 && Character.isWhitespace((char) c)) {
                nextChar();
            }
            if (c == '{') { // Geklammerter Kommentar
                nextChar();
                while (c != -1 && c != '}') {
                    nextChar();
                }
                if (c == -1) {
                    throw new CompileException("Unerwartetes Dateiende im Kommentar", position);
                }
                nextChar();
            } else if (c == '|') { // Kommentar bis Zeilenende
                nextChar();
                while (c != -1 && c != '\n') {
                    nextChar();
                }
                nextChar();
            } else {
                break;
            }
        }
        
        Position pos;
        switch (c) {
        case -1:
            symbol = new Symbol(Symbol.Id.EOF, position);
            break;
        case ':':
            symbol = new Symbol(Symbol.Id.COLON, position);
            nextChar();
            if (c == '=') {
                symbol = new Symbol(Symbol.Id.BECOMES, symbol.getPosition());
                nextChar();
            }
            break;
        case ';':
            symbol = new Symbol(Symbol.Id.SEMICOLON, position);
            nextChar();
            break;
        case ',':
            symbol = new Symbol(Symbol.Id.COMMA, position);
            nextChar();
            break;
        case '.':
            symbol = new Symbol(Symbol.Id.PERIOD, position);
            nextChar();
            break;
        case '(':
            symbol = new Symbol(Symbol.Id.LPAREN, position);
            nextChar();
            break;
        case ')':
            symbol = new Symbol(Symbol.Id.RPAREN, position);
            nextChar();
            break;
        case '=':
            symbol = new Symbol(Symbol.Id.EQ, position);
            nextChar();
            break;
        case '#':
            symbol = new Symbol(Symbol.Id.NEQ, position);
            nextChar();
            break;
        case '>':
            symbol = new Symbol(Symbol.Id.GT, position);
            nextChar();
            if (c == '=') {
                symbol = new Symbol(Symbol.Id.GTEQ, symbol.getPosition());
                nextChar();
            }
            break;
        case '<':
            symbol = new Symbol(Symbol.Id.LT, position);
            nextChar();
            if (c == '=') {
                symbol = new Symbol(Symbol.Id.LTEQ, symbol.getPosition());
                nextChar();
            }
            break;
        case '+':
            symbol = new Symbol(Symbol.Id.PLUS, position);
            nextChar();
            break;
        case '-':
            symbol = new Symbol(Symbol.Id.MINUS, position);
            nextChar();
            break;
        case '*':
            symbol = new Symbol(Symbol.Id.TIMES, position);
            nextChar();
            break;
        case '/':
            symbol = new Symbol(Symbol.Id.DIV, position);
            nextChar();
            break;
        case '\'':
            pos = new Position(position.getLine(), position.getColumn());
            nextChar();
            int ch;
            if (c == '\\') {
                nextChar();
                switch (c) {
                case 'n':
                    ch = '\n';
                    break;
                case 't':
                    ch = '\t';
                    break;
                case '\\':
                    ch = '\\';
                    break;
                default:
                    throw new CompileException("Zeichenliteral nicht erlaubt: "
                            + "'\\" + (char) c + " (Code " + c + ")", position);
                }
            } else if (c < ' ') {
                throw new CompileException("Unbekanntes Zeichen im Zeichenliteral (Code " + c + ").", position); 
            } else {
                ch = c;
            }
            nextChar();
            if (c != '\'') {
                throw new CompileException("Zeichenliteral nicht abgeschlossen.", position);
            }
            symbol = new Symbol(ch, pos);
            nextChar();
            break;
        default:
            pos = new Position(position.getLine() + offset.getLine(), position.getColumn() + offset.getColumn());
            offset = new Position(0, 0);
            if (Character.isDigit((char) c)) {
                int number = c - '0';
                nextChar();
                while (c != -1 && Character.isDigit((char) c)) {
                    number = number * 10 + c - '0';
                    nextChar();
                }
                symbol = new Symbol(number, pos);
            } else if (Character.isLetter((char) c)) {
                String ident = "" + (char) c;
                nextChar();
                while (c != -1 && Character.isLetterOrDigit((char) c)) {
                    ident = ident + (char) c;
                    nextChar();
                }
                
                if ("AND".equals(ident) || "OR".equals(ident)) {
                	char[] tmp = new char[5];
                	while (c != -1 && Character.isWhitespace(c)) {
                		nextTmpChar();
                	}
                	if ("AND".equals(ident)) {
	                	for (int i=0; c != -1 && i < 5; ++i) {
	                		tmp[i] = (char)c;
	                		if (i != 4) { 
	                			nextTmpChar();
	                		}
	                	}
                		if ("THEN".equals(new String(Arrays.copyOfRange(tmp, 0, 4))) &&  Character.isWhitespace(tmp[4])) {
                			ident = "AND THEN";
                		} else {
                			reader.unread(tmp);
                			nextChar();
                			offset = new Position(0,0);
                		}
                	} else {
                		for (int i = 0; c != -1 && i < 5; ++i) {
	                		tmp[i] = (char)c;   
	                		if (i != 4) { 
	                			nextTmpChar();
	                		}
	                	}
                		if ("ELSE".equals(new String(Arrays.copyOfRange(tmp, 0, 4))) &&  Character.isWhitespace(tmp[4])) {
                			ident = "OR ELSE";
                		} else {
                			reader.unread(tmp);
                			nextChar();
                			offset = new Position(0,0);
                		}
                	}
                	
                }
                
                Symbol.Id id = keywords.get(ident);
                if (id != null) {
                    symbol = new Symbol(id, pos);
                } else {
                    symbol = new Symbol(ident, pos);
                }
            } else {
                throw new CompileException("Unerwartetes Zeichen: " + (char) c + " (Code " + c + ")", position);
            }
        }
        if (printSymbols) {
            System.out.println(symbol.toString());
        }
    }
    
    /**
     * Gibt das zuletzt gelesene Symbol zurück.
     * Zuvor muss {@link #nextSymbol() nextSymbol} aufgerufen worden sein.
     * @return Das aktuelle Symbol.
     */
    public Symbol getSymbol() {
        return symbol;
    }
    
    /**
     * Gibt das zuvor gelesene Symbol zurück.
     * Zuvor muss {@link #nextSymbol() nextSymbol} 2 mal aufgerufen worden sein.
     * @return Das vorherige Symbol.
     */
    public Symbol getPreviousSymbol() {
        return previousSymbol;
    }
}
