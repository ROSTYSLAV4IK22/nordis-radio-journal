package com.nordisapps.nordisradiojournal.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val AndroidCell4Bar: ImageVector
    get() {
        if (android_cell_4_bar != null) {
            return android_cell_4_bar!!
        }
        android_cell_4_bar =
            ImageVector.Builder(
                name = "android_cell_4_bar",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(1.5f, 20f)
                        verticalLineTo(12f)
                        horizontalLineToRelative(3f)
                        verticalLineToRelative(8f)
                        horizontalLineToRelative(-3f)
                        close()
                        moveToRelative(6f, 0f)
                        verticalLineTo(9.5f)
                        horizontalLineToRelative(3f)
                        verticalLineTo(20f)
                        horizontalLineToRelative(-3f)
                        close()
                        moveToRelative(6f, 0f)
                        verticalLineTo(7f)
                        horizontalLineToRelative(3f)
                        verticalLineTo(20f)
                        horizontalLineToRelative(-3f)
                        close()
                        moveToRelative(6f, 0f)
                        verticalLineTo(4f)
                        horizontalLineToRelative(3f)
                        verticalLineTo(20f)
                        horizontalLineToRelative(-3f)
                        close()
                    }
                }
                .build()
        return android_cell_4_bar!!
    }

private var android_cell_4_bar: ImageVector? = null