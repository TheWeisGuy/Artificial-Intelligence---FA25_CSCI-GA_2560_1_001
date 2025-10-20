import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.io.FileWriter;

/*
Parses the BNF generated from GraphToBNF to CNF
Only works with the expressions generated from that, this is not a general BNF parser
*/
public class BNFToCNF {
    private String outputName;

    /**
     * Constructor
     * @param txtFile    Input BNF .txt file generated from GraphToBNF
     * @param outputName
     */
    public BNFToCNF(String outputName) {
        this.outputName = outputName;
    }

    // If no output path provided, write to output.txt
    public BNFToCNF() {
        this.outputName = "output.txt";
    }

    /**
     * Parses the BNF .txt file and outputs the converted CNF
     * 
     * @param txtFile
     */
    public void parseBNF(String txtFile) {
        FileWriter writer;
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(txtFile));
            writer = new FileWriter(this.outputName);
            String currentLine = reader.readLine();

            // parse all BNF expressions and convert to CNF
            while (currentLine != null) {
                if (currentLine == "") {
                    currentLine = reader.readLine();
                    continue;
                }
                ArrayList<String> parsed = toCNF(currentLine);

                for (String expression : parsed) {
                    writer.write(expression + "\n");
                    ;
                }

                currentLine = reader.readLine();
            }
            writer.close();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Convers a BNF string to CNF
     * 
     * @param bnf
     * @return ArrayList containing converted CNF
     */
    public ArrayList<String> toCNF(String bnf) {
        ArrayList<String> cnf = new ArrayList<>();
        bnf = bnf.trim();

        // case 1: OR clause like 4_0|4_1|4_2
        if (!bnf.contains("=>")) {
            StringBuilder clause = new StringBuilder();
            for (String token : bnf.split("\\|")) {
                clause.append(token.trim()).append(" ");
            }
            cnf.add(clause.toString().trim());
            return cnf;
        }

        // case 2: implication like 1_0=>[2_0]
        String[] parts = bnf.split("=>", 2);
        if (parts.length < 2) {
            // malformed line
            cnf.add("# ERROR: invalid implication => in line: " + bnf);
            return cnf;
        }

        String left = parts[0].trim();
        String right = parts[1].trim();

        // case 3: ![B|C] or !B
        if (right.startsWith("![")) {
            // A => ![B|C] → (!A | !B) ∧ (!A | !C)
            String inner = right.substring(2, right.length() - 1);
            for (String tok : inner.split("\\|")) {
                String lit = tok.trim();
                cnf.add("!" + left + " !" + lit);
            }

        } else if (right.startsWith("!")) {
            // A => !B → (!A | !B)
            String lit = right.substring(1).trim();
            cnf.add("!" + left + " !" + lit);

        } else if (right.startsWith("[")) {
            // A => [B|C] → (!A | B | C)
            String inner = right.substring(1, right.length() - 1);
            StringBuilder clause = new StringBuilder();
            clause.append("!").append(left);
            for (String token : inner.split("\\|")) {
                clause.append(" ").append(token.trim());
            }
            cnf.add(clause.toString());

        } else {
            // A => B → (!A | B)
            cnf.add("!" + left + " " + right);
        }

        return cnf;
    }

    //main method for local debugging
    public static void main(String[] args) {
        BNFToCNF converter = new BNFToCNF("testOut.txt");
        converter.parseBNF("tiny.txt.bnf2.dp");
    }
}