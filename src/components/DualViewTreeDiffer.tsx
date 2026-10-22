import React, { useState } from 'react';
import {
  ListTree,
  GitBranch,
  ShieldCheck,
  CheckCircle2,
  AlertTriangle,
  Play,
  RotateCcw,
  Clock,
  Layers,
  ChevronRight,
  ChevronDown,
  Info,
  Maximize2,
  Copy,
  Hash,
  Terminal,
  Activity,
  ArrowRight
} from 'lucide-react';

interface CompactNodeItem {
  id: string;
  className: string;
  text?: string;
  contentDesc?: string;
  bounds: string;
  clickable: boolean;
  editable?: boolean;
  checked?: boolean;
  depth: number;
}

interface ScenarioData {
  id: string;
  title: string;
  actionName: string;
  targetPackage: string;
  latencyMs: number;
  preHash: string;
  postHash: string;
  isMutated: boolean;
  preTree: CompactNodeItem[];
  postTree: CompactNodeItem[];
  addedNodes: string[];
  removedNodes: string[];
  modifiedAttributes: { target: string; from: string; to: string }[];
  verificationRule: string;
  mathematicalProof: string;
}

const PRESET_SCENARIOS: ScenarioData[] = [
  {
    id: 'keep_new_note',
    title: 'Google Keep: Tap "New text note" FAB',
    actionName: 'ClickNode(id=new_note_button, spatial=BOTTOM_RIGHT_FAB)',
    targetPackage: 'com.google.android.keep',
    latencyMs: 38,
    preHash: '0x8A4F19E3B9C102D7',
    postHash: '0xF42893A0C16BE7A1',
    isMutated: true,
    preTree: [
      { id: 'com.google.android.keep:id/browse_root', className: 'android.widget.FrameLayout', bounds: '[0,0][720,1440]', clickable: false, depth: 0 },
      { id: 'com.google.android.keep:id/search_bar', className: 'android.widget.EditText', text: 'Search your notes', bounds: '[32,64][688,140]', clickable: true, depth: 1 },
      { id: 'com.google.android.keep:id/notes_recycler', className: 'androidx.recyclerview.widget.RecyclerView', bounds: '[0,150][720,1320]', clickable: false, depth: 1 },
      { id: 'com.google.android.keep:id/note_card_1', className: 'android.widget.LinearLayout', text: 'Weekend Tasks: Study MediaTek specs', bounds: '[32,160][688,280]', clickable: true, depth: 2 },
      { id: 'com.google.android.keep:id/new_note_button', className: 'android.widget.ImageButton', contentDesc: 'New text note', bounds: '[600,1300][690,1390]', clickable: true, depth: 1 }
    ],
    postTree: [
      { id: 'com.google.android.keep:id/editor_root', className: 'android.widget.FrameLayout', bounds: '[0,0][720,1440]', clickable: false, depth: 0 },
      { id: 'com.google.android.keep:id/editor_toolbar', className: 'android.view.ViewGroup', bounds: '[0,48][720,128]', clickable: false, depth: 1 },
      { id: 'com.google.android.keep:id/navigate_up', className: 'android.widget.ImageButton', contentDesc: 'Navigate up', bounds: '[16,56][80,120]', clickable: true, depth: 2 },
      { id: 'com.google.android.keep:id/editable_title', className: 'android.widget.EditText', text: '', contentDesc: 'Title', bounds: '[32,144][688,210]', clickable: true, editable: true, depth: 1 },
      { id: 'com.google.android.keep:id/edit_note_text', className: 'android.widget.EditText', text: '', contentDesc: 'Note', bounds: '[32,220][688,1100]', clickable: true, editable: true, depth: 1 },
      { id: 'com.google.android.keep:id/bottom_bar', className: 'android.widget.LinearLayout', bounds: '[0,1360][720,1440]', clickable: false, depth: 1 }
    ],
    addedNodes: [
      'com.google.android.keep:id/editable_title (EditText)',
      'com.google.android.keep:id/edit_note_text (EditText)',
      'com.google.android.keep:id/navigate_up (ImageButton)'
    ],
    removedNodes: [
      'com.google.android.keep:id/notes_recycler (RecyclerView)',
      'com.google.android.keep:id/new_note_button (ImageButton)'
    ],
    modifiedAttributes: [
      { target: 'Root Window', from: 'com.google.android.keep:id/browse_root', to: 'com.google.android.keep:id/editor_root' }
    ],
    verificationRule: 'assert(post.packageName == "com.google.android.keep" && post.findNodeById("edit_note_text").isEditable)',
    mathematicalProof: 'FNV-1a 64-bit Hash shifted from 0x8A4F19E3B9C102D7 to 0xF42893A0C16BE7A1. Active editor container emerged in 38ms.'
  },
  {
    id: 'calculator_digit_7',
    title: 'AOSP Calculator: Click Digit "7"',
    actionName: 'ClickNode(id=digit_7, text="7")',
    targetPackage: 'com.android.calculator2',
    latencyMs: 24,
    preHash: '0x1C7B2E490A5F9D31',
    postHash: '0x7B9A03E2F415DC88',
    isMutated: true,
    preTree: [
      { id: 'com.android.calculator2:id/pad_root', className: 'android.widget.LinearLayout', bounds: '[0,0][720,1440]', clickable: false, depth: 0 },
      { id: 'com.android.calculator2:id/formula', className: 'android.widget.TextView', text: '', contentDesc: 'No formula entered', bounds: '[32,60][688,240]', clickable: false, depth: 1 },
      { id: 'com.android.calculator2:id/result', className: 'android.widget.TextView', text: '', bounds: '[32,250][688,380]', clickable: false, depth: 1 },
      { id: 'com.android.calculator2:id/digit_7', className: 'android.widget.Button', text: '7', bounds: '[32,560][176,700]', clickable: true, depth: 2 },
      { id: 'com.android.calculator2:id/digit_8', className: 'android.widget.Button', text: '8', bounds: '[208,560][352,700]', clickable: true, depth: 2 },
      { id: 'com.android.calculator2:id/digit_9', className: 'android.widget.Button', text: '9', bounds: '[384,560][528,700]', clickable: true, depth: 2 }
    ],
    postTree: [
      { id: 'com.android.calculator2:id/pad_root', className: 'android.widget.LinearLayout', bounds: '[0,0][720,1440]', clickable: false, depth: 0 },
      { id: 'com.android.calculator2:id/formula', className: 'android.widget.TextView', text: '7', contentDesc: '7', bounds: '[32,60][688,240]', clickable: false, depth: 1 },
      { id: 'com.android.calculator2:id/result', className: 'android.widget.TextView', text: '', bounds: '[32,250][688,380]', clickable: false, depth: 1 },
      { id: 'com.android.calculator2:id/digit_7', className: 'android.widget.Button', text: '7', bounds: '[32,560][176,700]', clickable: true, depth: 2 },
      { id: 'com.android.calculator2:id/digit_8', className: 'android.widget.Button', text: '8', bounds: '[208,560][352,700]', clickable: true, depth: 2 },
      { id: 'com.android.calculator2:id/digit_9', className: 'android.widget.Button', text: '9', bounds: '[384,560][528,700]', clickable: true, depth: 2 }
    ],
    addedNodes: [],
    removedNodes: [],
    modifiedAttributes: [
      { target: 'com.android.calculator2:id/formula (TextView)', from: 'text: ""', to: 'text: "7"' },
      { target: 'com.android.calculator2:id/formula (TextView)', from: 'contentDesc: "No formula entered"', to: 'contentDesc: "7"' }
    ],
    verificationRule: 'assert(post.formulaNode.text == "7" && post.formulaNode.text != pre.formulaNode.text)',
    mathematicalProof: 'Target node id/formula mutated value from empty to "7". Confirms click reached internal parser and rendered output.'
  },
  {
    id: 'settings_wifi_toggle',
    title: 'System Settings: Toggle Wi-Fi Switch',
    actionName: 'ClickNode(id=switch_widget, label="Wi-Fi")',
    targetPackage: 'com.android.settings',
    latencyMs: 45,
    preHash: '0x3D90EFA258C117B4',
    postHash: '0xE6A174F8B30299C5',
    isMutated: true,
    preTree: [
      { id: 'com.android.settings:id/settings_homepage', className: 'android.widget.FrameLayout', bounds: '[0,0][720,1440]', clickable: false, depth: 0 },
      { id: 'com.android.settings:id/title', className: 'android.widget.TextView', text: 'Network & internet', bounds: '[120,160][480,210]', clickable: false, depth: 2 },
      { id: 'com.android.settings:id/switch_widget', className: 'android.widget.Switch', text: 'OFF', bounds: '[600,150][680,210]', clickable: true, checked: false, depth: 2 },
      { id: 'com.android.settings:id/summary', className: 'android.widget.TextView', text: 'Disconnected', bounds: '[120,220][500,260]', clickable: false, depth: 2 }
    ],
    postTree: [
      { id: 'com.android.settings:id/settings_homepage', className: 'android.widget.FrameLayout', bounds: '[0,0][720,1440]', clickable: false, depth: 0 },
      { id: 'com.android.settings:id/title', className: 'android.widget.TextView', text: 'Network & internet', bounds: '[120,160][480,210]', clickable: false, depth: 2 },
      { id: 'com.android.settings:id/switch_widget', className: 'android.widget.Switch', text: 'ON', bounds: '[600,150][680,210]', clickable: true, checked: true, depth: 2 },
      { id: 'com.android.settings:id/summary', className: 'android.widget.TextView', text: 'Searching for networks...', bounds: '[120,220][500,260]', clickable: false, depth: 2 }
    ],
    addedNodes: [],
    removedNodes: [],
    modifiedAttributes: [
      { target: 'com.android.settings:id/switch_widget (Switch)', from: 'isChecked: false, text: "OFF"', to: 'isChecked: true, text: "ON"' },
      { target: 'com.android.settings:id/summary (TextView)', from: 'text: "Disconnected"', to: 'text: "Searching for networks..."' }
    ],
    verificationRule: 'assert(post.switchWidget.isChecked == true && pre.switchWidget.isChecked == false)',
    mathematicalProof: 'Switch widget isChecked state changed to true. Bounding box coordinates remained invariant ([600,150][680,210]).'
  },
  {
    id: 'sms_otp_copy',
    title: 'SMS Gateway: Copy 6-Digit Verification Token',
    actionName: 'ExtractFragment(tag="otp_verification", regex="\\b\\d{6}\\b")',
    targetPackage: 'com.google.android.apps.messaging',
    latencyMs: 19,
    preHash: '0x5F19C80B44AE27D0',
    postHash: '0x5F19C80B44AE27D0',
    isMutated: false,
    preTree: [
      { id: 'com.google.android.apps.messaging:id/message_list', className: 'android.widget.ListView', bounds: '[0,120][720,1320]', clickable: false, depth: 0 },
      { id: 'com.google.android.apps.messaging:id/message_text', className: 'android.widget.TextView', text: 'Your HDFC Bank security code is 849201. Valid for 5 minutes.', bounds: '[40,240][660,340]', clickable: true, depth: 1 }
    ],
    postTree: [
      { id: 'com.google.android.apps.messaging:id/message_list', className: 'android.widget.ListView', bounds: '[0,120][720,1320]', clickable: false, depth: 0 },
      { id: 'com.google.android.apps.messaging:id/message_text', className: 'android.widget.TextView', text: 'Your HDFC Bank security code is 849201. Valid for 5 minutes.', bounds: '[40,240][660,340]', clickable: true, depth: 1 }
    ],
    addedNodes: [],
    removedNodes: [],
    modifiedAttributes: [],
    verificationRule: 'assert(token.matches("^[0-9]{6}$") && memoryStack.push(token) > 0)',
    mathematicalProof: 'Zero-UI-mutation read-only action. Hash match verified: 0x5F19C80B44AE27D0. Extracted OTP token "849201" pushed to Room MemoryStack.'
  }
];

interface DualViewTreeDifferProps {
  oledMode?: boolean;
  onTraceStep?: (detail: string) => void;
}

export function DualViewTreeDiffer({ oledMode = false, onTraceStep }: DualViewTreeDifferProps) {
  const [selectedScenarioId, setSelectedScenarioId] = useState<string>('keep_new_note');
  const [activeTab, setActiveTab] = useState<'TREES' | 'MUTATIONS' | 'HASH_MATH' | 'TRACER'>('TREES');
  const [treeSearchQuery, setTreeSearchQuery] = useState('');
  const [isSimulatingCapture, setIsSimulatingCapture] = useState(false);
  const [copiedHash, setCopiedHash] = useState<string | null>(null);

  const scenario = PRESET_SCENARIOS.find(s => s.id === selectedScenarioId) || PRESET_SCENARIOS[0];

  const handleCopy = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    setCopiedHash(label);
    setTimeout(() => setCopiedHash(null), 2000);
  };

  const handleSimulateFreshCapture = () => {
    setIsSimulatingCapture(true);
    setTimeout(() => {
      setIsSimulatingCapture(false);
      onTraceStep?.(`SnapshotDiffer verified ${scenario.title} in ${scenario.latencyMs}ms. FNV-1a Hash: ${scenario.postHash}`);
    }, 450);
  };

  const filterNodes = (nodes: CompactNodeItem[]) => {
    if (!treeSearchQuery.trim()) return nodes;
    const q = treeSearchQuery.toLowerCase();
    return nodes.filter(
      n =>
        n.id.toLowerCase().includes(q) ||
        n.className.toLowerCase().includes(q) ||
        (n.text && n.text.toLowerCase().includes(q)) ||
        (n.contentDesc && n.contentDesc.toLowerCase().includes(q))
    );
  };

  return (
    <div
      className={`rounded-2xl border p-4 sm:p-5 transition-all space-y-4 ${
        oledMode ? 'bg-black border-cyan-950' : 'bg-slate-950/80 border-cyan-900/40 shadow-sm'
      }`}
    >
      {/* Header & Badges */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-900 pb-3">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-cyan-950/80 border border-cyan-800 text-cyan-400">
            <ListTree className="w-4 h-4 animate-pulse" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-white tracking-wide font-mono uppercase flex items-center gap-2">
              [ DUAL VIEW-TREE COMPARISON &bull; FNV-1a MUTATION ENGINE ]
            </h2>
            <p className="text-[11px] text-slate-400 font-sans mt-0.5">
              SnapshotDiffer.kt &bull; Pre/Post AccessibilityNodeInfo Hierarchies &bull; Mathematical Proof
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1.5 flex-wrap self-start sm:self-auto">
          <span className="text-[9px] font-mono text-cyan-300 bg-cyan-950/80 px-2 py-0.5 rounded border border-cyan-800/60">
            64-Bit FNV-1a Hasher
          </span>
          <span className="text-[9px] font-mono text-emerald-300 bg-emerald-950/80 px-2 py-0.5 rounded border border-emerald-800/60">
            Zero-Alloc Tree Traversal
          </span>
          <span className="text-[9px] font-mono text-amber-300 bg-amber-950/80 px-2 py-0.5 rounded border border-amber-800/60">
            Helio P23 Compliant
          </span>
        </div>
      </div>

      {/* Scenario Action Selector Pills */}
      <div className="space-y-1.5">
        <div className="text-[10px] font-mono text-slate-400 uppercase tracking-wider flex items-center justify-between">
          <span>Select Verified Action Scenario:</span>
          <button
            onClick={handleSimulateFreshCapture}
            disabled={isSimulatingCapture}
            className="text-[10px] font-mono text-cyan-400 hover:text-cyan-300 flex items-center gap-1"
          >
            <RotateCcw className={`w-3 h-3 ${isSimulatingCapture ? 'animate-spin' : ''}`} />
            <span>Re-Capture Observation</span>
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2 text-xs font-mono">
          {PRESET_SCENARIOS.map(s => {
            const isSelected = s.id === selectedScenarioId;
            return (
              <button
                key={s.id}
                onClick={() => setSelectedScenarioId(s.id)}
                className={`p-2.5 rounded-xl border text-left transition-all flex flex-col justify-between ${
                  isSelected
                    ? 'bg-cyan-950/80 text-white border-cyan-400 shadow-sm shadow-cyan-500/20'
                    : 'bg-black border-slate-900 text-slate-400 hover:text-slate-200 hover:border-slate-800'
                }`}
              >
                <div className="font-bold text-[11px] truncate text-white">{s.title}</div>
                <div className="flex items-center justify-between text-[9px] font-mono text-slate-500 mt-1">
                  <span className="truncate max-w-[130px]">{s.targetPackage}</span>
                  <span className="text-cyan-400 font-bold">{s.latencyMs}ms</span>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Mathematical Verification Banner */}
      <div className="p-3 bg-black border border-cyan-900/60 rounded-xl flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs font-mono">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5">
            <span className="text-slate-500 text-[10px]">PRE-STATE:</span>
            <code className="text-slate-300 font-bold bg-slate-900 px-1.5 py-0.5 rounded border border-slate-800">
              {scenario.preHash}
            </code>
          </div>
          <ArrowRight className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
          <div className="flex items-center gap-1.5">
            <span className="text-slate-500 text-[10px]">POST-STATE:</span>
            <code className="text-cyan-300 font-bold bg-cyan-950/80 px-1.5 py-0.5 rounded border border-cyan-800">
              {scenario.postHash}
            </code>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <span
            className={`px-2 py-0.5 rounded text-[10px] font-bold border ${
              scenario.isMutated
                ? 'bg-emerald-950 text-emerald-400 border-emerald-800'
                : 'bg-indigo-950 text-indigo-300 border-indigo-800'
            }`}
          >
            {scenario.isMutated ? '✓ MUTATED (STATE ADVANCED)' : 'INVARIANT (READ PASS)'}
          </span>
          <span className="text-slate-400 text-[10px]">Latency: {scenario.latencyMs}ms</span>
        </div>
      </div>

      {/* Navigation Sub-Tabs */}
      <div className="flex items-center justify-between border-b border-slate-900 pb-2">
        <div className="flex items-center gap-1 text-xs font-mono">
          {[
            { id: 'TREES', label: 'Side-by-Side Trees', icon: ListTree },
            { id: 'MUTATIONS', label: 'Mutation Delta', icon: GitBranch },
            { id: 'HASH_MATH', label: 'FNV-1a Hashing Math', icon: Hash },
            { id: 'TRACER', label: 'ExecutionTracer Console', icon: Terminal }
          ].map(tab => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as any)}
                className={`px-2.5 py-1 rounded-lg transition-colors flex items-center gap-1 text-[11px] ${
                  isActive
                    ? 'bg-cyan-500 text-black font-bold'
                    : 'bg-black text-slate-400 hover:text-white border border-slate-900'
                }`}
              >
                <Icon className="w-3 h-3" />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>

        {activeTab === 'TREES' && (
          <input
            type="text"
            value={treeSearchQuery}
            onChange={e => setTreeSearchQuery(e.target.value)}
            placeholder="Filter nodes..."
            className="bg-black border border-slate-800 focus:border-cyan-500 rounded-lg px-2 py-0.5 text-[10px] font-mono text-white placeholder-slate-600 focus:outline-none max-w-[140px]"
          />
        )}
      </div>

      {/* TAB CONTENT 1: SIDE-BY-SIDE HIERARCHY TREES */}
      {activeTab === 'TREES' && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs font-mono">
          {/* Pre-Action View Tree */}
          <div className="p-3 bg-black border border-slate-900 rounded-xl space-y-2">
            <div className="flex items-center justify-between border-b border-slate-900 pb-1.5">
              <span className="text-[11px] font-bold text-slate-300 flex items-center gap-1">
                <Clock className="w-3 h-3 text-slate-500" /> Pre-State Hierarchy (T-0)
              </span>
              <span className="text-[9px] text-slate-500 font-mono">
                {scenario.preTree.length} Nodes &bull; {scenario.preHash}
              </span>
            </div>

            <div className="space-y-1 max-h-72 overflow-y-auto pr-1">
              {filterNodes(scenario.preTree).map((node, idx) => (
                <div
                  key={idx}
                  style={{ paddingLeft: `${node.depth * 14}px` }}
                  className="p-1 rounded bg-slate-950/60 border border-slate-900 hover:border-slate-800 text-[10px] transition-colors leading-relaxed"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-cyan-400 font-semibold truncate max-w-[200px]">
                      {node.id.split('/').pop() || node.className.split('.').pop()}
                    </span>
                    <span className="text-slate-600 text-[8px] font-mono">{node.bounds}</span>
                  </div>
                  <div className="text-slate-400 text-[9px] truncate">
                    {node.text ? `"${node.text}"` : node.contentDesc ? `desc: "${node.contentDesc}"` : node.className}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Post-Action View Tree */}
          <div className="p-3 bg-black border border-cyan-900/60 rounded-xl space-y-2">
            <div className="flex items-center justify-between border-b border-slate-900 pb-1.5">
              <span className="text-[11px] font-bold text-cyan-300 flex items-center gap-1">
                <CheckCircle2 className="w-3 h-3 text-cyan-400" /> Post-Action Fresh Capture (T+1)
              </span>
              <span className="text-[9px] text-cyan-400 font-mono">
                {scenario.postTree.length} Nodes &bull; {scenario.postHash}
              </span>
            </div>

            <div className="space-y-1 max-h-72 overflow-y-auto pr-1">
              {filterNodes(scenario.postTree).map((node, idx) => {
                const isNewlyAdded = scenario.addedNodes.some(a => a.includes(node.id.split('/').pop() || 'xyz'));
                return (
                  <div
                    key={idx}
                    style={{ paddingLeft: `${node.depth * 14}px` }}
                    className={`p-1 rounded border text-[10px] transition-colors leading-relaxed ${
                      isNewlyAdded
                        ? 'bg-emerald-950/40 border-emerald-500/80 text-emerald-200'
                        : 'bg-slate-950/60 border-slate-900 text-slate-300'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className={`font-semibold truncate max-w-[200px] ${isNewlyAdded ? 'text-emerald-300 font-bold' : 'text-cyan-400'}`}>
                        {node.id.split('/').pop() || node.className.split('.').pop()}
                        {isNewlyAdded && ' (NEW)'}
                      </span>
                      <span className="text-slate-600 text-[8px] font-mono">{node.bounds}</span>
                    </div>
                    <div className="text-slate-400 text-[9px] truncate">
                      {node.text ? `"${node.text}"` : node.contentDesc ? `desc: "${node.contentDesc}"` : node.className}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* TAB CONTENT 2: MUTATION DELTA BREAKDOWN */}
      {activeTab === 'MUTATIONS' && (
        <div className="space-y-3 font-mono text-xs">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-2.5">
            {/* Added Nodes */}
            <div className="p-3 bg-black border border-emerald-900/60 rounded-xl space-y-2">
              <div className="flex items-center justify-between text-emerald-400 font-bold text-[11px]">
                <span>+ ADDED NODES</span>
                <span className="px-1.5 py-0.2 rounded bg-emerald-950 border border-emerald-800 text-[9px]">
                  {scenario.addedNodes.length}
                </span>
              </div>
              <div className="space-y-1 text-[10px]">
                {scenario.addedNodes.length === 0 ? (
                  <div className="text-slate-600 italic">No new nodes emerged</div>
                ) : (
                  scenario.addedNodes.map((item, idx) => (
                    <div key={idx} className="p-1 rounded bg-emerald-950/30 border border-emerald-800/40 text-emerald-300">
                      + {item}
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Removed Nodes */}
            <div className="p-3 bg-black border border-red-950/60 rounded-xl space-y-2">
              <div className="flex items-center justify-between text-red-400 font-bold text-[11px]">
                <span>- REMOVED NODES</span>
                <span className="px-1.5 py-0.2 rounded bg-red-950 border border-red-900 text-[9px]">
                  {scenario.removedNodes.length}
                </span>
              </div>
              <div className="space-y-1 text-[10px]">
                {scenario.removedNodes.length === 0 ? (
                  <div className="text-slate-600 italic">No nodes pruned</div>
                ) : (
                  scenario.removedNodes.map((item, idx) => (
                    <div key={idx} className="p-1 rounded bg-red-950/30 border border-red-900/40 text-red-300">
                      - {item}
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Modified Attributes */}
            <div className="p-3 bg-black border border-amber-950/60 rounded-xl space-y-2">
              <div className="flex items-center justify-between text-amber-400 font-bold text-[11px]">
                <span>Δ MODIFIED ATTRIBUTES</span>
                <span className="px-1.5 py-0.2 rounded bg-amber-950 border border-amber-800 text-[9px]">
                  {scenario.modifiedAttributes.length}
                </span>
              </div>
              <div className="space-y-1 text-[10px]">
                {scenario.modifiedAttributes.length === 0 ? (
                  <div className="text-slate-600 italic">Attributes unchanged</div>
                ) : (
                  scenario.modifiedAttributes.map((mod, idx) => (
                    <div key={idx} className="p-1 rounded bg-amber-950/30 border border-amber-900/40 space-y-0.5">
                      <div className="text-amber-300 font-semibold">{mod.target}</div>
                      <div className="text-slate-500 text-[9px]">
                        From: <span className="text-slate-400 line-through">{mod.from}</span> &rarr; To:{' '}
                        <span className="text-emerald-400">{mod.to}</span>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>

          {/* Mathematical Proof Card */}
          <div className="p-3 bg-cyan-950/20 border border-cyan-800/40 rounded-xl space-y-1">
            <div className="text-[10px] text-cyan-400 font-bold uppercase flex items-center gap-1.5">
              <ShieldCheck className="w-3.5 h-3.5" />
              <span>Independent Verification Rule &amp; Proof:</span>
            </div>
            <div className="text-slate-300 text-[11px] leading-relaxed">
              <code>{scenario.verificationRule}</code>
            </div>
            <p className="text-[10px] text-slate-400 font-sans pt-1 border-t border-cyan-900/40">
              <strong className="text-white">Engineering Proof:</strong> {scenario.mathematicalProof}
            </p>
          </div>
        </div>
      )}

      {/* TAB CONTENT 3: FNV-1a HASHING MATHEMATICS */}
      {activeTab === 'HASH_MATH' && (
        <div className="space-y-3 font-mono text-xs">
          <div className="p-3 bg-black border border-slate-900 rounded-xl space-y-2">
            <h3 className="text-xs font-bold text-white uppercase flex items-center gap-1.5">
              <Hash className="w-3.5 h-3.5 text-cyan-400" />
              <span>64-Bit Fowler-Noll-Vo (FNV-1a) State Signature Algorithm</span>
            </h3>
            <p className="text-[10px] text-slate-400 font-sans leading-relaxed">
              To operate strictly within the 48MB idle memory budget on the MediaTek Helio P23, LocalAgent does not store
              bloated JSON trees across steps. Instead, it computes a stream-oriented 64-bit FNV-1a hash during single-pass
              Accessibility tree traversal with zero intermediate heap allocations.
            </p>
            <div className="p-2.5 bg-slate-950 border border-slate-900 rounded-lg text-[10px] text-cyan-300 leading-relaxed overflow-x-auto">
              <div className="text-slate-500">// Fowler-Noll-Vo 64-bit Constants (Android 8.1 / Kotlin)</div>
              <div>val FNV_OFFSET_BASIS = -3750763034362895579L // 0xcbf29ce484222325</div>
              <div>val FNV_PRIME = 1099511628211L // 0x100000001b3</div>
              <div className="mt-1 text-slate-400">
                hash = (hash xor char.code.toLong()) * FNV_PRIME
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 text-[10px]">
            <div className="p-2.5 bg-black border border-slate-900 rounded-lg space-y-1">
              <span className="text-slate-500 uppercase">TIME COMPLEXITY</span>
              <div className="text-white font-bold">O(N) Single-Pass Stream</div>
              <div className="text-emerald-400">&lt; 0.4ms for 50-node layout tree</div>
            </div>
            <div className="p-2.5 bg-black border border-slate-900 rounded-lg space-y-1">
              <span className="text-slate-500 uppercase">HEAP ALLOCATION</span>
              <div className="text-white font-bold">0 Bytes Allocated</div>
              <div className="text-cyan-400">Zero GC pauses on Helio P23 Cortex-A53</div>
            </div>
          </div>
        </div>
      )}

      {/* TAB CONTENT 4: EXECUTION TRACER CONSOLE */}
      {activeTab === 'TRACER' && (
        <div className="space-y-2 font-mono text-xs">
          <div className="flex items-center justify-between text-[10px] text-slate-400">
            <span>ExecutionTracer Ring Buffer (Capacity: 64 Steps):</span>
            <span className="text-emerald-400">100% Monotonic Clock Verified</span>
          </div>

          <div className="bg-black border border-slate-900 rounded-xl p-3 max-h-64 overflow-y-auto space-y-1.5 text-[10px]">
            {PRESET_SCENARIOS.map((s, idx) => (
              <div key={s.id} className="p-1.5 bg-slate-950 rounded border border-slate-900 flex items-start justify-between gap-2">
                <div>
                  <div className="flex items-center gap-1.5 font-bold">
                    <span className="text-cyan-400">Step #{idx + 1}</span>
                    <span className="text-white">{s.title}</span>
                  </div>
                  <div className="text-slate-400 text-[9px] mt-0.5">
                    Pre: <span className="text-slate-300">{s.preHash}</span> &rarr; Post:{' '}
                    <span className="text-cyan-300">{s.postHash}</span> &bull; {s.latencyMs}ms
                  </div>
                </div>
                <span className="px-1.5 py-0.5 rounded text-[8px] font-bold bg-emerald-950 text-emerald-400 border border-emerald-800 shrink-0">
                  VERIFIED
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
