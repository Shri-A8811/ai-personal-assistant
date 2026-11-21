package com.mitaoe.shridhar202401040197.util;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownUtil {

    private static final Pattern FENCED_CODE_PATTERN = Pattern.compile("```(?:[a-zA-Z0-9_+#.-]+)?\\n?([\\s\\S]*?)```");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*([^*\\n]+)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)([^*\\n]+)(?<!\\*)\\*(?!\\*)");
    private static final Pattern CODE_PATTERN = Pattern.compile("`([^`\\n]+)`");
    private static final Pattern HEADER_PATTERN = Pattern.compile("^#{1,3}\\s+(.*)$");
    private static final Pattern BULLET_PATTERN = Pattern.compile("^[\\*\\-]\\s+(.*)$");
    private static final int CODE_BG_COLOR = 0x3338BDF8;

    public static CharSequence renderMarkdown(String rawText, int codeColor) {
        if (rawText == null || rawText.isEmpty()) return "";

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        String[] lines = rawText.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineStart = ssb.length();

            Matcher headerMatcher = HEADER_PATTERN.matcher(line);
            if (headerMatcher.matches()) {
                String heading = headerMatcher.group(1);
                ssb.append(heading);
                int lineEnd = ssb.length();
                ssb.setSpan(new StyleSpan(Typeface.BOLD), lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new RelativeSizeSpan(1.15f), lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                Matcher bulletMatcher = BULLET_PATTERN.matcher(line);
                if (bulletMatcher.matches()) {
                    ssb.append("  •  ").append(bulletMatcher.group(1));
                } else {
                    ssb.append(line);
                }
            }
            if (i < lines.length - 1) {
                ssb.append("\n");
            }
        }

        // Single-pass bounded replacements by adjusting offset cleanly
        applyRegexSpan(ssb, FENCED_CODE_PATTERN, codeColor, true, false, false);
        applyRegexSpan(ssb, INLINE_BOLD_HELPER, codeColor, false, true, false);
        applyRegexSpan(ssb, CODE_PATTERN, codeColor, false, false, true);
        applyRegexSpan(ssb, ITALIC_PATTERN, codeColor, false, false, false);

        return ssb;
    }

    private static final Pattern INLINE_BOLD_HELPER = BOLD_PATTERN;

    private static void applyRegexSpan(
            SpannableStringBuilder ssb,
            Pattern pattern,
            int codeColor,
            boolean isCodeBlock,
            boolean isBold,
            boolean isInlineCode
    ) {
        String snapshot = ssb.toString();
        Matcher matcher = pattern.matcher(snapshot);
        int offset = 0;
        while (matcher.find()) {
            String inner = matcher.group(1);
            if (inner == null) continue;
            int origStart = matcher.start();
            int origEnd = matcher.end();
            int s = origStart + offset;
            int e = origEnd + offset;
            if (s < 0 || e > ssb.length() || s >= e) break;

            String replacement = isCodeBlock ? inner.trim() : inner;
            ssb.replace(s, e, replacement);
            int spanEnd = s + replacement.length();
            offset += replacement.length() - (origEnd - origStart);

            if (isCodeBlock || isInlineCode) {
                ssb.setSpan(new TypefaceSpan("monospace"), s, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new BackgroundColorSpan(CODE_BG_COLOR), s, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (codeColor != 0) {
                    ssb.setSpan(new ForegroundColorSpan(codeColor), s, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else if (isBold) {
                ssb.setSpan(new StyleSpan(Typeface.BOLD), s, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                ssb.setSpan(new StyleSpan(Typeface.ITALIC), s, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }
}
