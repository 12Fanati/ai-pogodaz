package com.example.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun getGlassTextStyle(
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    isSecondary: Boolean = false
): TextStyle {
    // Detect dark status based on standard Slate background color or theme selection
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF080B1A)
    
    val colors = if (isDark) {
        if (isSecondary) {
            listOf(
                Color(0xFFE2E8F0).copy(alpha = 0.82f),
                Color(0xFF94A3B8).copy(alpha = 0.75f),
                Color(0xFFCBD5E1).copy(alpha = 0.60f)
            )
        } else {
            listOf(
                Color(0xFFFAFAFA),
                Color(0xFFF1F5F9),
                Color(0xFF93C5FD) // Translucent Frosty Sky Blue bottom tint
            )
        }
    } else {
        if (isSecondary) {
            listOf(
                Color(0xFF1E293B).copy(alpha = 0.85f),
                Color(0xFF475569).copy(alpha = 0.75f),
                Color(0xFF64748B).copy(alpha = 0.68f)
            )
        } else {
            listOf(
                Color(0xFF0F172A), // Crisp slate charcoal top
                Color(0xFF334155),
                Color(0xFF6366F1) // Subtle rich indigo/violet bottom tint
            )
        }
    }
    
    val shadowColor = if (isDark) {
        if (isSecondary) Color(0xFF38BDF8).copy(alpha = 0.30f) else Color(0xFF6366F1).copy(alpha = 0.48f)
    } else {
        if (isSecondary) Color(0xFF6366F1).copy(alpha = 0.18f) else Color(0xFF0EA5E9).copy(alpha = 0.32f)
    }

    return TextStyle(
        fontSize = fontSize,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        textAlign = textAlign ?: TextAlign.Start,
        brush = Brush.verticalGradient(
            colors = colors
        ),
        shadow = Shadow(
            color = shadowColor,
            offset = Offset(1.5f, 2.0f),
            blurRadius = if (isSecondary) 3f else 4f
        )
    )
}

@Composable
fun GlassText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    Text(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        style = getGlassTextStyle(fontSize, fontWeight, letterSpacing, textAlign, isSecondary = false)
    )
}

@Composable
fun GlassTextMuted(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    Text(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        style = getGlassTextStyle(fontSize, fontWeight, letterSpacing, textAlign, isSecondary = true)
    )
}
