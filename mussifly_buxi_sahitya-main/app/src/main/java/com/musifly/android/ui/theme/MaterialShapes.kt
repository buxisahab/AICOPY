package com.musifly.android.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object MaterialShapes {
    val Circle = RoundedPolygon.circle()
    val Square = RoundedPolygon.rectangle()
    val Pill = RoundedPolygon.pill()
    
    // Approximations for missing shapes
    val Diamond = RoundedPolygon.star(4, innerRadius = 0.6f)
    val Pentagon = RoundedPolygon.star(5, innerRadius = 0.8f) // Polygon-like
    val Heart = RoundedPolygon.star(5, innerRadius = 0.5f) // Placeholder
    val Triangle = RoundedPolygon.star(3)
    
    // Mapping others to generic shapes for now to prevent build errors
    val Oval = Circle
    val Arch = Circle
    val SemiCircle = Circle
    val Arrow = Triangle
    val Fan = Circle
    val Gem = Diamond
    val Bun = Pill
    val Ghostish = Circle
    val Cookie4Sided = RoundedPolygon.star(4, innerRadius = 0.7f, rounding = CornerRounding(0.2f))
    val Cookie6Sided = RoundedPolygon.star(6, innerRadius = 0.8f, rounding = CornerRounding(0.2f))
    val Cookie7Sided = RoundedPolygon.star(7, innerRadius = 0.8f, rounding = CornerRounding(0.2f))
    val Cookie9Sided = RoundedPolygon.star(9, innerRadius = 0.85f, rounding = CornerRounding(0.2f))
    val Cookie12Sided = RoundedPolygon.star(12, innerRadius = 0.9f, rounding = CornerRounding(0.2f))
    val Clover4Leaf = RoundedPolygon.star(4, innerRadius = 0.5f, rounding = CornerRounding(0.5f))
    val Clover8Leaf = RoundedPolygon.star(8, innerRadius = 0.6f, rounding = CornerRounding(0.5f))
    val Sunny = RoundedPolygon.star(8, innerRadius = 0.8f)
    val VerySunny = RoundedPolygon.star(12, innerRadius = 0.8f)
    val Burst = RoundedPolygon.star(16, innerRadius = 0.7f)
    val SoftBurst = RoundedPolygon.star(16, innerRadius = 0.7f, rounding = CornerRounding(0.5f))
    val Boom = RoundedPolygon.star(10, innerRadius = 0.4f)
    val SoftBoom = RoundedPolygon.star(10, innerRadius = 0.4f, rounding = CornerRounding(0.5f))
    val Flower = RoundedPolygon.star(8, innerRadius = 0.6f, rounding = CornerRounding(0.8f))
    val PixelCircle = Circle
    val PixelTriangle = Triangle
    val Puffy = Circle
    val PuffyDiamond = Diamond
    val Slanted = Square
    val ClamShell = Circle
}

fun RoundedPolygon.toShape(): Shape {
    return object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            val path = this@toShape.toPath().asComposePath()
            // Scale path to fit size
            val bounds = path.getBounds()
            val scaleX = size.width / bounds.width
            val scaleY = size.height / bounds.height
            
            // This is a naive implementation, real toShape handles bounds better
            // But RoundedPolygons are usually normalized to 0..1 or center based.
            // Check androidx.graphics.shapes implementation if possible.
            // The default RoundedPolygon is usually unit circle/square based? 
            // Actually, created from libraries they might be around (0,0) with radius 1.
            
            // We need to transform the path to fit the box (0,0) - (w,h)
            
            val matrix = androidx.compose.ui.graphics.Matrix()
            // Reset to 0,0
            matrix.translate(-bounds.left, -bounds.top) 
            matrix.scale(scaleX, scaleY)
            
            path.transform(matrix)
            return Outline.Generic(path)
        }
    }
}
