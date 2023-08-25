package utilities;

import java.util.*;

interface Gene {
    double coef(int n);
}
 
class Term {
    private final List<Double> cache = new ArrayList<Double>();
    private final Gene gene;
 
    public Term(Gene g) { gene = g; }
 
    public double get(int n) {
        if (n < 0)
            return 0;
        else if (n >= cache.size())
            for (int i = cache.size(); i <= n; i++)
                cache.add(gene.coef(i));
        return cache.get(n);
    }
}
 
public class FormalPS {
    private static final int DISP_TERM = 12;
    private static final String X_VAR = "x";
    private Term term;
 
    public FormalPS() { }
    public void copyFrom(FormalPS foo) {
        term = foo.term;
    }
 
    public FormalPS(Term t) {
        term = t;
    }
 
    public FormalPS(final double[] polynomial) {
        this(new Term(new Gene() {
                public double coef(int n) {
                    if (n < 0 || n >= polynomial.length)
                        return 0;
                    else
                        return polynomial[n];
                }
            }));
    }
 
    public double inverseCoef(int n) {
        double[] res = new double[n + 1];
        res[0] = 1 / term.get(0);
        for (int i = 1; i <= n; i++) {
            res[i] = 0;
            for (int j = 0; j < i; j++)
                res[i] += term.get(i-j) * res[j];
            res[i] *= -res[0];
        }
        return res[n];
    }
 
    public FormalPS add(final FormalPS rhs) {
        return new FormalPS(new Term(new Gene() {
                public double coef(int n) {
                    return term.get(n) + rhs.term.get(n);
                }
            }));
    }
 
    public FormalPS sub(final FormalPS rhs) {
        return new FormalPS(new Term(new Gene() {
                public double coef(int n) {
                    return term.get(n) - rhs.term.get(n);
                }
            }));
    }
 
    public FormalPS mul(final FormalPS rhs) {
        return new FormalPS(new Term(new Gene() {
                public double coef(int n) {
                    double res = 0;
                    for (int i = 0; i <= n; i++)
                        res += term.get(i) * rhs.term.get(n-i);
                    return res;
                }
            }));
    }
 
    public FormalPS div(final FormalPS rhs) {
        return new FormalPS(new Term(new Gene() {
                public double coef(int n) {
                    double res = 0;
                    for (int i = 0; i <= n; i++)
                        res += term.get(i) * rhs.inverseCoef(n-i);
                    return res;
                }
            }));
    }
 
    public FormalPS diff() {
        return new FormalPS(new Term(new Gene() {
                public double coef(int n) {
                    return term.get(n+1) * (n+1);
                }
            }));
    }
 
    public FormalPS intg() {
        return new FormalPS(new Term(new Gene() {
                public double coef(int n) {
                    if (n == 0)
                        return 0;
                    else
                        return term.get(n-1) / n;
                }
            }));
    }
 
    public String toString() {
        return toString(DISP_TERM);
    }
 
    public String toString(int dpTerm) {
        StringBuffer s = new StringBuffer();
        {
            double c = term.get(0);
            if (c != 0)
                s.append(c);
        }
        for (int i = 1; i < dpTerm; i++) {
            double c = term.get(i);
            if (c != 0) {
                if (c > 0 && s.length() > 0)
                    s.append("+");
                if (c == 1)
                    s.append(X_VAR);
                else if (c == -1)
                    s.append("-" + X_VAR);
                else
                    s.append(c + X_VAR);
                if (i > 1)
                    s.append(i);
            }
        }
        if (s.length() == 0)
            s.append("0");
        s.append("+...");
        return s.toString();
    }
 
    public static void main(String[] args) {
        FormalPS cos = new FormalPS();
        
        List<FormalPS> array = new ArrayList() ;
        array.add(new RationalPolynomial(new BigRational(1, 1),0));
        array.add(l2);
        array.add(l2.times(l2));
        array.add(l2.times(l2).times(l2));
        
        
        FormalPS sin = cos.intg();
        cos.copyFrom(new FormalPS(new double[]{1}).sub(sin.intg()));
        System.out.println("SIN(x) = " + sin);
        System.out.println("COS(x) = " + cos);
    }
}