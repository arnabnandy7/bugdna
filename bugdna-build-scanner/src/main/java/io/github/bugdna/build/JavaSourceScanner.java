package io.github.bugdna.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class JavaSourceScanner {

    private static final String CATCH_KEYWORD = "catch";
    private static final String CATCH_KEYWORD_WITH_LEADING_SPACE = " " + CATCH_KEYWORD + " ";
    private static final String CATCH_KEYWORD_WITH_TRAILING_SPACE = CATCH_KEYWORD + " ";
    private static final String NEW_KEYWORD = "new";
    private static final String THROWS_KEYWORD = "throws";
    private static final List<String> CHECKED_EXCEPTION_CALLS = Arrays.asList(
            "Files.read", "Files.write", "Files.copy", "Files.move", "Files.delete",
            "Files.newInputStream", "Files.newOutputStream", "new FileInputStream",
            "new FileOutputStream", "new FileReader", "new FileWriter", "Thread.sleep",
            "Class.forName", ".wait(", ".join(", ".openStream(", ".openConnection("
    );

    private final Path file;

    JavaSourceScanner(Path file) {
        this.file = file;
    }

    List<BuildScanIssue> scan() throws IOException {
        List<String> rawLines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> lines = stripBlockComments(rawLines);
        List<BuildScanIssue> issues = new ArrayList<>();
        ScanState state = new ScanState();

        for (int index = 0; index < lines.size(); index++) {
            scanLine(lines, index, issues, state);
        }

        return issues;
    }

    private void scanLine(
            List<String> lines,
            int index,
            List<BuildScanIssue> issues,
            ScanState state
    ) {
        String code = stripLineComment(lines.get(index));
        String trimmed = code.trim();
        int lineNumber = index + 1;

        detectMethodStart(state, trimmed);
        detectGenericExceptionUsage(issues, lineNumber, trimmed);
        detectEmptyCatchBlock(issues, lines, index);
        detectUnhandledExceptionInMethod(issues, lineNumber, trimmed, state);
        updateMethodState(state, code);
        updateExceptionHandlingState(state, code, trimmed);
    }

    private void detectMethodStart(ScanState state, String trimmed) {
        if (state.method == null) {
            state.method = detectMethod(trimmed);
        }
    }

    private void detectUnhandledExceptionInMethod(
            List<BuildScanIssue> issues,
            int lineNumber,
            String trimmed,
            ScanState state
    ) {
        if (state.method != null) {
            detectUnhandledException(
                    issues,
                    lineNumber,
                    trimmed,
                    state.method,
                    state.isInsideExceptionHandling()
            );
        }
    }

    private void updateMethodState(ScanState state, String code) {
        if (state.method == null) {
            return;
        }
        if (code.indexOf('{') >= 0) {
            state.method.sawOpeningBrace = true;
        }
        state.method.depth += count(code, '{') - count(code, '}');
        if (state.method.depth <= 0 && state.method.sawOpeningBrace) {
            state.method = null;
        }
    }

    private void updateExceptionHandlingState(ScanState state, String code, String trimmed) {
        state.blockDepth += count(code, '{') - count(code, '}');
        if (isExceptionHandlingBlock(trimmed) && code.indexOf('{') >= 0) {
            state.exceptionHandlingDepth = Math.max(
                    state.exceptionHandlingDepth,
                    state.blockDepth
            );
        }
        if (state.exceptionHandlingDepth > 0
                && state.blockDepth < state.exceptionHandlingDepth) {
            state.exceptionHandlingDepth = 0;
        }
    }

    private MethodContext detectMethod(String trimmed) {
        if (trimmed.startsWith("if ") || trimmed.startsWith("for ") || trimmed.startsWith("while ")
                || trimmed.startsWith("switch ")
                || trimmed.startsWith(CATCH_KEYWORD_WITH_TRAILING_SPACE)) {
            return null;
        }
        if (containsJavaTypeDeclaration(trimmed) || !looksLikeMethodDeclaration(trimmed)) {
            return null;
        }
        MethodContext context = new MethodContext();
        context.declaresThrows = containsWord(trimmed, THROWS_KEYWORD);
        context.depth = 0;
        context.sawOpeningBrace = false;
        return context;
    }

    private void detectGenericExceptionUsage(List<BuildScanIssue> issues, int line, String trimmed) {
        List<String> catchDeclarations = catchDeclarations(trimmed);
        for (String declaration : catchDeclarations) {
            if (usesGenericException(declaration)) {
                issues.add(issue(
                        BuildScanRule.GENERIC_EXCEPTION_USAGE,
                        BuildScanSeverity.WARNING,
                        line,
                        "Avoid catching generic Exception, Throwable, or RuntimeException.",
                        trimmed
                ));
            }
        }
        if (declaresGenericException(trimmed) || createsGenericException(trimmed)) {
            issues.add(issue(
                    BuildScanRule.GENERIC_EXCEPTION_USAGE,
                    BuildScanSeverity.WARNING,
                    line,
                    "Use a specific exception type instead of generic Exception, Throwable, or RuntimeException.",
                    trimmed
            ));
        }
    }

    private void detectEmptyCatchBlock(List<BuildScanIssue> issues, List<String> lines, int index) {
        String trimmed = stripLineComment(lines.get(index)).trim();
        if (!hasCatchClause(trimmed)) {
            return;
        }

        int openLine = findOpeningBraceLine(lines, index);
        if (openLine < 0) {
            return;
        }

        CatchBody body = readCatchBody(lines, openLine);
        if (body.isClosed() && body.isEmpty()) {
            issues.add(issue(
                    BuildScanRule.EMPTY_CATCH_BLOCK,
                    BuildScanSeverity.WARNING,
                    index + 1,
                    "Catch block is empty; log, rethrow, or document intentional suppression.",
                    stripLineComment(lines.get(index)).trim()
            ));
        }
    }

    private int findOpeningBraceLine(List<String> lines, int startLine) {
        int lineIndex = startLine;
        String trimmed = stripLineComment(lines.get(lineIndex)).trim();
        while (trimmed.indexOf('{') < 0 && lineIndex + 1 < lines.size()) {
            lineIndex++;
            trimmed = stripLineComment(lines.get(lineIndex)).trim();
        }
        return trimmed.indexOf('{') >= 0 ? lineIndex : -1;
    }

    private CatchBody readCatchBody(List<String> lines, int openLine) {
        CatchBody body = new CatchBody();
        for (int i = openLine; i < lines.size(); i++) {
            String code = stripLineComment(lines.get(i));
            int start = i == openLine ? code.indexOf('{') : 0;
            for (int j = Math.max(start, 0); j < code.length(); j++) {
                body.accept(code.charAt(j));
                if (body.isClosed()) {
                    return body;
                }
            }
        }
        return body;
    }

    private void detectUnhandledException(
            List<BuildScanIssue> issues,
            int line,
            String trimmed,
            MethodContext method,
            boolean insideExceptionHandling
    ) {
        if (method.declaresThrows || insideExceptionHandling || trimmed.startsWith("//")) {
            return;
        }
        for (String call : CHECKED_EXCEPTION_CALLS) {
            if (trimmed.contains(call)) {
                issues.add(issue(
                        BuildScanRule.UNHANDLED_EXCEPTION,
                        BuildScanSeverity.WARNING,
                        line,
                        "Checked-exception API appears outside try/catch or a method with throws.",
                        trimmed
                ));
                return;
            }
        }
    }

    private boolean usesGenericException(String declaration) {
        return containsExceptionType(declaration);
    }

    private List<String> catchDeclarations(String value) {
        List<String> declarations = new ArrayList<>();
        int searchFrom = 0;
        int catchIndex = catchIndex(value, searchFrom);
        while (catchIndex >= 0) {
            int openParenthesis = nextNonWhitespace(value, catchIndex + CATCH_KEYWORD.length());
            int closeParenthesis = matchingParenthesis(value, openParenthesis);
            if (closeParenthesis >= 0) {
                declarations.add(value.substring(openParenthesis + 1, closeParenthesis));
                searchFrom = closeParenthesis + 1;
            } else {
                searchFrom = catchIndex + CATCH_KEYWORD.length();
            }
            catchIndex = catchIndex(value, searchFrom);
        }
        return declarations;
    }

    private boolean hasCatchClause(String value) {
        return catchIndex(value, 0) >= 0;
    }

    private int catchIndex(String value, int start) {
        int index = value.indexOf(CATCH_KEYWORD, start);
        while (index >= 0) {
            if (isWordAt(value, index, CATCH_KEYWORD)) {
                int next = nextNonWhitespace(value, index + CATCH_KEYWORD.length());
                if (next < value.length() && value.charAt(next) == '(') {
                    return index;
                }
            }
            index = value.indexOf(CATCH_KEYWORD, index + CATCH_KEYWORD.length());
        }
        return -1;
    }

    private int nextNonWhitespace(String value, int start) {
        int cursor = start;
        while (cursor < value.length() && Character.isWhitespace(value.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private int matchingParenthesis(String value, int openParenthesis) {
        if (openParenthesis < 0
                || openParenthesis >= value.length()
                || value.charAt(openParenthesis) != '(') {
            return -1;
        }
        int depth = 0;
        for (int index = openParenthesis; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '(') {
                depth++;
            } else if (character == ')') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private boolean declaresGenericException(String trimmed) {
        int throwsIndex = trimmed.indexOf(THROWS_KEYWORD);
        if (throwsIndex < 0 || !isWordAt(trimmed, throwsIndex, THROWS_KEYWORD)) {
            return false;
        }
        int end = firstIndexOf(trimmed, throwsIndex, ';', '{');
        String declaration = end >= 0
                ? trimmed.substring(throwsIndex + THROWS_KEYWORD.length(), end)
                : trimmed.substring(throwsIndex + THROWS_KEYWORD.length());
        return containsExceptionType(declaration);
    }

    private boolean createsGenericException(String trimmed) {
        return containsWord(trimmed, NEW_KEYWORD)
                && (containsTypeConstruction(trimmed, "Exception")
                || containsTypeConstruction(trimmed, "Throwable")
                || containsTypeConstruction(trimmed, "RuntimeException"));
    }

    private boolean containsTypeConstruction(String value, String type) {
        int index = value.indexOf(type);
        while (index >= 0) {
            int cursor = index + type.length();
            while (cursor < value.length() && Character.isWhitespace(value.charAt(cursor))) {
                cursor++;
            }
            if (isWordAt(value, index, type) && cursor < value.length() && value.charAt(cursor) == '(') {
                return true;
            }
            index = value.indexOf(type, index + type.length());
        }
        return false;
    }

    private boolean containsExceptionType(String value) {
        return containsWord(value, "Exception")
                || containsWord(value, "Throwable")
                || containsWord(value, "RuntimeException");
    }

    private boolean containsJavaTypeDeclaration(String value) {
        return containsWord(value, "class")
                || containsWord(value, "interface")
                || containsWord(value, "enum")
                || containsWord(value, "record");
    }

    private boolean looksLikeMethodDeclaration(String value) {
        int openParenthesis = value.indexOf('(');
        int closeParenthesis = value.indexOf(')', openParenthesis + 1);
        if (openParenthesis <= 0 || closeParenthesis < 0 || value.indexOf(';') >= 0) {
            return false;
        }

        String beforeParameters = value.substring(0, openParenthesis).trim();
        int methodNameStart = lastIdentifierStart(beforeParameters);
        if (methodNameStart < 0) {
            return false;
        }

        String returnAndModifiers = beforeParameters.substring(0, methodNameStart).trim();
        return !returnAndModifiers.isEmpty();
    }

    private int lastIdentifierStart(String value) {
        int cursor = value.length() - 1;
        while (cursor >= 0 && Character.isWhitespace(value.charAt(cursor))) {
            cursor--;
        }
        if (cursor < 0 || !Character.isJavaIdentifierPart(value.charAt(cursor))) {
            return -1;
        }
        while (cursor >= 0 && Character.isJavaIdentifierPart(value.charAt(cursor))) {
            cursor--;
        }
        return cursor + 1;
    }

    private boolean containsWord(String value, String word) {
        int index = value.indexOf(word);
        while (index >= 0) {
            if (isWordAt(value, index, word)) {
                return true;
            }
            index = value.indexOf(word, index + word.length());
        }
        return false;
    }

    private boolean isWordAt(String value, int index, String word) {
        if (index < 0 || index + word.length() > value.length()) {
            return false;
        }
        if (!value.regionMatches(index, word, 0, word.length())) {
            return false;
        }
        boolean startsCleanly = index == 0 || !Character.isJavaIdentifierPart(value.charAt(index - 1));
        int end = index + word.length();
        boolean endsCleanly = end == value.length() || !Character.isJavaIdentifierPart(value.charAt(end));
        return startsCleanly && endsCleanly;
    }

    private int firstIndexOf(String value, int start, char first, char second) {
        int firstIndex = value.indexOf(first, start);
        int secondIndex = value.indexOf(second, start);
        if (firstIndex < 0) {
            return secondIndex;
        }
        if (secondIndex < 0) {
            return firstIndex;
        }
        return Math.min(firstIndex, secondIndex);
    }

    private boolean isExceptionHandlingBlock(String trimmed) {
        return trimmed.startsWith("try")
                || trimmed.contains(" try ")
                || trimmed.startsWith(CATCH_KEYWORD)
                || trimmed.contains(CATCH_KEYWORD_WITH_LEADING_SPACE);
    }

    private BuildScanIssue issue(
            BuildScanRule rule,
            BuildScanSeverity severity,
            int line,
            String message,
            String snippet
    ) {
        return new BuildScanIssue(rule, severity, file, line, message, snippet);
    }

    private static List<String> stripBlockComments(List<String> lines) {
        List<String> stripped = new ArrayList<>(lines.size());
        BlockCommentState state = new BlockCommentState();
        for (String line : lines) {
            StringBuilder output = new StringBuilder();
            int cursor = 0;
            while (cursor < line.length()) {
                BlockCommentAction action = state.handle(line, cursor);
                if (action.emit) {
                    output.append(line.charAt(cursor));
                }
                cursor += action.consumedCharacters;
            }
            stripped.add(output.toString());
        }
        return stripped;
    }

    private static String stripLineComment(String line) {
        int comment = line.indexOf("//");
        return comment >= 0 ? line.substring(0, comment) : line;
    }

    private static int count(String value, char expected) {
        int total = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == expected) {
                total++;
            }
        }
        return total;
    }

    private static final class MethodContext {
        private int depth;
        private boolean declaresThrows;
        private boolean sawOpeningBrace;
    }

    private static final class CatchBody {
        private final StringBuilder content = new StringBuilder();
        private int depth;
        private boolean inside;
        private boolean closed;

        private void accept(char character) {
            if (closed) {
                return;
            }
            if (character == '{') {
                depth++;
                inside = true;
                return;
            }
            if (character == '}') {
                closeScope();
                return;
            }
            appendContent(character);
        }

        private void closeScope() {
            depth--;
            if (depth == 0) {
                closed = true;
            }
        }

        private void appendContent(char character) {
            if (inside && depth > 0) {
                content.append(character);
            }
        }

        private boolean isClosed() {
            return closed;
        }

        private boolean isEmpty() {
            return content.toString().trim().isEmpty();
        }
    }

    private static final class BlockCommentState {
        private boolean inBlock;

        private BlockCommentAction handle(String line, int index) {
            if (inBlock) {
                return handleInsideBlock(line, index);
            }
            return handleOutsideBlock(line, index);
        }

        private BlockCommentAction handleInsideBlock(String line, int index) {
            if (hasPair(line, index, '*', '/')) {
                inBlock = false;
                return BlockCommentAction.skipPair();
            }
            return BlockCommentAction.skipCharacter();
        }

        private BlockCommentAction handleOutsideBlock(String line, int index) {
            if (hasPair(line, index, '/', '*')) {
                inBlock = true;
                return BlockCommentAction.skipPair();
            }
            return BlockCommentAction.emitCharacter();
        }

        private boolean hasPair(String line, int index, char first, char second) {
            return index + 1 < line.length()
                    && line.charAt(index) == first
                    && line.charAt(index + 1) == second;
        }
    }

    private static final class BlockCommentAction {
        private final boolean emit;
        private final int consumedCharacters;

        private BlockCommentAction(boolean emit, int consumedCharacters) {
            this.emit = emit;
            this.consumedCharacters = consumedCharacters;
        }

        private static BlockCommentAction emitCharacter() {
            return new BlockCommentAction(true, 1);
        }

        private static BlockCommentAction skipCharacter() {
            return new BlockCommentAction(false, 1);
        }

        private static BlockCommentAction skipPair() {
            return new BlockCommentAction(false, 2);
        }
    }

    private static final class ScanState {
        private MethodContext method;
        private int blockDepth;
        private int exceptionHandlingDepth;

        private boolean isInsideExceptionHandling() {
            return exceptionHandlingDepth > 0 && blockDepth >= exceptionHandlingDepth;
        }
    }
}
