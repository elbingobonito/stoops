package oopsc;

import java.io.FileNotFoundException;
import oopsc.parser.ResolvableIdentifier;
import oopsc.parser.SyntaxAnalysis;
import oopsc.streams.CodeStream;

/**
 * Diese Klasse stellt die Hauptmethode des Übersetzers für OOPS 
 * dar. Sie wertet die Kommandozeilen-Optionen aus und bietet 
 * eine Hilfe an, falls diese falsch sind.
 */
class OOPSC {
    /**
     * Die Hauptmethode des Übersetzers. 
     * Sie wertet die Kommandozeilen-Optionen aus und bietet eine Hilfe an, falls diese falsch sind.
     * Sind sie gültig, wird zuerst die Syntaxanalyse durchgeführt. Diese erzeugt den
     * Syntaxbaum des Programms, in dem dann die Kontextanalyse durchgeführt wird. Zum
     * Schluss wird dann der Code generiert.
     * @param args Die Kommandozeilenargumente. Diese sind im Quelltext der Methode 
     * {@link #usage usage} nachzulesen.
     */
    public static void main(String[] args) throws Exception {
        String inFile = null;
        String outFile = null;
        boolean showContext = false;
        boolean showSymbols = false;
        boolean showIdentifiers = false;
        boolean showSyntax = false;
        boolean optimize = false;
        int heapSize = 100;
        int stackSize = 100;

        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.equals("-c")) {
                showContext = true;
            } else if (arg.equals("-h")) {
                usage();
            } else if (arg.equals("-hs")) {
                if (++i < args.length) {
                    heapSize = Integer.parseInt(args[i]);
                } else {
                    System.out.println("Fehlendes Argument fuer " + arg);
                    usage();
                }
            } else if (arg.equals("-o")) {
            	optimize = true;
            } else if (arg.equals("-i")) {
                showIdentifiers = true;
            } else if (arg.equals("-l")) {
                showSymbols = true;
            } else if (arg.equals("-s")) {
                showSyntax = true;
            } else if (arg.equals("-ss")) {
                if (++i < args.length) {
                    stackSize = Integer.parseInt(args[i]);
                } else {
                    System.out.println("Fehlendes Argument fuer " + arg);
                    usage();
                }
            } else if (arg.length() > 0 && arg.charAt(0) == '-') {
                System.out.println("Unbekannte Option " + arg);
                usage();
            } else if (outFile != null) {
                System.out.println("Nur zwei Dateinamen erlaubt");
                usage();
            } else if (inFile != null) {
                outFile = arg;
            } else {
                inFile = arg;
            }
        }
            
        if (inFile == null) {
            System.out.println("Keine Quelldatei angegeben");
            usage();
        }
        
        try {
            Program p = new SyntaxAnalysis(inFile, showSymbols).parse();

            if (showSyntax) {
                p.printTree();
            }
            
            p.contextAnalysis();
            
            if (optimize) {
            	p.optimize();
            }
            
            if (showIdentifiers) {
                ResolvableIdentifier.print();
            }
            
            if (showContext) {
                p.printTree();
            }
            
            CodeStream stream = outFile == null ? new CodeStream() : new CodeStream(outFile);
            p.generateCode(stream, stackSize, heapSize);
            if (outFile != null) {
                stream.close();
            }
        } catch (CompileException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (FileNotFoundException e) {
            System.out.println("Fehler: "
                    + e.getMessage().substring(0, e.getMessage().indexOf(" ("))
                    + " kann nicht gefunden oder erzeugt werden");
            System.exit(2);
        }
    }
    
    /**
     * Die Methode gibt eine Hilfe auf der Konsole aus und beendet das Programm.
     */
    private static void usage() {
        System.out.println("java -jar OOPSC.jar [-c] [-h] [-hs <n>] [-i] [-l] [-s] [-ss <n>] <quelldatei> [<ausgabedatei>]");
        System.out.println("    -o       Optimiert den Syntaxbaum, bevor der Code generiert wird");
        System.out.println("    -c       Zeige das Ergebnis der Kontextanalyse");
        System.out.println("    -h       Zeige diese Hilfe");
        System.out.println("    -hs <n>  Reserviere <n> Worte fuer den Heap (Standard ist 100)");
        System.out.println("    -i       Zeige die Zuordnung der Bezeichner");
        System.out.println("    -l       Zeige das Ergebnis der lexikalischen Analyse");
        System.out.println("    -s       Zeige das Ergebnis der syntaktischen Analyse");
        System.out.println("    -ss <n>  Reserviere <n> Worte fuer den Stapel (Standard ist 100)");
        System.exit(2);
    }
}
