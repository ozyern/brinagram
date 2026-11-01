# Generates the alternate "Portrait" launcher icon from Tools/branding/brinagram_alt_icon_source.png
#   drawable-nodpi/brina_alt_background.png  full-bleed adaptive background (108dp @ xxxhdpi)
#   mipmap-*/brina_alt_launcher.png, brina_alt_launcher_round.png  legacy round icons
param(
    [string]$Source = "$PSScriptRoot\brinagram_alt_icon_source.png",
    [string]$Res = "$PSScriptRoot\..\..\TMessagesProj\src\main\res"
)
Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Bitmap]::FromFile((Resolve-Path $Source))
$side = [math]::Min($src.Width, $src.Height)
$sx = ($src.Width - $side) / 2; $sy = ($src.Height - $side) / 2

function Save-Square([int]$size, [string]$out, [bool]$circle, [double]$inset) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.SmoothingMode = 'AntiAlias'; $g.InterpolationMode = 'HighQualityBicubic'; $g.PixelOffsetMode = 'HighQuality'
    $d = $size * (1 - 2 * $inset); $o = ($size - $d) / 2
    $dest = New-Object System.Drawing.RectangleF ([float]$o), ([float]$o), ([float]$d), ([float]$d)
    if ($circle) {
        $brush = New-Object System.Drawing.TextureBrush $src
        $brush.WrapMode = 'Clamp'
        $m = New-Object System.Drawing.Drawing2D.Matrix
        $m.Translate([float]$o, [float]$o); $m.Scale([float]($d / $side), [float]($d / $side)); $m.Translate([float](-$sx), [float](-$sy))
        $brush.Transform = $m
        $g.FillEllipse($brush, $dest)
        $brush.Dispose()
    } else {
        $srcRect = New-Object System.Drawing.RectangleF ([float]$sx), ([float]$sy), ([float]$side), ([float]$side)
        $g.DrawImage($src, $dest, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
    }
    $g.Dispose(); $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
}

Save-Square 432 (Join-Path $Res 'drawable-nodpi\brina_alt_background.png') $false 0
$densities = @{ 'mdpi' = 1.0; 'hdpi' = 1.5; 'xhdpi' = 2.0; 'xxhdpi' = 3.0; 'xxxhdpi' = 4.0 }
foreach ($d in $densities.Keys) {
    $px = [int][math]::Round(48 * $densities[$d]); $dir = Join-Path $Res "mipmap-$d"
    Save-Square $px (Join-Path $dir 'brina_alt_launcher.png') $true (1 / 48)
    Save-Square $px (Join-Path $dir 'brina_alt_launcher_round.png') $true (1 / 48)
}
$src.Dispose()
Write-Output "alt icon generated"
