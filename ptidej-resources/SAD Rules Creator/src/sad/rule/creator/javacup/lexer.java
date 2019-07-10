package sad.rule.creator.javacup;

import java.util.Hashtable;
import sad.rule.creator.javacup.runtime.Symbol;

/** This class implements a small scanner (aka lexical analyzer or lexer) for
 *  the JavaCup specification.  This scanner reads characters from standard 
 *  input (System.in) and returns integers corresponding to the terminal 
 *  number of the next Symbol. Once end of input is reached the EOF Symbol is 
 *  returned on every subsequent call.<p>
 *  Symbols currently returned include: <pre>
 *    Symbol        Constant Returned     Symbol        Constant Returned
 *    ------        -----------------     ------        -----------------
 *    "package"     PACKAGE               "import"      IMPORT 
 *    "code"        CODE                  "action"      ACTION 
 *    "parser"      PARSER                "terminal"    TERMINAL
 *    "non"         NON                   "init"        INIT 
 *    "scan"        SCAN                  "with"        WITH
 *    "start"       START                 "precedence"  PRECEDENCE
 *    "left"        LEFT		  "right"       RIGHT
 *    "nonassoc"    NONASSOC		  "%prec        PRECENT_PREC  
 *      [           LBRACK                  ]           RBRACK
 *      ;           SEMI 
 *      ,           COMMA                   *           STAR 
 *      .           DOT                     :           COLON
 *      ::=         COLON_COLON_EQUALS      |           BAR
 *    identifier    ID                    {:...:}       CODE_STRING
 *    "nonterminal" NONTERMINAL
 *  </pre>
 *  All symbol constants are defined in sym.java which is generated by 
 *  JavaCup from parser.cup.<p>
 * 
 *  In addition to the scanner proper (called first via init() then with
 *  next_token() to get each Symbol) this class provides simple error and 
 *  warning routines and keeps a count of errors and warnings that is 
 *  publicly accessible.<p>
 *  
 *  This class is "static" (i.e., it has only static members and methods).
 *
 * @version last updated: 7/3/96
 * @author  Frank Flannery
 */
public class lexer {

	/*-----------------------------------------------------------*/
	/*--- Constructor(s) ----------------------------------------*/
	/*-----------------------------------------------------------*/

	/** First character of lookahead. */
	protected static int next_char;

	/*-----------------------------------------------------------*/
	/*--- Static (Class) Variables ------------------------------*/
	/*-----------------------------------------------------------*/

	/** Second character of lookahead. */
	protected static int next_char2;

	/** Second character of lookahead. */
	protected static int next_char3;

	/** Second character of lookahead. */
	protected static int next_char4;

	/** EOF constant. */
	protected static final int EOF_CHAR = -1;

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Table of keywords.  Keywords are initially treated as identifiers.
	 *  Just before they are returned we look them up in this table to see if
	 *  they match one of the keywords.  The string of the name is the key here,
	 *  which indexes Integer objects holding the symbol number. 
	 */
	protected static Hashtable keywords = new Hashtable(23);

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Table of single character symbols.  For ease of implementation, we 
	 *  store all unambiguous single character Symbols in this table of Integer
	 *  objects keyed by Integer objects with the numerical value of the 
	 *  appropriate char (currently Character objects have a bug which precludes
	 *  their use in tables).
	 */
	protected static Hashtable char_symbols = new Hashtable(11);

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Current line number for use in error messages. */
	protected static int current_line = 1;

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Character position in current line. */
	protected static int current_position = 1;

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Character position in current line. */
	protected static int absolute_position = 1;

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Count of total errors detected so far. */
	public static int error_count = 0;

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Count of warnings issued so far */
	public static int warning_count = 0;

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Advance the scanner one character in the input stream.  This moves
	 * next_char2 to next_char and then reads a new next_char2.  
	 */
	protected static void advance() throws java.io.IOException {
		int old_char;

		old_char = lexer.next_char;
		lexer.next_char = lexer.next_char2;
		if (lexer.next_char == lexer.EOF_CHAR) {
			lexer.next_char2 = lexer.EOF_CHAR;
			lexer.next_char3 = lexer.EOF_CHAR;
			lexer.next_char4 = lexer.EOF_CHAR;
		}
		else {
			lexer.next_char2 = lexer.next_char3;
			if (lexer.next_char2 == lexer.EOF_CHAR) {
				lexer.next_char3 = lexer.EOF_CHAR;
				lexer.next_char4 = lexer.EOF_CHAR;
			}
			else {
				lexer.next_char3 = lexer.next_char4;
				if (lexer.next_char3 == lexer.EOF_CHAR) {
					lexer.next_char4 = lexer.EOF_CHAR;
				}
				else {
					lexer.next_char4 = System.in.read();
				}
			}
		}

		/* count this */
		lexer.absolute_position++;
		lexer.current_position++;
		if (old_char == '\n' || old_char == '\r' && lexer.next_char != '\n') {
			lexer.current_line++;
			lexer.current_position = 1;
		}
	}

	/*-----------------------------------------------------------*/
	/*--- Static Methods ----------------------------------------*/
	/*-----------------------------------------------------------*/

	/** Debugging version of next_token().  This routine calls the real scanning
	 *  routine, prints a message on System.out indicating what the Symbol is,
	 *  then returns it.
	 */
	public static Symbol debug_next_token() throws java.io.IOException {
		final Symbol result = lexer.real_next_token();
		System.out.println("# next_Symbol() => " + result.sym);
		return result;
	}

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Swallow up a code string.  Code strings begin with "{:" and include
	 all characters up to the first occurrence of ":}" (there is no way to 
	 include ":}" inside a code string).  The routine returns a String
	 object suitable for return by the scanner.
	 */
	protected static Symbol do_code_string() throws java.io.IOException {
		final StringBuffer result = new StringBuffer();

		/* at this point we have lookahead of "{:" -- swallow that */
		lexer.advance();
		lexer.advance();

		/* save chars until we see ":}" */
		while (!(lexer.next_char == ':' && lexer.next_char2 == '}')) {
			/* if we have run off the end issue a message and break out of loop */
			if (lexer.next_char == lexer.EOF_CHAR) {
				lexer
					.emit_error("Specification file ends inside a code string");
				break;
			}

			/* otherwise record the char and move on */
			result.append(new Character((char) lexer.next_char));
			lexer.advance();
		}

		/* advance past the closer and build a return Symbol */
		lexer.advance();
		lexer.advance();
		return new Symbol(sym.CODE_STRING, result.toString());
	}

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Process an identifier.  Identifiers begin with a letter, underscore,
	 *  or dollar sign, which is followed by zero or more letters, numbers,
	 *  underscores or dollar signs.  This routine returns a String suitable
	 *  for return by the scanner.
	 */
	protected static Symbol do_id() throws java.io.IOException {
		final StringBuffer result = new StringBuffer();
		String result_str;
		Integer keyword_num;
		final char buffer[] = new char[1];

		/* next_char holds first character of id */
		buffer[0] = (char) lexer.next_char;
		result.append(buffer, 0, 1);
		lexer.advance();

		/* collect up characters while they fit in id */
		while (lexer.id_char(lexer.next_char)) {
			buffer[0] = (char) lexer.next_char;
			result.append(buffer, 0, 1);
			lexer.advance();
		}

		/* extract a string and try to look it up as a keyword */
		result_str = result.toString();
		keyword_num = (Integer) lexer.keywords.get(result_str);

		/* if we found something, return that keyword */
		if (keyword_num != null) {
			return new Symbol(keyword_num.intValue());
		}

		/* otherwise build and return an id Symbol with an attached string */
		return new Symbol(sym.ID, result_str);
	}

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Emit an error message.  The message will be marked with both the 
	 *  current line number and the position in the line.  Error messages
	 *  are printed on standard error (System.err).
	 * @param message the message to print.
	 */
	public static void emit_error(final String message) {
		System.err.println("Error at " + lexer.current_line + "("
				+ lexer.current_position + "): " + message);
		lexer.error_count++;
	}

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Emit a warning message.  The message will be marked with both the 
	 *  current line number and the position in the line.  Messages are 
	 *  printed on standard error (System.err).
	 * @param message the message to print.
	 */
	public static void emit_warn(final String message) {
		System.err.println("Warning at " + lexer.current_line + "("
				+ lexer.current_position + "): " + message);
		lexer.warning_count++;
	}

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Try to look up a single character symbol, returns -1 for not found. 
	 * @param ch the character in question.
	 */
	protected static int find_single_char(final int ch) {
		Integer result;

		result = (Integer) lexer.char_symbols.get(new Integer((char) ch));
		if (result == null) {
			return -1;
		}
		else {
			return result.intValue();
		}
	}

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Determine if a character is ok for the middle of an id.
	 * @param ch the character in question. 
	 */
	protected static boolean id_char(final int ch) {
		return lexer.id_start_char(ch) || ch >= '0' && ch <= '9';
	}

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Determine if a character is ok to start an id. 
	 * @param ch the character in question.
	 */
	protected static boolean id_start_char(final int ch) {
		/* allow for % in identifiers.  a hack to allow my
		 %prec in.  Should eventually make lex spec for this 
		 frankf */
		return ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch == '_';

		// later need to deal with non-8-bit chars here
	}

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Initialize the scanner.  This sets up the keywords and char_symbols
	 * tables and reads the first two characters of lookahead.  
	 */
	public static void init() throws java.io.IOException {
		/* set up the keyword table */
		lexer.keywords.put("package", new Integer(sym.PACKAGE));
		lexer.keywords.put("import", new Integer(sym.IMPORT));
		lexer.keywords.put("code", new Integer(sym.CODE));
		lexer.keywords.put("action", new Integer(sym.ACTION));
		lexer.keywords.put("parser", new Integer(sym.PARSER));
		lexer.keywords.put("terminal", new Integer(sym.TERMINAL));
		lexer.keywords.put("non", new Integer(sym.NON));
		lexer.keywords.put("nonterminal", new Integer(sym.NONTERMINAL));// [CSA]
		lexer.keywords.put("init", new Integer(sym.INIT));
		lexer.keywords.put("scan", new Integer(sym.SCAN));
		lexer.keywords.put("with", new Integer(sym.WITH));
		lexer.keywords.put("start", new Integer(sym.START));
		lexer.keywords.put("precedence", new Integer(sym.PRECEDENCE));
		lexer.keywords.put("left", new Integer(sym.LEFT));
		lexer.keywords.put("right", new Integer(sym.RIGHT));
		lexer.keywords.put("nonassoc", new Integer(sym.NONASSOC));

		/* set up the table of single character symbols */
		lexer.char_symbols.put(new Integer(';'), new Integer(sym.SEMI));
		lexer.char_symbols.put(new Integer(','), new Integer(sym.COMMA));
		lexer.char_symbols.put(new Integer('*'), new Integer(sym.STAR));
		lexer.char_symbols.put(new Integer('.'), new Integer(sym.DOT));
		lexer.char_symbols.put(new Integer('|'), new Integer(sym.BAR));
		lexer.char_symbols.put(new Integer('['), new Integer(sym.LBRACK));
		lexer.char_symbols.put(new Integer(']'), new Integer(sym.RBRACK));

		/* read two characters of lookahead */
		lexer.next_char = System.in.read();
		if (lexer.next_char == lexer.EOF_CHAR) {
			lexer.next_char2 = lexer.EOF_CHAR;
			lexer.next_char3 = lexer.EOF_CHAR;
			lexer.next_char4 = lexer.EOF_CHAR;
		}
		else {
			lexer.next_char2 = System.in.read();
			if (lexer.next_char2 == lexer.EOF_CHAR) {
				lexer.next_char3 = lexer.EOF_CHAR;
				lexer.next_char4 = lexer.EOF_CHAR;
			}
			else {
				lexer.next_char3 = System.in.read();
				if (lexer.next_char3 == lexer.EOF_CHAR) {
					lexer.next_char4 = lexer.EOF_CHAR;
				}
				else {
					lexer.next_char4 = System.in.read();
				}
			}
		}
	}

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Return one Symbol.  This is the main external interface to the scanner.
	 *  It consumes sufficient characters to determine the next input Symbol
	 *  and returns it.  To help with debugging, this routine actually calls
	 *  real_next_token() which does the work.  If you need to debug the 
	 *  parser, this can be changed to call debug_next_token() which prints
	 *  a debugging message before returning the Symbol.
	 */
	public static Symbol next_token() throws java.io.IOException {
		return lexer.real_next_token();
	}

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** The actual routine to return one Symbol.  This is normally called from
	 *  next_token(), but for debugging purposes can be called indirectly from
	 *  debug_next_token(). 
	 */
	protected static Symbol real_next_token() throws java.io.IOException {
		int sym_num;

		for (;;) {
			/* look for white space */
			if (lexer.next_char == ' ' || lexer.next_char == '\t'
					|| lexer.next_char == '\n' || lexer.next_char == '\f'
					|| lexer.next_char == '\r') {
				/* advance past it and try the next character */
				lexer.advance();
				continue;
			}

			/* look for a single character symbol */
			sym_num = lexer.find_single_char(lexer.next_char);
			if (sym_num != -1) {
				/* found one -- advance past it and return a Symbol for it */
				lexer.advance();
				return new Symbol(sym_num);
			}

			/* look for : or ::= */
			if (lexer.next_char == ':') {
				/* if we don't have a second ':' return COLON */
				if (lexer.next_char2 != ':') {
					lexer.advance();
					return new Symbol(sym.COLON);
				}

				/* move forward and look for the '=' */
				lexer.advance();
				if (lexer.next_char2 == '=') {
					lexer.advance();
					lexer.advance();
					return new Symbol(sym.COLON_COLON_EQUALS);
				}
				else {
					/* return just the colon (already consumed) */
					return new Symbol(sym.COLON);
				}
			}

			/* find a "%prec" string and return it.  otherwise, a '%' was found,
			 which has no right being in the specification otherwise */
			if (lexer.next_char == '%') {
				lexer.advance();
				if (lexer.next_char == 'p' && lexer.next_char2 == 'r'
						&& lexer.next_char3 == 'e' && lexer.next_char4 == 'c') {
					lexer.advance();
					lexer.advance();
					lexer.advance();
					lexer.advance();
					return new Symbol(sym.PERCENT_PREC);
				}
				else {
					lexer.emit_error("Found extraneous percent sign");
				}
			}

			/* look for a comment */
			if (lexer.next_char == '/'
					&& (lexer.next_char2 == '*' || lexer.next_char2 == '/')) {
				/* swallow then continue the scan */
				lexer.swallow_comment();
				continue;
			}

			/* look for start of code string */
			if (lexer.next_char == '{' && lexer.next_char2 == ':') {
				return lexer.do_code_string();
			}

			/* look for an id or keyword */
			if (lexer.id_start_char(lexer.next_char)) {
				return lexer.do_id();
			}

			/* look for EOF */
			if (lexer.next_char == lexer.EOF_CHAR) {
				return new Symbol(sym.EOF);
			}

			/* if we get here, we have an unrecognized character */
			lexer.emit_warn("Unrecognized character '"
					+ new Character((char) lexer.next_char) + "'("
					+ lexer.next_char + ") -- ignored");

			/* advance past it */
			lexer.advance();
		}
	}

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** Handle swallowing up a comment.  Both old style C and new style C++
	 *  comments are handled.
	 */
	protected static void swallow_comment() throws java.io.IOException {
		/* next_char == '/' at this point */

		/* is it a traditional comment */
		if (lexer.next_char2 == '*') {
			/* swallow the opener */
			lexer.advance();
			lexer.advance();

			/* swallow the comment until end of comment or EOF */
			for (;;) {
				/* if its EOF we have an error */
				if (lexer.next_char == lexer.EOF_CHAR) {
					lexer
						.emit_error("Specification file ends inside a comment");
					return;
				}

				/* if we can see the closer we are done */
				if (lexer.next_char == '*' && lexer.next_char2 == '/') {
					lexer.advance();
					lexer.advance();
					return;
				}

				/* otherwise swallow char and move on */
				lexer.advance();
			}
		}

		/* is its a new style comment */
		if (lexer.next_char2 == '/') {
			/* swallow the opener */
			lexer.advance();
			lexer.advance();

			/* swallow to '\n', '\r', '\f', or EOF */
			while (lexer.next_char != '\n' && lexer.next_char != '\r'
					&& lexer.next_char != '\f'
					&& lexer.next_char != lexer.EOF_CHAR) {
				lexer.advance();
			}

			return;

		}

		/* shouldn't get here, but... if we get here we have an error */
		lexer.emit_error("Malformed comment in specification -- ignored");
		lexer.advance();
	}

	/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

	/** The only constructor is private, so no instances can be created. */
	private lexer() {
	}

	/*-----------------------------------------------------------*/
}
