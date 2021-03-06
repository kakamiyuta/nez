//package nez.peg.regex;
//
//import java.io.IOException;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//
//import nez.ast.CommonTree;
//import nez.ast.Source;
//import nez.ast.Symbol;
//import nez.io.CommonSource;
//import nez.junks.GrammarFileLoader;
//import nez.junks.TreeVisitor;
//import nez.lang.Expression;
//import nez.lang.Expressions;
//import nez.lang.Grammar;
//import nez.parser.Parser;
//import nez.parser.ParserStrategy;
//import nez.util.ConsoleUtils;
//import nez.util.StringUtils;
//import nez.util.UList;
//import nez.util.Verbose;
//
//public class RegexGrammar extends TreeVisitor {
//
//	static Grammar regexGrammar = null;
//
//	public final static Grammar loadGrammar(Source regex, ParserStrategy option) throws IOException {
//		if (regexGrammar == null) {
//			try {
//				regexGrammar = GrammarFileLoader.loadGrammar("regex.nez", null);
//			} catch (IOException e) {
//				ConsoleUtils.exit(1, "can't load regex.nez");
//			}
//		}
//		Parser p = regexGrammar.newParser("File");
//		p.setDisabledUnconsumed(true);
//		CommonTree node = p.parse(regex);
//		p.ensureNoErrors();
//		Grammar grammar = new Grammar("re", null);
//		RegexGrammar conv = new RegexGrammar();
//		conv.convert(node, grammar);
//		return grammar;
//	}
//
//	public final static Parser newPrarser(String pattern) {
//		try {
//			Grammar grammar = loadGrammar(CommonSource.newStringSource(pattern), ParserStrategy.newDefaultStrategy() /* FIXME */);
//			return grammar.newParser("File");
//		} catch (IOException e) {
//			Verbose.traceException(e);
//		}
//		return null;
//	}
//
//	RegexGrammar() {
//	}
//
//	private Grammar grammar;
//
//	void convert(CommonTree e, Grammar grammar) {
//		this.grammar = grammar;
//		grammar.addProduction(e, "File", pi(e, null));
//		grammar.addProduction(e, "Chunk", grammar.newNonTerminal(e, "File"));
//	}
//
//	@Override
//	protected Method getClassMethod(String method, Symbol tag) throws NoSuchMethodException, SecurityException {
//		String name = method + tag.getSymbol();
//		return this.getClass().getMethod(name, CommonTree.class, Expression.class);
//	}
//
//	final Expression pi(CommonTree expr, Expression k) {
//		Symbol tag = expr.getTag();
//		Method m = findMethod("pi", tag);
//		if (m != null) {
//			try {
//				return (Expression) m.invoke(this, expr, k);
//			} catch (IllegalAccessException e) {
//				e.printStackTrace();
//			} catch (IllegalArgumentException e) {
//				e.printStackTrace();
//			} catch (InvocationTargetException e) {
//				e.printStackTrace();
//			}
//		}
//		return null;
//	}
//
//	public Expression piPattern(CommonTree e, Expression k) {
//		return this.pi(e.get(0), k);
//	}
//
//	// pi(e, k) e: regular expression, k: continuation
//	// pi(e1|e2, k) = pi(e1, k) / pi(e2, k)
//	public Expression piOr(CommonTree e, Expression k) {
//		return toChoice(e, pi(e.get(0), k), pi(e.get(1), k));
//	}
//
//	// pi(e1e2, k) = pi(e1, pi(e2, k))
//	public Expression piConcatenation(CommonTree e, Expression k) {
//		return pi(e.get(0), pi(e.get(1), k));
//	}
//
//	// pi((?>e), k) = pi(e, "") k
//	public Expression piIndependentExpr(CommonTree e, Expression k) {
//		return toSeq(e, pi(e.get(0), toEmpty(e)), k);
//	}
//
//	// pi((?=e), k) = &pi(e, "") k
//	public Expression piAnd(CommonTree e, Expression k) {
//		return toAnd(e, k);
//	}
//
//	// pi((?!e), k) = !pi(e, "") k
//	public Expression piNot(CommonTree e, Expression k) {
//		return toNot(e, k);
//	}
//
//	// pi(e*+, k) = pi(e*, "") k
//	public Expression piPossessiveRepetition(CommonTree e, Expression k) {
//		return toSeq(e, piRepetition(e, toEmpty(e)), k);
//	}
//
//	int NonTerminalCount = 0;
//
//	// pi(e*?, k) = A, A <- k / pi(e, A)
//	public Expression piLazyQuantifiers(CommonTree e, Expression k) {
//		String ruleName = "Repetition" + NonTerminalCount++;
//		Expression ne = Expressions.newNonTerminal(e, this.grammar, ruleName);
//		if (k == null) {
//			k = Expressions.newEmpty(null);
//		}
//		grammar.addProduction(e, ruleName, toChoice(e, k, pi(e.get(0), ne)));
//		return ne;
//	}
//
//	// pi(e*, k) = A, A <- pi(e, A) / k
//	public Expression piRepetition(CommonTree e, Expression k) {
//		String ruleName = "Repetition" + NonTerminalCount++;
//		Expression ne = Expressions.newNonTerminal(e, this.grammar, ruleName);
//		grammar.addProduction(e, ruleName, toChoice(e, pi(e.get(0), ne), k));
//		return ne;
//	}
//
//	// pi(e?, k) = pi(e, k) / k
//	public Expression piOption(CommonTree e, Expression k) {
//		return toChoice(e, pi(e.get(0), k), k);
//	}
//
//	public Expression piOneMoreRepetition(CommonTree e, Expression k) {
//		return pi(e.get(0), piRepetition(e, k));
//	}
//
//	public Expression piAny(CommonTree e, Expression k) {
//		return toSeq(e, k);
//	}
//
//	public Expression piNegativeCharacterSet(CommonTree e, Expression k) {
//		Expression nce = toSeq(e, Expressions.newNot(e, toCharacterSet(e)), toAny(e));
//		return toSeq(e, nce, k);
//	}
//
//	public Expression piCharacterSet(CommonTree e, Expression k) {
//		return toSeq(e, k);
//	}
//
//	public Expression piCharacterRange(CommonTree e, Expression k) {
//		return toSeq(e, k);
//	}
//
//	public Expression piCharacterSetItem(CommonTree e, Expression k) {
//		return toSeq(e, k);
//	}
//
//	// pi(c, k) = c k
//	// c: single character
//	public Expression piCharacter(CommonTree c, Expression k) {
//		return toSeq(c, k);
//	}
//
//	private Expression toExpression(CommonTree e) {
//		return (Expression) this.visit("to", e);
//	}
//
//	public Expression toCharacter(CommonTree c) {
//		String text = c.toText();
//		byte[] utf8 = StringUtils.toUtf8(text);
//		if (utf8.length != 1) {
//			ConsoleUtils.exit(1, "Error: not Character Literal");
//		}
//		return Expressions.newByte(null, utf8[0]);
//	}
//
//	boolean byteMap[];
//	boolean useByteMap = true;
//
//	public Expression toCharacterSet(CommonTree e) {
//		UList<Expression> l = new UList<Expression>(new Expression[e.size()]);
//		byteMap = new boolean[257];
//		for (CommonTree subnode : e) {
//			Expressions.addChoice(l, toExpression(subnode));
//		}
//		if (useByteMap) {
//			return Expressions.newByteSet(null, byteMap);
//		} else {
//			return Expressions.newChoice(l);
//		}
//	}
//
//	public Expression toCharacterRange(CommonTree e) {
//		byte[] begin = StringUtils.toUtf8(e.get(0).toText());
//		byte[] end = StringUtils.toUtf8(e.get(1).toText());
//		byteMap = new boolean[257];
//		for (byte i = begin[0]; i <= end[0]; i++) {
//			byteMap[i] = true;
//		}
//		return Expressions.newCharSet(null, e.get(0).toText(), e.get(1).toText());
//	}
//
//	public Expression toCharacterSetItem(CommonTree c) {
//		byte[] utf8 = StringUtils.toUtf8(c.toText());
//		byteMap[utf8[0]] = true;
//		return Expressions.newByte(null, utf8[0]);
//	}
//
//	public Expression toEmpty(CommonTree node) {
//		return Expressions.newEmpty(null);
//	}
//
//	public Expression toAny(CommonTree e) {
//		return Expressions.newAny(null);
//	}
//
//	public Expression toAnd(CommonTree e, Expression k) {
//		return toSeq(e, Expressions.newAnd(null, pi(e.get(0), toEmpty(e))), k);
//	}
//
//	public Expression toNot(CommonTree e, Expression k) {
//		return toSeq(e, Expressions.newNot(null, pi(e.get(0), toEmpty(e))), k);
//	}
//
//	public Expression toChoice(CommonTree node, Expression e, Expression k) {
//		UList<Expression> l = new UList<Expression>(new Expression[2]);
//		Expressions.addChoice(l, e);
//		if (k != null) {
//			Expressions.addChoice(l, k);
//		} else {
//			Expressions.addChoice(l, toEmpty(node));
//		}
//		return Expressions.newChoice(l);
//	}
//
//	public Expression toSeq(CommonTree e, Expression k) {
//		UList<Expression> l = new UList<Expression>(new Expression[2]);
//		Expressions.addSequence(l, toExpression(e));
//		if (k != null) {
//			Expressions.addSequence(l, k);
//		}
//		return Expressions.newPair(l);
//	}
//
//	public Expression toSeq(CommonTree node, Expression e, Expression k) {
//		UList<Expression> l = new UList<Expression>(new Expression[2]);
//		Expressions.addSequence(l, e);
//		if (k != null) {
//			Expressions.addSequence(l, k);
//		}
//		return Expressions.newPair(l);
//	}
//
// }
