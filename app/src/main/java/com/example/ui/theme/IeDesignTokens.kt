package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.ValueClassification

// =============================================================================
// STITCH "PRECISION INDUSTRIAL OPERATIONS" DESIGN TOKENS
// =============================================================================

object IeSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    val screenPadding = 16.dp
    val cardPadding = 14.dp
    val compactPadding = 8.dp
    val tableRowPaddingVertical = 8.dp
    val tableRowPaddingHorizontal = 12.dp
}

object IeRadius {
    val none = 0.dp
    val xs = 2.dp
    val sm = 4.dp
    val md = 6.dp
    val lg = 8.dp
    val pill = 999.dp

    val cardShape = RoundedCornerShape(md)
    val buttonShape = RoundedCornerShape(md)
    val inputShape = RoundedCornerShape(md)
    val badgeShape = RoundedCornerShape(sm)
    val pillShape = RoundedCornerShape(pill)
    val tableShape = RoundedCornerShape(md)
    val dialogShape = RoundedCornerShape(lg)
}

object IeBorders {
    val thin = 1.dp
    val medium = 1.5.dp
    val focus = 2.dp

    val cardBorder = BorderStroke(thin, StitchSlate200)
    val activeBorder = BorderStroke(thin, StitchCobalt600)
    val subtleBorder = BorderStroke(thin, StitchSlate100)
    val dividerBorder = BorderStroke(thin, StitchSlate200)
    val inputBorder = BorderStroke(thin, StitchSlate300)
    val errorBorder = BorderStroke(thin, StitchNvaRed)
}

object IeElevation {
    val none = 0.dp
    val flat = 0.dp
    val subtle = 1.dp
    val modal = 4.dp
}

// =============================================================================
// UNIFIED LEAN CLASSIFICATION TOKENS (VA, NNVA, NVA, TAKT)
// =============================================================================

object IeLeanTokens {
    // Value Added (Emerald)
    val vaColor = StitchVaGreen
    val vaDark = StitchVaGreenDark
    val vaContainer = StitchVaGreenLight
    val vaText = StitchVaGreenText

    // Necessary Non-Value Added (Amber)
    val nnvaColor = StitchNnvaAmber
    val nnvaDark = StitchNnvaAmberDark
    val nnvaContainer = StitchNnvaAmberLight
    val nnvaText = StitchNnvaAmberText

    // Non-Value Added / Waste (Rose/Red)
    val nvaColor = StitchNvaRed
    val nvaDark = StitchNvaRedDark
    val nvaContainer = StitchNvaRedLight
    val nvaText = StitchNvaRedText

    // Takt Time & Target Lines
    val taktColor = StitchTaktLineIndigo
    val taktContainer = StitchTaktLineLight
    val taktText = StitchTaktLineText

    fun getColorForValueClassification(vc: ValueClassification): Color {
        return when (vc) {
            ValueClassification.VA -> vaColor
            ValueClassification.NNVA -> nnvaColor
            ValueClassification.NVA -> nvaColor
        }
    }

    fun getContainerForValueClassification(vc: ValueClassification): Color {
        return when (vc) {
            ValueClassification.VA -> vaContainer
            ValueClassification.NNVA -> nnvaContainer
            ValueClassification.NVA -> nvaContainer
        }
    }

    fun getTextForValueClassification(vc: ValueClassification): Color {
        return when (vc) {
            ValueClassification.VA -> vaText
            ValueClassification.NNVA -> nnvaText
            ValueClassification.NVA -> nvaText
        }
    }
}
