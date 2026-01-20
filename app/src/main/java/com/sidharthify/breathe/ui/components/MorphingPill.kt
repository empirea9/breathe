package com.sidharthify.breathe.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingPill(
    isSelected: Boolean,
    from: RoundedPolygon,
    to: RoundedPolygon,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val morph = remember(from, to) {
        Morph(from.normalized(), to.normalized())
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            progress.snapTo(0f)
            kotlinx.coroutines.delay(120L)
            progress.animateTo(
                1f,
                animationSpec = tween(300)
            )
        } else {
            progress.snapTo(0f)
        }
    }

    val path = remember { Path() }
    val matrix = remember { Matrix() }

    Box(
        modifier = modifier
            .drawWithContent {
                val p = morph.toPath(
                    progress = progress.value,
                    path = path,
                    startAngle = 0
                )

                processPath(
                    path = p,
                    size = size,
                    scaleFactor = 1f,
                    scaleMatrix = matrix
                )

                drawPath(path, color = color)
            }
    )
}

private fun processPath(
    path: Path,
    size: Size,
    scaleFactor: Float,
    scaleMatrix: Matrix = Matrix(),
): Path {
    scaleMatrix.reset()
    scaleMatrix.apply { scale(x = size.width * scaleFactor, y = size.height * scaleFactor) }
    path.transform(scaleMatrix)
    path.translate(size.center - path.getBounds().center)
    return path
}
