import java.util.ArrayList;
import java.util.List;

public class CNFConverter {

    // 1. Eliminate biconditional (a <=> b)
    Expr eliminateIff(Expr e) {
        if (e instanceof Iff iff) {
            Expr a = iff.left, b = iff.right;
            return new And(
                    eliminateIff(new Impl(a, b)),
                    eliminateIff(new Impl(b, a)));
        } else if (e instanceof Impl impl) {
            return new Impl(eliminateIff(impl.left), eliminateIff(impl.right));
        } else if (e instanceof And and) {
            return new And(eliminateIff(and.left), eliminateIff(and.right));
        } else if (e instanceof Or or) {
            return new Or(eliminateIff(or.left), eliminateIff(or.right));
        } else if (e instanceof Not not) {
            return new Not(eliminateIff(not.e));
        } else
            return e;
    }

    // 2. Eliminate implication (a => b) => (¬a v b)
    Expr eliminateImpl(Expr e) {
        if (e instanceof Impl impl) {
            return new Or(new Not(eliminateImpl(impl.left)),
                    eliminateImpl(impl.right));
        } else if (e instanceof And and) {
            return new And(eliminateImpl(and.left), eliminateImpl(and.right));
        } else if (e instanceof Or or) {
            return new Or(eliminateImpl(or.left), eliminateImpl(or.right));
        } else if (e instanceof Not not) {
            return new Not(eliminateImpl(not.e));
        } else
            return e;
    }

    // 3. Push negations inward (De Morgan + double negation)
    Expr pushNeg(Expr e) {
        if (e instanceof Not not) {
            Expr x = not.e;
            if (x instanceof Not inner)
                return pushNeg(inner.e); // ¬¬a -> a
            if (x instanceof And and)
                return new Or(pushNeg(new Not(and.left)), // ¬(a ^ b) -> ¬a v ¬b
                        pushNeg(new Not(and.right)));
            if (x instanceof Or or)
                return new And(pushNeg(new Not(or.left)), // ¬(a v b) -> ¬a ^ ¬b
                        pushNeg(new Not(or.right)));
            return new Not(pushNeg(x)); // negation on atom
        }
        if (e instanceof And and)
            return new And(pushNeg(and.left), pushNeg(and.right));
        if (e instanceof Or or)
            return new Or(pushNeg(or.left), pushNeg(or.right));
        return e;
    }

    // 4. Distribute OR over AND
    Expr distribute(Expr e) {
        if (e instanceof Or or) {
            Expr a = distribute(or.left);
            Expr b = distribute(or.right);
            if (a instanceof And andA) {
                // a v (b ^ c) -> (a v b) ^ (a v c)
                return new And(
                        distribute(new Or(andA.left, b)),
                        distribute(new Or(andA.right, b)));
            } else if (b instanceof And andB) {
                // (a ^ b) v c -> (a v c) ^ (b v c)
                return new And(
                        distribute(new Or(a, andB.left)),
                        distribute(new Or(a, andB.right)));
            } else
                return new Or(a, b);
        } else if (e instanceof And and) {
            return new And(distribute(and.left), distribute(and.right));
        } else
            return e;
    }

    // 5. Extract CNF clauses (each AND level = separate clause)
    List<List<String>> toClauses(Expr e) {
        List<List<String>> result = new ArrayList<>();
        if (e instanceof And and) {
            result.addAll(toClauses(and.left));
            result.addAll(toClauses(and.right));
        } else if (e instanceof Or or) {
            List<String> clause = new ArrayList<>();
            collectOr(or, clause);
            result.add(clause);
        } else if (e instanceof Not not && not.e instanceof Var v) {
            result.add(List.of("!" + v.name));
        } else if (e instanceof Var v) {
            result.add(List.of(v.name));
        }
        return result;
    }

    void collectOr(Expr e, List<String> clause) {
        if (e instanceof Or or) {
            collectOr(or.left, clause);
            collectOr(or.right, clause);
        } else if (e instanceof Not not && not.e instanceof Var v) {
            clause.add("!" + v.name);
        } else if (e instanceof Var v) {
            clause.add(v.name);
        }
    }

    public static void main(String[] args) {
        Expr newAnd = new And(new Var("C"), new Var("A"));
        Expr newOr = new Or(new Var("B"), new Var("D"));
        Expr ifExpr = new Impl(newAnd,newOr); 
        CNFConverter converter = new CNFConverter();
        Expr noIff = converter.eliminateIff(ifExpr);
        Expr noImpl = converter.eliminateImpl(noIff);
        Expr pushed = converter.pushNeg(noImpl);
        Expr dist = converter.distribute(pushed);
        List<List<String>> cnf = converter.toClauses(dist);

        System.out.println("CNF clauses: " + cnf);
    }
}
