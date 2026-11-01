# Generates BrinaGram launcher icons from Tools/branding/brinagram_icon_source.png
#   mipmap-*/ic_launcher.png, ic_launcher_round.png   legacy icons (pre Android 8)
#   mipmap-*/brina_icon_foreground.png                 adaptive icon foreground (108dp layer)
# Usage: powershell -ExecutionPolicy Bypass -File Tools/branding/generate_icons.ps1
param(
    [string]$Source = "$PSScriptRoot\brinagram_icon_source.png",
    [string]$Res = "$PSScriptRoot\..\..\TMessagesProj\src\main\res",
    [double]$CenterX = 627, [double]$CenterY = 618, [double]$Radius = 615
)
Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Bitmap]::FromFile((Resolve-Path $Source))

function New-Medallion([int]$canvas, [double]$diameter, [string]$out) {
    $bmp = New-Object System.Drawing.Bitmap $canvas, $canvas, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.SmoothingMode = 'AntiAlias'; $g.InterpolationMode = 'HighQualityBicubic'; $g.PixelOffsetMode = 'HighQuality'; $g.CompositingQuality = 'HighQuality'
    $off = ($canvas - $diameter) / 2
    $scale = $diameter / (2 * $Radius)
    $brush = New-Object System.Drawing.TextureBrush $src
    $brush.WrapMode = 'Clamp'
    $m = New-Object System.Drawing.Drawing2D.Matrix
    $m.Translate([float]($off), [float]($off))
    $m.Scale([float]$scale, [float]$scale)
    $m.Translate([float](-($CenterX - $Radius)), [float](-($CenterY - $Radius)))
    $brush.Transform = $m
    $g.FillEllipse($brush, [float]$off, [float]$off, [float]$diameter, [float]$diameter)
    $g.Dispose(); $brush.Dispose()
    $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
}

$densities = @{ 'mdpi' = 1.0; 'hdpi' = 1.5; 'xhdpi' = 2.0; 'xxhdpi' = 3.0; 'xxxhdpi' = 4.0 }
foreach ($d in $densities.Keys) {
    $s = $densities[$d]; $dir = Join-Path $Res "mipmap-$d"
    $legacy = [int][math]::Round(48 * $s)
    New-Medallion $legacy ($legacy * 46 / 48) (Join-Path $dir 'ic_launcher.png')
    New-Medallion $legacy ($legacy * 46 / 48) (Join-Path $dir 'ic_launcher_round.png')
    # Adaptive: 108dp layer, launcher masks show the central 72dp. Slightly overscan the mask so the gold rim fills it.
    $layer = [int][math]::Round(108 * $s)
    New-Medallion $layer ($layer * 74 / 108) (Join-Path $dir 'brina_icon_foreground.png')
    Write-Output "mipmap-$d done ($legacy / $layer px)"
}
$src.Dispose()

# Splash / launch medallion (drawable-nodpi, sized for 160dp at xxxhdpi)
$src = [System.Drawing.Bitmap]::FromFile((Resolve-Path $Source))
New-Medallion 640 640 (Join-Path $Res 'drawable-nodpi\brina_medallion.png')
$src.Dispose()
Write-Output "drawable-nodpi/brina_medallion.png done"
