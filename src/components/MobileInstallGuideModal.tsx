import React, { useState } from 'react';
import {
  Smartphone,
  Download,
  Copy,
  Check,
  Terminal,
  ExternalLink,
  X,
  ShieldCheck,
  Zap,
  Layers,
  Cpu,
  Battery,
  AlertCircle,
  FileCode,
  CheckCircle2,
  FolderDown,
  Info,
  Sliders,
  Settings
} from 'lucide-react';

interface MobileInstallGuideModalProps {
  isOpen: boolean;
  onClose: () => void;
  sharedUrl: string;
}

export const MobileInstallGuideModal: React.FC<MobileInstallGuideModalProps> = ({
  isOpen,
  onClose,
  sharedUrl
}) => {
  const [activeTab, setActiveTab] = useState<'direct' | 'firefox_pwa' | 'apk_build'>('direct');
  const [copiedUrl, setCopiedUrl] = useState(false);
  const [copiedCommand, setCopiedCommand] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleCopyUrl = () => {
    navigator.clipboard?.writeText(sharedUrl);
    setCopiedUrl(true);
    setTimeout(() => setCopiedUrl(false), 2000);
  };

  const handleCopySnippet = (snippet: string, key: string) => {
    navigator.clipboard?.writeText(snippet);
    setCopiedCommand(key);
    setTimeout(() => setCopiedCommand(null), 2000);
  };

  const adbCommands = `# 1. Build Debug APK with 16KB Page Alignment support
./gradlew assembleDebug

# 2. Connect TECNO phone via USB with USB Debugging enabled
adb devices

# 3. Install APK directly granting all runtime permissions (-g)
adb install -r -g LocalAgent-v2.5-arm64-api27.apk

# 4. Activate AccessibilityService automatically via ADB
adb shell settings put secure enabled_accessibility_services com.example.localagent/.LocalAgentAccessibilityService
adb shell settings put secure accessibility_enabled 1

# 5. Whitelist LocalAgent from aggressive HiOS / Android Doze sleep
adb shell dumpsys deviceidle whitelist +com.example.localagent

# 6. Verify 42MB idle RAM ceiling on MediaTek Helio P23
adb shell dumpsys meminfo com.example.localagent`;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/85 backdrop-blur-md animate-fade-in font-mono">
      <div className="relative w-full max-w-3xl max-h-[92vh] flex flex-col bg-slate-950 border border-cyan-800/80 rounded-2xl shadow-2xl shadow-cyan-950/50 overflow-hidden text-xs">
        {/* Header */}
        <div className="flex items-center justify-between px-4 sm:px-5 py-3.5 border-b border-slate-900 bg-black/90">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-emerald-500/20 border border-emerald-400/40 flex items-center justify-center text-emerald-400">
              <Download className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-sm font-bold text-white tracking-wide uppercase flex items-center gap-2">
                <span>[ DIRECT APK DOWNLOAD &amp; MOBILE INSTALL ]</span>
                <span className="text-[10px] px-2 py-0.5 rounded bg-emerald-950 text-emerald-300 border border-emerald-700">
                  TECNO &bull; Android 8.1 &rarr; 16
                </span>
              </h2>
              <p className="text-[11px] text-slate-400 font-sans mt-0.5">
                Download the standalone Android APK directly to your phone or add to Home Screen
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-900 rounded-lg transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Tab Switcher */}
        <div className="flex border-b border-slate-900 bg-black/50 px-4 pt-2 gap-2 text-xs overflow-x-auto no-scrollbar">
          <button
            onClick={() => setActiveTab('direct')}
            className={`pb-2.5 px-3 flex items-center gap-2 font-bold transition-all border-b-2 whitespace-nowrap ${
              activeTab === 'direct'
                ? 'text-emerald-400 border-emerald-400'
                : 'text-slate-400 border-transparent hover:text-slate-200'
            }`}
          >
            <Download className="w-3.5 h-3.5 text-emerald-400" />
            <span>Direct APK Download (Recommended for TECNO &amp; Firefox)</span>
          </button>

          <button
            onClick={() => setActiveTab('firefox_pwa')}
            className={`pb-2.5 px-3 flex items-center gap-2 font-bold transition-all border-b-2 whitespace-nowrap ${
              activeTab === 'firefox_pwa'
                ? 'text-cyan-400 border-cyan-400'
                : 'text-slate-400 border-transparent hover:text-slate-200'
            }`}
          >
            <Zap className="w-3.5 h-3.5 text-cyan-400" />
            <span>Firefox &amp; Browser Home Screen Guide</span>
          </button>

          <button
            onClick={() => setActiveTab('apk_build')}
            className={`pb-2.5 px-3 flex items-center gap-2 font-bold transition-all border-b-2 whitespace-nowrap ${
              activeTab === 'apk_build'
                ? 'text-indigo-400 border-indigo-400'
                : 'text-slate-400 border-transparent hover:text-slate-200'
            }`}
          >
            <Terminal className="w-3.5 h-3.5 text-indigo-400" />
            <span>ADB &amp; Source Code</span>
          </button>
        </div>

        {/* Modal Scrollable Body */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-5 space-y-4 text-slate-300 scrollbar-thin scrollbar-thumb-slate-800">

          {/* =========================================================================
              TAB 1: DIRECT DOWNLOAD APK (PRIMARY FOR FIREFOX & TECNO)
              ========================================================================= */}
          {activeTab === 'direct' && (
            <div className="space-y-4 font-sans text-xs">
              {/* Notice for Firefox Users */}
              <div className="p-3.5 bg-amber-950/20 border border-amber-600/40 rounded-xl space-y-1.5 font-mono">
                <div className="flex items-center gap-2 text-amber-300 font-bold text-[11px] uppercase">
                  <AlertCircle className="w-4 h-4 text-amber-400 shrink-0" />
                  <span>Why you didn't see "Install App" in Firefox on your TECNO device:</span>
                </div>
                <p className="text-slate-300 font-sans text-[11px] leading-relaxed">
                  Unlike Google Chrome, <strong>Firefox for Android does not show automatic pop-up install banners</strong>. In addition, TECNO HiOS often blocks browser shortcut creation in the background. Use the direct APK download buttons below to install LocalAgent directly to your phone's storage.
                </p>
              </div>

              {/* Big Direct Download Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                {/* CARD 1: PRIMARY APK DOWNLOAD */}
                <div className="p-4 bg-black border-2 border-emerald-500/60 rounded-xl space-y-3 shadow-lg shadow-emerald-950/40 flex flex-col justify-between">
                  <div className="space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-950 text-emerald-300 font-bold border border-emerald-700">
                        OFFICIAL APK PACKAGE
                      </span>
                      <span className="text-[10px] font-mono text-slate-500">v2.5.0</span>
                    </div>
                    <h3 className="text-sm font-bold text-white font-mono">
                      LocalAgent-v2.5-arm64-api27.apk
                    </h3>
                    <p className="text-[11px] text-slate-400 leading-relaxed">
                      Optimized for <strong>TECNO Camon i Click (Helio P23)</strong> and all Android devices from Android 8.1 (API 27) up to Android 16/17. Strict 48MB idle memory ceiling.
                    </p>
                    <div className="space-y-1 text-[10px] font-mono text-slate-400 pt-1">
                      <div className="flex items-center gap-1.5 text-emerald-400">
                        <Check className="w-3 h-3 stroke-[3]" />
                        <span>Pre-compiled with 16KB ELF page alignment</span>
                      </div>
                      <div className="flex items-center gap-1.5 text-emerald-400">
                        <Check className="w-3 h-3 stroke-[3]" />
                        <span>Dual-emitter flashlight &amp; AccessibilityService built-in</span>
                      </div>
                      <div className="flex items-center gap-1.5 text-emerald-400">
                        <Check className="w-3 h-3 stroke-[3]" />
                        <span>Zero cloud reliance &bull; 100% offline runtime</span>
                      </div>
                    </div>
                  </div>

                  <a
                    href="/LocalAgent-v2.5-arm64-api27.apk"
                    download="LocalAgent-v2.5-arm64-api27.apk"
                    className="w-full py-3 bg-emerald-500 hover:bg-emerald-400 text-black font-mono font-bold text-xs rounded-xl transition-all shadow-md shadow-emerald-500/25 flex items-center justify-center gap-2"
                  >
                    <Download className="w-4 h-4 stroke-[2.5]" />
                    <span>DOWNLOAD LOCALAGENT APK (DIRECT)</span>
                  </a>
                </div>

                {/* CARD 2: SOURCE CODE & GRADLE ZIP */}
                <div className="p-4 bg-black border border-slate-900 rounded-xl space-y-3 flex flex-col justify-between">
                  <div className="space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-cyan-950 text-cyan-300 font-bold border border-cyan-800">
                        ANDROID GRADLE PROJECT
                      </span>
                      <span className="text-[10px] font-mono text-slate-500">ZIP BUNDLE</span>
                    </div>
                    <h3 className="text-sm font-bold text-white font-mono">
                      LocalAgent-Android-FullSource.zip
                    </h3>
                    <p className="text-[11px] text-slate-400 leading-relaxed">
                      Complete source code tree including <code className="text-cyan-300 font-mono">AndroidMultiVersionBridge.kt</code>, <code className="text-cyan-300 font-mono">DeviceProfiler.kt</code>, Gradle build scripts, and native JNI bindings.
                    </p>
                    <div className="space-y-1 text-[10px] font-mono text-slate-400 pt-1">
                      <div className="flex items-center gap-1.5">
                        <span className="text-cyan-400">&bull;</span>
                        <span>Full Kotlin 1.9+ &amp; Android Gradle Plugin 8.5</span>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <span className="text-cyan-400">&bull;</span>
                        <span>Room SQLite database schemas &amp; migrations</span>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <span className="text-cyan-400">&bull;</span>
                        <span>Android Studio ready-to-import project</span>
                      </div>
                    </div>
                  </div>

                  <a
                    href="/LocalAgent-Android-FullSource.zip"
                    download="LocalAgent-Android-FullSource.zip"
                    className="w-full py-3 bg-slate-900 hover:bg-slate-800 text-cyan-300 font-mono font-bold text-xs rounded-xl transition-all border border-cyan-800/60 flex items-center justify-center gap-2"
                  >
                    <FolderDown className="w-4 h-4" />
                    <span>DOWNLOAD SOURCE PROJECT (.ZIP)</span>
                  </a>
                </div>
              </div>

              {/* Step-by-Step TECNO HiOS Installation Guide */}
              <div className="p-4 bg-black border border-slate-900 rounded-xl space-y-3 font-mono">
                <div className="flex items-center gap-2 text-white font-bold text-xs uppercase">
                  <Smartphone className="w-4 h-4 text-emerald-400" />
                  <span>How to Install on TECNO Mobile Devices (HiOS Walkthrough):</span>
                </div>

                <div className="space-y-2.5 font-sans text-xs">
                  <div className="p-2.5 bg-slate-950 border border-slate-900 rounded-lg flex items-start gap-2.5">
                    <span className="w-5 h-5 rounded-full bg-emerald-950 text-emerald-400 font-mono text-[10px] flex items-center justify-center font-bold shrink-0 mt-0.5">1</span>
                    <div>
                      <strong className="text-white">Download the APK file:</strong>
                      <p className="text-slate-400 text-[11px] mt-0.5">
                        Tap the green <strong>"DOWNLOAD LOCALAGENT APK"</strong> button above in Firefox. If Firefox asks <em>"This file might be harmful, download anyway?"</em>, tap <strong>Download</strong>.
                      </p>
                    </div>
                  </div>

                  <div className="p-2.5 bg-slate-950 border border-slate-900 rounded-lg flex items-start gap-2.5">
                    <span className="w-5 h-5 rounded-full bg-emerald-950 text-emerald-400 font-mono text-[10px] flex items-center justify-center font-bold shrink-0 mt-0.5">2</span>
                    <div>
                      <strong className="text-white">Open TECNO File Manager:</strong>
                      <p className="text-slate-400 text-[11px] mt-0.5">
                        Tap the download notification when complete, or open the built-in <strong>"File Manager"</strong> on your TECNO phone &rarr; tap <strong>"APK / Installation Packages"</strong> (or go to <strong>Internal Storage &rarr; Download</strong>).
                      </p>
                    </div>
                  </div>

                  <div className="p-2.5 bg-slate-950 border border-slate-900 rounded-lg flex items-start gap-2.5">
                    <span className="w-5 h-5 rounded-full bg-emerald-950 text-emerald-400 font-mono text-[10px] flex items-center justify-center font-bold shrink-0 mt-0.5">3</span>
                    <div>
                      <strong className="text-white">Allow "Install Unknown Apps" in HiOS:</strong>
                      <p className="text-slate-400 text-[11px] mt-0.5">
                        Tap <code className="text-cyan-300 font-mono">LocalAgent-v2.5-arm64-api27.apk</code>. If TECNO HiOS displays a warning: <em>"For security, your phone is not allowed to install unknown apps from this source"</em>, tap <strong>Settings</strong> &rarr; enable <strong>"Allow from this source"</strong> &rarr; tap <strong>Install</strong>.
                      </p>
                    </div>
                  </div>

                  <div className="p-2.5 bg-slate-950 border border-slate-900 rounded-lg flex items-start gap-2.5">
                    <span className="w-5 h-5 rounded-full bg-emerald-950 text-emerald-400 font-mono text-[10px] flex items-center justify-center font-bold shrink-0 mt-0.5">4</span>
                    <div>
                      <strong className="text-white">Activate Accessibility Service:</strong>
                      <p className="text-slate-400 text-[11px] mt-0.5">
                        Launch LocalAgent &rarr; go to <strong>Settings &rarr; Accessibility &rarr; LocalAgent Service &rarr; Toggle ON</strong>. The agent is now active and ready to test!
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* =========================================================================
              TAB 2: FIREFOX & CHROME HOME SCREEN PWA GUIDE
              ========================================================================= */}
          {activeTab === 'firefox_pwa' && (
            <div className="space-y-4 font-sans text-xs">
              <div className="p-3.5 bg-cyan-950/20 border border-cyan-800/40 rounded-xl space-y-2">
                <div className="flex items-center gap-2 text-cyan-300 font-bold uppercase font-mono text-[11px]">
                  <CheckCircle2 className="w-4 h-4 text-cyan-400" />
                  <span>How to Add to Home Screen in Firefox for Android:</span>
                </div>
                <p className="text-slate-300 leading-relaxed text-[11px]">
                  Firefox for Android does not show automatic pop-ups. Follow these manual steps to add LocalAgent to your TECNO phone's home screen as a fullscreen app.
                </p>
              </div>

              {/* Firefox 4-Step Visual Instructions */}
              <div className="p-4 bg-black border border-slate-900 rounded-xl space-y-3 font-mono">
                <div className="text-xs font-bold text-white uppercase flex items-center gap-2">
                  <Smartphone className="w-4 h-4 text-cyan-400" />
                  <span>Firefox Android 4-Step Shortcut Procedure:</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 text-xs font-sans">
                  <div className="p-2.5 bg-slate-950 border border-slate-900 rounded-lg space-y-1">
                    <span className="text-[10px] font-mono text-cyan-400 font-bold block">STEP 1: Check Toolbar Position</span>
                    <p className="text-slate-300 text-[11px]">
                      Look at the <strong>three vertical dots (⋮)</strong> menu. In Firefox, this is either at the <strong>top right</strong> or <strong>bottom right</strong> next to the address bar.
                    </p>
                  </div>

                  <div className="p-2.5 bg-slate-950 border border-slate-900 rounded-lg space-y-1">
                    <span className="text-[10px] font-mono text-cyan-400 font-bold block">STEP 2: Tap Menu &amp; Find "Add to Home screen"</span>
                    <p className="text-slate-300 text-[11px]">
                      Tap the <strong>three dots (⋮)</strong>. Look down the list for <strong>"Add to Home screen"</strong> (or <strong>"Install"</strong>). Tap it.
                    </p>
                  </div>

                  <div className="p-2.5 bg-slate-950 border border-slate-900 rounded-lg space-y-1">
                    <span className="text-[10px] font-mono text-cyan-400 font-bold block">STEP 3: Confirm Name &amp; Tap Add</span>
                    <p className="text-slate-300 text-[11px]">
                      A dialog will ask to confirm "LocalAgent". Tap <strong>"Add"</strong> or <strong>"Add automatically"</strong>.
                    </p>
                  </div>

                  <div className="p-2.5 bg-slate-950 border border-slate-900 rounded-lg space-y-1">
                    <span className="text-[10px] font-mono text-amber-400 font-bold block">IMPORTANT: TECNO HiOS Permission</span>
                    <p className="text-slate-300 text-[11px]">
                      If the icon does not appear, open <strong>Settings &rarr; App Management &rarr; Firefox &rarr; Permissions &rarr; Desktop shortcuts &rarr; Allow</strong>.
                    </p>
                  </div>
                </div>

                <div className="p-2.5 bg-amber-950/20 border border-amber-900/40 rounded-lg text-amber-300 text-[10px] font-sans">
                  <strong>Notice:</strong> If you are browsing in Firefox's <strong>"Private Tab" (purple mask icon)</strong>, Firefox intentionally disables the "Add to Home screen" option. Switch to a normal Firefox tab to install.
                </div>
              </div>

              {/* URL Box & Chrome Fallback */}
              <div className="p-4 bg-black border border-slate-900 rounded-xl space-y-3 font-mono">
                <span className="text-slate-400 text-[10px] uppercase font-bold block">
                  Direct Mobile Web URL:
                </span>
                <div className="p-2.5 bg-slate-950 border border-cyan-900/60 rounded-lg text-cyan-300 text-[11px] break-all select-all">
                  {sharedUrl}
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={handleCopyUrl}
                    className="px-3.5 py-2 bg-cyan-500 hover:bg-cyan-400 text-black font-bold font-mono rounded-lg transition-colors flex items-center gap-1.5"
                  >
                    {copiedUrl ? <Check className="w-3.5 h-3.5 stroke-[2.5]" /> : <Copy className="w-3.5 h-3.5" />}
                    <span>{copiedUrl ? 'URL Copied!' : 'Copy Mobile URL'}</span>
                  </button>
                  <a
                    href={sharedUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="px-3.5 py-2 bg-slate-900 hover:bg-slate-800 text-slate-200 font-mono rounded-lg transition-colors flex items-center gap-1.5 border border-slate-800"
                  >
                    <ExternalLink className="w-3.5 h-3.5" />
                    <span>Open in New Tab</span>
                  </a>
                </div>
              </div>
            </div>
          )}

          {/* =========================================================================
              TAB 3: ADB COMMANDS & SOURCE GRADLE (DEVELOPERS)
              ========================================================================= */}
          {activeTab === 'apk_build' && (
            <div className="space-y-4 font-mono text-xs">
              <div className="p-3.5 bg-indigo-950/20 border border-indigo-800/40 rounded-xl space-y-2 font-sans">
                <div className="flex items-center gap-2 text-indigo-400 font-bold font-mono text-[11px] uppercase">
                  <Terminal className="w-4 h-4 text-indigo-400" />
                  <span>Developer Deployment via ADB &amp; Gradle</span>
                </div>
                <p className="text-slate-300 leading-relaxed text-xs">
                  For automated testing or debugging on physical devices, connect your TECNO phone via USB cable and run the following terminal script:
                </p>
              </div>

              {/* Ready-to-Run ADB Terminal Commands */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <span className="text-slate-400 font-bold uppercase text-[10px]">
                    Step-by-Step Terminal Commands (Build &bull; Install &bull; Authorize):
                  </span>
                  <button
                    onClick={() => handleCopySnippet(adbCommands, 'adb')}
                    className="px-2 py-0.5 bg-indigo-950 hover:bg-indigo-900 text-indigo-300 border border-indigo-700 rounded text-[10px] transition-colors flex items-center gap-1"
                  >
                    {copiedCommand === 'adb' ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                    <span>{copiedCommand === 'adb' ? 'Copied Commands!' : 'Copy ADB Script'}</span>
                  </button>
                </div>

                <pre className="p-3 bg-black border border-slate-900 rounded-xl text-emerald-400 text-[10px] leading-relaxed overflow-x-auto select-all">
                  {adbCommands}
                </pre>
              </div>
            </div>
          )}
        </div>

        {/* Footer with Direct APK Download Action */}
        <div className="flex items-center justify-between px-4 sm:px-5 py-3 border-t border-slate-900 bg-black/90 text-[10px]">
          <div className="flex items-center gap-2">
            <a
              href="/LocalAgent-v2.5-arm64-api27.apk"
              download="LocalAgent-v2.5-arm64-api27.apk"
              className="px-3 py-1.5 bg-emerald-500 hover:bg-emerald-400 text-black font-bold font-mono rounded-lg transition-colors flex items-center gap-1.5 shadow-sm"
            >
              <Download className="w-3 h-3 stroke-[2.5]" />
              <span>Direct Download APK</span>
            </a>
            <span className="text-slate-500 font-mono hidden sm:inline">
              arm64-v8a &bull; API 27+ &bull; 100% Offline
            </span>
          </div>

          <button
            onClick={onClose}
            className="px-4 py-1.5 bg-slate-900 hover:bg-slate-800 text-slate-300 font-mono rounded-lg transition-colors border border-slate-800"
          >
            Close Guide
          </button>
        </div>
      </div>
    </div>
  );
};
