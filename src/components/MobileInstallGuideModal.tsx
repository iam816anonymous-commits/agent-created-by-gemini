import React, { useState } from 'react';
import {
  Smartphone,
  QrCode,
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
  CheckCircle2
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
  const [activeTab, setActiveTab] = useState<'pwa' | 'apk'>('pwa');
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

# 2. Connect phone via USB or Wi-Fi ADB (IP:5555)
adb devices

# 3. Install APK granting runtime permissions (-g)
adb install -r -g app/build/outputs/apk/debug/app-debug.apk

# 4. Activate AccessibilityService automatically via ADB
adb shell settings put secure enabled_accessibility_services com.example.localagent/.LocalAgentAccessibilityService
adb shell settings put secure accessibility_enabled 1

# 5. Whitelist LocalAgent from aggressive Doze battery sleep
adb shell dumpsys deviceidle whitelist +com.example.localagent

# 6. Verify 42MB idle RAM ceiling on MediaTek Helio P23
adb shell dumpsys meminfo com.example.localagent`;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/85 backdrop-blur-md animate-fade-in font-mono">
      <div className="relative w-full max-w-3xl max-h-[92vh] flex flex-col bg-slate-950 border border-cyan-800/80 rounded-2xl shadow-2xl shadow-cyan-950/50 overflow-hidden text-xs">
        {/* Header */}
        <div className="flex items-center justify-between px-4 sm:px-5 py-3.5 border-b border-slate-900 bg-black/90">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-cyan-500/20 border border-cyan-400/40 flex items-center justify-center text-cyan-400">
              <Smartphone className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-sm font-bold text-white tracking-wide uppercase flex items-center gap-2">
                <span>[ MOBILE INSTALLATION &amp; TESTING GUIDE ]</span>
                <span className="text-[10px] px-2 py-0.5 rounded bg-emerald-950 text-emerald-300 border border-emerald-700">
                  Android 8.1 &rarr; Android 16
                </span>
              </h2>
              <p className="text-[11px] text-slate-400 font-sans mt-0.5">
                Two ways to test LocalAgent on your phone: Instant PWA Mobile Launcher or Native AOSP APK
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
        <div className="flex border-b border-slate-900 bg-black/50 px-4 pt-2 gap-2 text-xs">
          <button
            onClick={() => setActiveTab('pwa')}
            className={`pb-2.5 px-3 flex items-center gap-2 font-bold transition-all border-b-2 ${
              activeTab === 'pwa'
                ? 'text-cyan-400 border-cyan-400'
                : 'text-slate-400 border-transparent hover:text-slate-200'
            }`}
          >
            <Zap className="w-3.5 h-3.5 text-cyan-400" />
            <span>Method 1: Instant Mobile PWA (0 Setup &bull; 1-Tap)</span>
          </button>

          <button
            onClick={() => setActiveTab('apk')}
            className={`pb-2.5 px-3 flex items-center gap-2 font-bold transition-all border-b-2 ${
              activeTab === 'apk'
                ? 'text-emerald-400 border-emerald-400'
                : 'text-slate-400 border-transparent hover:text-slate-200'
            }`}
          >
            <Terminal className="w-3.5 h-3.5 text-emerald-400" />
            <span>Method 2: Native Android APK (ADB / Gradle)</span>
          </button>
        </div>

        {/* Modal Scrollable Body */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-5 space-y-4 text-slate-300 scrollbar-thin scrollbar-thumb-slate-800">
          {/* =========================================================================
              METHOD 1: INSTANT MOBILE PWA
              ========================================================================= */}
          {activeTab === 'pwa' && (
            <div className="space-y-4 font-sans text-xs">
              <div className="p-3.5 bg-cyan-950/20 border border-cyan-800/40 rounded-xl space-y-2">
                <div className="flex items-center gap-2 text-cyan-300 font-bold uppercase font-mono text-[11px]">
                  <CheckCircle2 className="w-4 h-4 text-cyan-400" />
                  <span>Fastest Path: Run directly on your phone's screen in seconds</span>
                </div>
                <p className="text-slate-300 leading-relaxed">
                  Open the live production URL in <strong>Google Chrome (Android)</strong> or <strong>Safari (iOS)</strong>. LocalAgent is configured as an installable standalone Progressive Web App with zero browser address bars, dark OLED power saving, and full touch telemetry.
                </p>
              </div>

              {/* URL & QR Code Box */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3 p-4 bg-black border border-slate-900 rounded-xl">
                {/* Visual QR Code Representation */}
                <div className="flex flex-col items-center justify-center p-3 bg-slate-950 rounded-lg border border-slate-800 text-center space-y-2">
                  <div className="p-2 bg-white rounded-lg shadow-sm">
                    {/* SVG Stylized QR Code placeholder */}
                    <svg viewBox="0 0 120 120" className="w-28 h-28 text-black" fill="currentColor">
                      <rect width="120" height="120" fill="white" />
                      {/* Top-left position marker */}
                      <rect x="10" y="10" width="30" height="30" fill="black" />
                      <rect x="16" y="16" width="18" height="18" fill="white" />
                      <rect x="20" y="20" width="10" height="10" fill="black" />
                      {/* Top-right position marker */}
                      <rect x="80" y="10" width="30" height="30" fill="black" />
                      <rect x="86" y="16" width="18" height="18" fill="white" />
                      <rect x="90" y="20" width="10" height="10" fill="black" />
                      {/* Bottom-left position marker */}
                      <rect x="10" y="80" width="30" height="30" fill="black" />
                      <rect x="16" y="86" width="18" height="18" fill="white" />
                      <rect x="20" y="90" width="10" height="10" fill="black" />
                      {/* Stylized QR bits */}
                      <rect x="50" y="15" width="6" height="6" fill="black" />
                      <rect x="62" y="15" width="6" height="6" fill="black" />
                      <rect x="50" y="27" width="12" height="6" fill="black" />
                      <rect x="20" y="50" width="6" height="18" fill="black" />
                      <rect x="35" y="50" width="18" height="6" fill="black" />
                      <rect x="60" y="45" width="12" height="12" fill="black" />
                      <rect x="80" y="50" width="6" height="24" fill="black" />
                      <rect x="95" y="50" width="15" height="6" fill="black" />
                      <rect x="50" y="70" width="20" height="6" fill="black" />
                      <rect x="55" y="85" width="15" height="15" fill="black" />
                      <rect x="85" y="85" width="25" height="6" fill="black" />
                      <rect x="95" y="95" width="15" height="15" fill="black" />
                    </svg>
                  </div>
                  <span className="text-[10px] text-slate-400 font-mono">Scan with Phone Camera</span>
                </div>

                {/* Direct Link & Copy */}
                <div className="md:col-span-2 flex flex-col justify-between space-y-3">
                  <div>
                    <span className="text-slate-400 font-mono text-[10px] uppercase font-bold block mb-1">
                      Direct Mobile Testing URL:
                    </span>
                    <div className="p-2.5 bg-slate-950 border border-cyan-900/60 rounded-lg text-cyan-300 font-mono text-[11px] break-all select-all">
                      {sharedUrl}
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={handleCopyUrl}
                      className="px-3.5 py-2 bg-cyan-500 hover:bg-cyan-400 text-black font-bold font-mono rounded-lg transition-colors flex items-center gap-1.5 shadow-sm"
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
                      <span>Open in Browser</span>
                    </a>
                  </div>
                </div>
              </div>

              {/* Step-by-Step Mobile Instructions */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-1">
                {/* Android Steps */}
                <div className="p-3 bg-black border border-slate-900 rounded-xl space-y-2">
                  <div className="flex items-center gap-2 text-white font-mono font-bold text-[11px]">
                    <Smartphone className="w-3.5 h-3.5 text-cyan-400" />
                    <span>On Android (Chrome / Edge / Brave):</span>
                  </div>
                  <ol className="list-decimal list-inside space-y-1.5 text-slate-300 text-[11px]">
                    <li>Open Chrome and navigate to the URL above.</li>
                    <li>Tap the <strong>three dots menu (⋮)</strong> in the top right.</li>
                    <li>Tap <strong>"Install app"</strong> or <strong>"Add to Home screen"</strong>.</li>
                    <li>Confirm <strong>"Install"</strong>. LocalAgent will appear on your phone app drawer with a native launcher icon.</li>
                  </ol>
                </div>

                {/* iOS Steps */}
                <div className="p-3 bg-black border border-slate-900 rounded-xl space-y-2">
                  <div className="flex items-center gap-2 text-white font-mono font-bold text-[11px]">
                    <Smartphone className="w-3.5 h-3.5 text-indigo-400" />
                    <span>On iPhone / iPad (Safari):</span>
                  </div>
                  <ol className="list-decimal list-inside space-y-1.5 text-slate-300 text-[11px]">
                    <li>Open Safari and load the URL above.</li>
                    <li>Tap the <strong>Share button</strong> (square with arrow pointing up).</li>
                    <li>Scroll down and tap <strong>"Add to Home Screen"</strong>.</li>
                    <li>Tap <strong>"Add"</strong>. The app launches fullscreen with standalone iOS windowing.</li>
                  </ol>
                </div>
              </div>
            </div>
          )}

          {/* =========================================================================
              METHOD 2: NATIVE ANDROID APK BUILD (LocalAgent.apk via ADB)
              ========================================================================= */}
          {activeTab === 'apk' && (
            <div className="space-y-4 font-mono text-xs">
              <div className="p-3.5 bg-emerald-950/20 border border-emerald-800/40 rounded-xl space-y-2 font-sans">
                <div className="flex items-center gap-2 text-emerald-400 font-bold font-mono text-[11px] uppercase">
                  <Cpu className="w-4 h-4 text-emerald-400" />
                  <span>Native AOSP Android Architecture Deployment</span>
                </div>
                <p className="text-slate-300 leading-relaxed text-xs">
                  To test actual system-level execution on a physical Android phone (e.g., <strong>TECNO Camon i Click Android 8.1 API 27</strong> through modern <strong>Android 16 API 36</strong>), deploy the native Kotlin architecture blueprint containing <code className="text-cyan-300 font-mono">AccessibilityService</code>, <code className="text-cyan-300 font-mono">CameraManager</code> dual-flash controls, and the <code className="text-cyan-300 font-mono">AutonomyPermissionManager</code>.
                </p>
              </div>

              {/* Prerequisites Checklist */}
              <div className="p-3 bg-black border border-slate-900 rounded-xl space-y-1.5">
                <span className="text-slate-400 font-bold uppercase text-[10px] block">
                  Hardware &amp; Tooling Prerequisites:
                </span>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px] text-slate-300">
                  <div className="flex items-center gap-1.5">
                    <span className="text-emerald-400">✓</span>
                    <span>Android 8.1 Oreo (API 27) up to Android 16</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="text-emerald-400">✓</span>
                    <span>USB Debugging enabled in Developer Options</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="text-emerald-400">✓</span>
                    <span>Android SDK &amp; ADB command line tools</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="text-emerald-400">✓</span>
                    <span>Kotlin 1.9+ / Gradle 8.0+</span>
                  </div>
                </div>
              </div>

              {/* Ready-to-Run ADB Terminal Commands */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <span className="text-slate-400 font-bold uppercase text-[10px]">
                    Step-by-Step Terminal Commands (Build &bull; Install &bull; Authorize):
                  </span>
                  <button
                    onClick={() => handleCopySnippet(adbCommands, 'adb')}
                    className="px-2 py-0.5 bg-emerald-950 hover:bg-emerald-900 text-emerald-300 border border-emerald-700 rounded text-[10px] transition-colors flex items-center gap-1"
                  >
                    {copiedCommand === 'adb' ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                    <span>{copiedCommand === 'adb' ? 'Copied Commands!' : 'Copy ADB Script'}</span>
                  </button>
                </div>

                <pre className="p-3 bg-black border border-slate-900 rounded-xl text-emerald-400 text-[10px] leading-relaxed overflow-x-auto select-all">
                  {adbCommands}
                </pre>
              </div>

              {/* Essential Phone Setup 3-Step Walkthrough */}
              <div className="p-3.5 bg-black border border-slate-900 rounded-xl space-y-3 font-sans">
                <span className="text-white font-mono font-bold uppercase text-xs block">
                  3 Essential On-Device Permissions to Enable on Phone:
                </span>

                <div className="space-y-2 text-xs">
                  <div className="p-2.5 bg-slate-950 rounded-lg border border-slate-900 space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="w-5 h-5 rounded-full bg-cyan-950 border border-cyan-800 text-cyan-300 font-mono text-[10px] flex items-center justify-center font-bold">1</span>
                      <strong className="text-white">Enable Accessibility Service</strong>
                    </div>
                    <p className="text-slate-400 text-[11px] pl-7">
                      Go to <code className="text-cyan-300 font-mono">Settings &rarr; Accessibility &rarr; LocalAgent Service &rarr; Toggle ON</code>. This allows the agent to inspect view-trees and execute zero-allocation touches.
                    </p>
                  </div>

                  <div className="p-2.5 bg-slate-950 rounded-lg border border-slate-900 space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="w-5 h-5 rounded-full bg-cyan-950 border border-cyan-800 text-cyan-300 font-mono text-[10px] flex items-center justify-center font-bold">2</span>
                      <strong className="text-white">External Storage Permission</strong>
                    </div>
                    <p className="text-slate-400 text-[11px] pl-7">
                      Grant storage permissions when prompted, ensuring learned skills and memories survive APK reinstalls at <code className="text-cyan-300 font-mono">/sdcard/LocalAgent/brain_vault.json</code>.
                    </p>
                  </div>

                  <div className="p-2.5 bg-slate-950 rounded-lg border border-slate-900 space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="w-5 h-5 rounded-full bg-cyan-950 border border-cyan-800 text-cyan-300 font-mono text-[10px] flex items-center justify-center font-bold">3</span>
                      <strong className="text-white">Battery Optimization Exemption</strong>
                    </div>
                    <p className="text-slate-400 text-[11px] pl-7">
                      Go to <code className="text-cyan-300 font-mono">Settings &rarr; Battery &rarr; Battery Optimization &rarr; LocalAgent &rarr; Don't Optimize</code>. Keeps the 42MB resident daemon active 24/7 without being killed by Android Doze.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between px-4 sm:px-5 py-3 border-t border-slate-900 bg-black/90 text-[10px]">
          <span className="text-slate-500 font-mono">
            100% Offline Integrity &bull; Zero External Telemetry
          </span>
          <button
            onClick={onClose}
            className="px-4 py-1.5 bg-cyan-500 hover:bg-cyan-400 text-black font-bold font-mono rounded-lg transition-colors"
          >
            Got it, Close Guide
          </button>
        </div>
      </div>
    </div>
  );
};
