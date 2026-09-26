package com.mitaoe.shridhar202401040197.util;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownUtil {

    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)");
    private static final Pattern CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern HEADER_PATTERN = Pattern.compile("(?m)^#{1,3}\\s+(.*)$");
    private static final Pattern BULLET_PATTERN = Pattern.compile("(?m)^[\\*\\-]\\s+(.*)$");

    public static CharSequence renderMarkdown(String rawText, int codeColor) {
        if (rawText == null || rawText.isEmpty()) return "";

        // First process lines for headers and bullets
        String[] lines = rawText.split("\n");
        StringBuilder processedLines = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher bulletMatcher = BULLET_PATTERN.matcher(line);
            if (bulletMatcher.matches()) {
                line = "  •  " + bulletMatcher.group(1);
            }
            processedLines.append(line);
            if (i < lines.length - 1) {
                processedLines.append("\n");
            }
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder(processedLines.toString());

        // Process Headers
        Matcher headerMatcher = HEADER_PATTERN.matcher(ssb);
        while (headerMatcher.find()) {
            int start = headerMatcher.start();
            int end = headerMatcher.end();
            ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new RelativeSizeSpan(1.15f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Process Bold (**text**)
        Matcher boldMatcher = BOLD_PATTERN.matcher(ssb.toString());
        while (boldMatcher.find()) {
            int start = boldMatcher.start();
            int end = boldMatcher.end();
            String content = boldMatcher.group(1);

            ssb.replace(start, end, content);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), start, start + content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            boldMatcher = BOLD_PATTERN.matcher(ssb.toString());
        }

        // Process Inline Code (`code`)
        Matcher codeMatcher = CODE_PATTERN.matcher(ssb.toString());
        while (codeMatcher.find()) {
            int start = codeMatcher.start();
            int end = codeMatcher.end();
            String content = codeMatcher.group(1);

            ssb.replace(start, end, content);
            ssb.setSpan(new TypefaceSpan("monospace"), start, start + content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (codeColor != 0) {
                ssb.setSpan(new ForegroundColorSpan(codeColor), start, start + content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            codeMatcher = CODE_PATTERN.matcher(ssb.toString());
        }

        // Process Italic (*text*)
        Matcher italicMatcher = ITALIC_PATTERN.matcher(ssb.toString());
        while (italicMatcher.find()) {
            int start = italicMatcher.start();
            int end = italicMatcher.end();
            String content = italicMatcher.group(1);

            ssb.replace(start, end, content);
            ssb.setSpan(new StyleSpan(Typeface.ITALIC), start, start + content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            italicMatcher = ITALIC_PATTERN.matcher(ssb.toString());
        }

        return ssb;
    }
}
