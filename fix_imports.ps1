$files = @(
'C:\Users\braya\AndroidStudioProjects\AdministradorFarmadon-limpio-main\app\src\main\java\com\app\administradorfarmadon\inventario\ui\crear_producto\CreateProductCommonWidgets.kt',
'C:\Users\braya\AndroidStudioProjects\AdministradorFarmadon-limpio-main\app\src\main\java\com\app\administradorfarmadon\inventario\ui\crear_producto\CreateProductDateComponents.kt',
'C:\Users\braya\AndroidStudioProjects\AdministradorFarmadon-limpio-main\app\src\main\java\com\app\administradorfarmadon\inventario\ui\crear_producto\dialogs\CreateProductSupplierDialogs.kt',
'C:\Users\braya\AndroidStudioProjects\AdministradorFarmadon-limpio-main\app\src\main\java\com\app\administradorfarmadon\inventario\ui\crear_producto\pasos\compartidos\componentes\Step2FinanceSection.kt',
'C:\Users\braya\AndroidStudioProjects\AdministradorFarmadon-limpio-main\app\src\main\java\com\app\administradorfarmadon\inventario\ui\crear_producto\pasos\compartidos\componentes\CreateProductSupplierSection.kt',
'C:\Users\braya\AndroidStudioProjects\AdministradorFarmadon-limpio-main\app\src\main\java\com\app\administradorfarmadon\inventario\ui\crear_producto\pasos\paso1_producto\barcode\BarcodeAiDialogs.kt',
'C:\Users\braya\AndroidStudioProjects\AdministradorFarmadon-limpio-main\app\src\main\java\com\app\administradorfarmadon\inventario\ui\crear_producto\pasos\paso1_producto\barcode\BarcodeSkeletonCards.kt'
)

foreach($f in $files) {
    $c = Get-Content $f -Raw
    $c = $c -replace '(?m)^import com\.app\.administradorfarmadon\.inventario\.ui\.crear_producto\.compartidos\.diseno\.FDColors\.\w+\r?\n', ''
    $c = $c -replace '(?m)^import com\.app\.administradorfarmadon\.inventario\.ui\.crear_producto\.compartidos\.diseno\.Cyber\w+\r?\n', ''
    if ($c -notmatch 'import com\.app\.administradorfarmadon\.disenotemaapp\.ui\.FDColors') {
        $c = $c -replace '(package .+)', "`$1`n`nimport com.app.administradorfarmadon.disenotemaapp.ui.FDColors"
    }
    Set-Content $f -Value $c -NoNewline
    Write-Output "Fixed: $f"
}
