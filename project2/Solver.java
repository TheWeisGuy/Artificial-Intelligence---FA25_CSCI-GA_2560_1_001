import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Solver {
    

    /**
     * Main method for parsing our graph and assigning colors
     * @param args Usage: solver [-v] <ncolors> <input-file>
     */
    public static void main(String[] args) {
        try {

            if (args.length < 2) {
                System.out.println("Usage: solver [-v] <ncolors> <input-file>");
                System.out.println("  -v           Optional verbose flag");
                System.out.println("  <ncolors>    Number of colors (2=RG, 3=RGB, 4=RGBY)");
                System.out.println("  <input-file> Graph input file");
                return;
            }

            int argIndex = 0;
            boolean verbose;
            // Optional -v
            if (args[0].equals("-v")) {
                verbose = true;
                argIndex++;
            } else {
                verbose = false;
            }

            if (argIndex + 2 > args.length) {
                System.out.println("Error: Missing arguments.");
                System.out.println("Usage: solver [-v] <ncolors> <input-file>");
                return;
            }

            // Parse number of colors
            int nColors;
            try {
                nColors = Integer.parseInt(args[argIndex]);
            } catch (NumberFormatException e) {
                System.out.println("Error: ncolors must be an integer.");
                return;
            }

            String inputPath = args[argIndex + 1];

            DPLLSolver solver = new DPLLSolver(verbose);
            GraphToBNF graphToBNF = new GraphToBNF(nColors);
            graphToBNF.parseFile(inputPath);
            String bnfOutputPath = inputPath + ".bnf." + nColors + ".dp";

            String cnfOutputPath = inputPath + "." + nColors + ".dp";
            BNFToCNF bnfToCNF = new BNFToCNF(cnfOutputPath,verbose);
            bnfToCNF.parseBNF(bnfOutputPath);

            File CNFFile = new File(cnfOutputPath);

            List<List<String>> cnf;
            cnf = DPLLSolver.parseCNF(CNFFile);
            boolean result = solver.solve(cnf);
            FileWriter writer = new FileWriter(inputPath+"."+nColors+".out");
            if (result == false) {
                writer.write("No solution for " + nColors + " colors");
                writer.close();
                return;
            }

            ArrayList<String> assignments = new ArrayList<>();
            solver.getAssignments().forEach((k, v) -> {
                if (verbose == true) {
                    System.out.print(k + "=" + v + " ");
                }
                if (v == true) {
                    assignments.add(k);
                }
            });

            HashMap<String, String> colorsMap = new HashMap<>();
            colorsMap.put("0", "Red");
            colorsMap.put("1", "Green");
            colorsMap.put("2", "Blue");
            colorsMap.put("3", "Yellow");
            for (String k : assignments) {
                String[] splitK = k.split("_");
                String color = splitK[1].split("\\s")[0];
                writer.write(splitK[0] + " = " + colorsMap.get(color) + "\n");
            }
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
