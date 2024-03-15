package org.json;

import java.io.*;
import java.nio.charset.Charset;

/*
Public Domain.
 */

/**
 * A JSONTokener takes a source string and extracts characters and tokens from it. It is used by the JSONObject and
 * JSONArray constructors to parse JSON source strings.
 *
 * @author JSON.org
 * @version 2014-05-03
 */
public class JSONTokener {

    /**
     * current read character position on the current line.
     */
    private long character;
    /**
     * flag to indicate if the end of the input has been found.
     */
    private boolean eof;
    /**
     * current read index of the input.
     */
    private long index;
    /**
     * current line of the input.
     */
    private long line;
    /**
     * previous character read from the input.
     */
    private char previous;
    /**
     * Reader for the input.
     */
    private final Reader reader;
    /**
     * flag to indicate that a previous character was requested.
     */
    private boolean usePrevious;
    /**
     * the number of characters read in the previous line.
     */
    private long characterPreviousLine;


    /**
     * Construct a JSONTokener from a Reader. The caller must close the Reader.
     *
     * @param reader A reader.
     */
    public JSONTokener(Reader reader) {
        this.reader = reader.markSupported()
            ? reader
            : new BufferedReader(reader);
        this.eof = false;
        this.usePrevious = false;
        this.previous = 0;
        this.index = 0;
        this.character = 1;
        this.characterPreviousLine = 0;
        this.line = 1;
    }


    /**
     * Construct a JSONTokener from an InputStream. The caller must close the input stream.
     *
     * @param inputStream The source.
     */
    public JSONTokener(InputStream inputStream) {
        this(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    }


    /**
     * Construct a JSONTokener from a string.
     *
     * @param s A source string.
     */
    public JSONTokener(String s) {
        this(new StringReader(s));
    }


    /**
     * Back up one character. This provides a sort of lookahead capability, so that you can test for a digit or letter
     * before attempting to parse the next number or identifier.
     *
     * @throws JSONException Thrown if trying to step back more than 1 step or if already at the start of the string
     */
    public void back() throws JSONException {
        if (this.usePrevious || this.index <= 0) {
            throw new JSONException("Stepping back two steps is not supported");
        }
        this.decrementIndexes();
        this.usePrevious = true;
        this.eof = false;
    }

    /**
     * Decrements the indexes for the {@link #back()} method based on the previous character read.
     */
    private void decrementIndexes() {
        this.index--;
        if (this.previous == '\r' || this.previous == '\n') {
            this.line--;
            this.character = this.characterPreviousLine;
        } else if (this.character > 0) {
            this.character--;
        }
    }

    /**
     * Get the hex value of a character (base16).
     *
     * @param c A character between '0' and '9' or between 'A' and 'F' or between 'a' and 'f'.
     * @return An int between 0 and 15, or -1 if c was not a hex digit.
     */
    public static int dehexchar(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return c - ('A' - 10);
        }
        if (c >= 'a' && c <= 'f') {
            return c - ('a' - 10);
        }
        return -1;
    }

    /**
     * Checks if the end of the input has been reached.
     *
     * @return true if at the end of the file and we didn't step back
     */
    public boolean end() {
        return this.eof && !this.usePrevious;
    }


    /**
     * Determine if the source string still contains characters that next() can consume.
     *
     * @return true if not yet at the end of the source.
     * @throws JSONException thrown if there is an error stepping forward or backward while checking for more data.
     */
    public boolean more() throws JSONException {
        if (this.usePrevious) {
            return true;
        }
        try {
            this.reader.mark(1);
        } catch (IOException e) {
            throw new JSONException("Unable to preserve stream position", e);
        }
        try {
            // -1 is EOF, but next() can not consume the null character '\0'
            if (this.reader.read() <= 0) {
                this.eof = true;
                return false;
            }
            this.reader.reset();
        } catch (IOException e) {
            throw new JSONException("Unable to read the next character from the stream", e);
        }
        return true;
    }


    /**
     * Get the next character in the source string.
     *
     * @return The next character, or 0 if past the end of the source string.
     * @throws JSONException Thrown if there is an error reading the source string.
     */
    public char next() throws JSONException {
        int c;
        if (this.usePrevious) {
            this.usePrevious = false;
            c = this.previous;
        } else {
            try {
                c = this.reader.read();
            } catch (IOException exception) {
                throw new JSONException(exception);
            }
        }
        if (c <= 0) { // End of stream
            this.eof = true;
            return 0;
        }
        this.incrementIndexes(c);
        this.previous = (char) c;
        return this.previous;
    }

    /**
     * Get the last character read from the input or '\0' if nothing has been read yet.
     *
     * @return the last character read from the input.
     */
    protected char getPrevious() {
        return this.previous;
    }

    /**
     * Increments the internal indexes according to the previous character read and the character passed as the current
     * character.
     *
     * @param c the current character read.
     */
    private void incrementIndexes(int c) {
        if (c > 0) {
            this.index++;
            if (c == '\r') {
                this.line++;
                this.characterPreviousLine = this.character;
                this.character = 0;
            } else if (c == '\n') {
                if (this.previous != '\r') {
                    this.line++;
                    this.characterPreviousLine = this.character;
                }
                this.character = 0;
            } else {
                this.character++;
            }
        }
    }

    /**
     * Consume the next character, and check that it matches a specified character.
     *
     * @param c The character to match.
     * @return The character.
     * @throws JSONException if the character does not match.
     */
    public char next(char c) throws JSONException {
        char n = this.next();
        if (n != c) {
            if (n > 0) {
                throw this.syntaxError("Expected '" + c + "' and instead saw '" +
                    n + "'");
            }
            throw this.syntaxError("Expected '" + c + "' and instead saw ''");
        }
        return n;
    }


    /**
     * Get the next n characters.
     *
     * @param n The number of characters to take.
     * @return A string of n characters.
     * @throws JSONException Substring bounds error if there are not n characters remaining in the source string.
     */
    public String next(int n) throws JSONException {
        if (n == 0) {
            return "";
        }

        char[] chars = new char[n];
        int pos = 0;

        while (pos < n) {
            chars[pos] = this.next();
            if (this.end()) {
                throw this.syntaxError("Substring bounds error");
            }
            pos += 1;
        }
        return new String(chars);
    }


    /**
     * Get the next char in the string, skipping whitespace.
     *
     * @return A character, or 0 if there are no more characters.
     * @throws JSONException Thrown if there is an error reading the source string.
     */
    public char nextClean() throws JSONException {
        for (; ; ) {
            char c = this.next();
            if (c == 0 || c > ' ') {
                return c;
            }
        }
    }


    /**
     * Return the characters up to the next close quote character. Backslash processing is done. The formal JSON format
     * does not allow strings in single quotes, but an implementation is allowed to accept them.
     *
     * @param quote The quoting character, either
     *              <code>"</code>&nbsp;<small>(double quote)</small> or
     *              <code>'</code>&nbsp;<small>(single quote)</small>.
     * @return A String.
     * @throws JSONException Unterminated string.
     */
    public String nextString(char quote) throws JSONException {
        char c;
        StringBuilder sb = new StringBuilder();
        for (; ; ) {
            c = this.next();
            switch (c) {
                case 0:
                case '\n':
                case '\r':
                    throw this.syntaxError("Unterminated string");
                case '\\':
                    c = this.next();
                    switch (c) {
                        case 'b':
                            sb.append('\b');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 'u':
                            try {
                                sb.append((char) Integer.parseInt(this.next(4), 16));
                            } catch (NumberFormatException e) {
                                throw this.syntaxError("Illegal escape.", e);
                            }
                            break;
                        case '"':
                        case '\'':
                        case '\\':
                        case '/':
                            sb.append(c);
                            break;
                        default:
                            throw this.syntaxError("Illegal escape.");
                    }
                    break;
                default:
                    if (c == quote) {
                        return sb.toString();
                    }
                    sb.append(c);
            }
        }
    }


    /**
     * Get the text up but not including the specified character or the end of line, whichever comes first.
     *
     * @param delimiter A delimiter character.
     * @return A string.
     * @throws JSONException Thrown if there is an error while searching for the delimiter
     */
    public String nextTo(char delimiter) throws JSONException {
        StringBuilder sb = new StringBuilder();
        for (; ; ) {
            char c = this.next();
            if (c == delimiter || c == 0 || c == '\n' || c == '\r') {
                if (c != 0) {
                    this.back();
                }
                return sb.toString().trim();
            }
            sb.append(c);
        }
    }


    /**
     * Get the text up but not including one of the specified delimiter characters or the end of line, whichever comes
     * first.
     *
     * @param delimiters A set of delimiter characters.
     * @return A string, trimmed.
     * @throws JSONException Thrown if there is an error while searching for the delimiter
     */
    public String nextTo(String delimiters) throws JSONException {
        char c;
        StringBuilder sb = new StringBuilder();
        for (;;) {
            c = this.next();
            if (delimiters.indexOf(c) >= 0 || c == 0 ||
                c == '\n' || c == '\r') {
                if (c != 0) {
                    this.back();
                }
                return sb.toString().trim();
            }
            sb.append(c);
        }
    }


    /**
     * Get the next value. The value can be a Boolean, Double, Integer, JSONArray, JSONObject, Long, or String, or the
     * JSONObject.NULL object.
     *
     * @return An object.
     * @throws JSONException If syntax error.
     */
    public Object nextValue() throws JSONException {
        return nextValue(false);
    }

    /**
     * Get the next value. The value can be a Boolean, Double, Integer, JSONArray, JSONObject, Long, or String, or the
     * JSONObject.NULL object. The strictMode parameter controls the behavior of the method when parsing the value.
     *
     * @param strictMode If true, the method will strictly adhere to the JSON syntax, throwing a JSONException for any
     *                   deviations.
     * @return An object.
     * @throws JSONException If syntax error.
     */
    public Object nextValue(boolean strictMode) throws JSONException {
        char c = this.nextClean();
        switch (c) {
            case '{':
                this.back();
                return getJsonObject(strictMode);
            case '[':
                this.back();
                return getJsonArray();
            default:
                return getValue(c, strictMode);
        }
    }

    /**
     * This method is used to get the next value.
     *
     * @param c          The next character in the JSONTokener.
     * @param strictMode If true, the method will strictly adhere to the JSON syntax, throwing a JSONException if the
     *                   value is not surrounded by quotes.
     * @return An object which is the next value in the JSONTokener.
     * @throws JSONException If the value is not surrounded by quotes when strictMode is true.
     */
    private Object getValue(char c, boolean strictMode) {
        if (strictMode) {
            Object valueToValidate = nextSimpleValue(c, true);

            boolean isNumeric = valueToValidate.toString().chars().allMatch( Character::isDigit );

            if(isNumeric){
                return valueToValidate;
            }

            boolean hasQuotes = valueIsWrappedByQuotes(valueToValidate);

            if (!hasQuotes) {
                throw new JSONException("Value is not surrounded by quotes: " + valueToValidate);
            }

            return valueToValidate;
        }

        return nextSimpleValue(c);
    }

    /**
     * This method is used to get a JSONObject from the JSONTokener. The strictMode parameter controls the behavior of
     * the method when parsing the JSONObject.
     *
     * @param strictMode If true, the method will strictly adhere to the JSON syntax, throwing a JSONException for any
     *                   deviations.
     * @return A JSONObject which is the next value in the JSONTokener.
     * @throws JSONException If the JSONObject or JSONArray depth is too large to process.
     */
    private JSONObject getJsonObject(boolean strictMode) {
        try {
            if (strictMode) {
                return new JSONObject(this, new JSONParserConfiguration().withStrictMode(true));
            }

            return new JSONObject(this);
        } catch (StackOverflowError e) {
            throw new JSONException("JSON Array or Object depth too large to process.", e);
        }
    }

    /**
     * This method is used to get a JSONArray from the JSONTokener.
     *
     * @return A JSONArray which is the next value in the JSONTokener.
     * @throws JSONException If the JSONArray depth is too large to process.
     */
    private JSONArray getJsonArray() {
        try {
            return new JSONArray(this);
        } catch (StackOverflowError e) {
            throw new JSONException("JSON Array or Object depth too large to process.", e);
        }
    }

    /**
     * This method checks if the provided value is wrapped by quotes.
     *
     * @param valueToValidate The value to be checked. It is converted to a string before checking.
     * @return A boolean indicating whether the value is wrapped by quotes. It returns true if the value is wrapped by
     * either single or double quotes.
     */
    private boolean valueIsWrappedByQuotes(Object valueToValidate) {
        String stringToValidate = valueToValidate.toString();
        boolean isWrappedByDoubleQuotes = isWrappedByQuotes(stringToValidate, "\"");
        boolean isWrappedBySingleQuotes = isWrappedByQuotes(stringToValidate, "'");
        return isWrappedByDoubleQuotes || isWrappedBySingleQuotes;
    }

    private boolean isWrappedByQuotes(String valueToValidate, String quoteType) {
        return valueToValidate.startsWith(quoteType) && valueToValidate.endsWith(quoteType);
    }

    Object nextSimpleValue(char c) {
        return nextSimpleValue(c, false);
    }

    Object nextSimpleValue(char c, boolean strictMode) {
        if (c == '"' || c == '\'') {
            String str = this.nextString(c);
            if (strictMode) {
                return String.format("\"%s\"", str);
            }
            return str;
        }

        return parsedUnquotedText(c);
    }

    /**
     * Parses unquoted text from the JSON input. This could be the values true, false, or null, or it can be a number.
     * Non-standard forms are also accepted. Characters are accumulated until the end of the text or a formatting
     * character is reached.
     *
     * @param c The starting character.
     * @return The parsed object.
     * @throws JSONException If the parsed string is empty.
     */
    private Object parsedUnquotedText(char c) {
        StringBuilder sb = new StringBuilder();
        while (c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
            sb.append(c);
            c = this.next();
        }
        if (!this.eof) {
            this.back();
        }

        String string = sb.toString().trim();
        if (string.isEmpty()) {
            throw this.syntaxError("Missing value");
        }
        return JSONObject.stringToValue(string);
    }


    /**
     * Skip characters until the next character is the requested character. If the requested character is not found, no
     * characters are skipped.
     *
     * @param to A character to skip to.
     * @return The requested character, or zero if the requested character is not found.
     * @throws JSONException Thrown if there is an error while searching for the to character
     */
    public char skipTo(char to) throws JSONException {
        char c;
        try {
            long startIndex = this.index;
            long startCharacter = this.character;
            long startLine = this.line;
            this.reader.mark(1000000);
            do {
                c = this.next();
                if (c == 0) {
                    // in some readers, reset() may throw an exception if
                    // the remaining portion of the input is greater than
                    // the mark size (1,000,000 above).
                    this.reader.reset();
                    this.index = startIndex;
                    this.character = startCharacter;
                    this.line = startLine;
                    return 0;
                }
            } while (c != to);
            this.reader.mark(1);
        } catch (IOException exception) {
            throw new JSONException(exception);
        }
        this.back();
        return c;
    }

    /**
     * Make a JSONException to signal a syntax error.
     *
     * @param message The error message.
     * @return A JSONException object, suitable for throwing
     */
    public JSONException syntaxError(String message) {
        return new JSONException(message + this.toString());
    }

    /**
     * Make a JSONException to signal a syntax error.
     *
     * @param message  The error message.
     * @param causedBy The throwable that caused the error.
     * @return A JSONException object, suitable for throwing
     */
    public JSONException syntaxError(String message, Throwable causedBy) {
        return new JSONException(message + this.toString(), causedBy);
    }

    /**
     * Make a printable string of this JSONTokener.
     *
     * @return " at {index} [character {character} line {line}]"
     */
    @Override
    public String toString() {
        return " at " + this.index + " [character " + this.character + " line " +
            this.line + "]";
    }

    /**
     * Closes the underlying reader, releasing any resources associated with it.
     *
     * @throws IOException If an I/O error occurs while closing the reader.
     */
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
}
