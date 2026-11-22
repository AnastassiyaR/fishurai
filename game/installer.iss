; Inno Setup script for FISHURAI.
; Defines can be overridden from the command line:
;   ISCC.exe /DAppVersion=1.0.0 /DSourceDir=dist /DAppExe=fishurai.exe /DIconFile=logo.ico installer.iss

#ifndef AppVersion
  #define AppVersion "1.0.0"
#endif
#ifndef SourceDir
  #define SourceDir "dist"
#endif
#ifndef AppExe
  #define AppExe "fishurai.exe"
#endif
#ifndef IconFile
  #define IconFile "icons\logo.ico"
#endif

#define AppName "FISHURAI"

[Setup]
AppId={{B7C1E5A2-4F0D-4B6E-9A53-7D2F8C1A9E10}
AppName={#AppName}
AppVersion={#AppVersion}
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
OutputDir=out
OutputBaseFilename=FISHURAI-{#AppVersion}-windows-setup
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
SetupIconFile={#IconFile}
UninstallDisplayIcon={app}\logo.ico

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion
Source: "{#IconFile}"; DestDir: "{app}"; DestName: "logo.ico"; Flags: ignoreversion

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExe}"; IconFilename: "{app}\logo.ico"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; IconFilename: "{app}\logo.ico"

[Run]
Filename: "{app}\{#AppExe}"; Description: "Launch {#AppName}"; Flags: nowait postinstall skipifsilent
