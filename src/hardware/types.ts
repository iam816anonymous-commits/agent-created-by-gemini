/**
 * Authoritative Hardware Types, Provenance Contracts, and Operation Models.
 *
 * Enforces Zero Fabricated Data:
 * Every metric and capability explicitly declares its provenance (MEASURED, ENUMERATED,
 * ESTIMATED, UNAVAILABLE, UNSUPPORTED, PERMISSION_DENIED, ERROR, STALE).
 */

export type Provenance =
  | 'MEASURED'           // Directly obtained from a live, real platform API
  | 'ENUMERATED'         // Confirmed to exist through a supported platform discovery API
  | 'ESTIMATED'          // Derived mathematically from measured values via documented formula
  | 'UNAVAILABLE'        // Platform API does not expose the information (sandboxed/hidden)
  | 'UNSUPPORTED'        // Hardware or API explicitly not supported by this platform
  | 'PERMISSION_DENIED'  // Operation requires permission that was denied by user or OS
  | 'PERMISSION_REQUIRED'// Operation requires permission that has not yet been requested
  | 'ERROR'              // API call attempted but threw an exception
  | 'STALE';             // Last valid reading exceeds the freshness threshold

export interface HardwareValue<T> {
  readonly value: T | null;
  readonly provenance: Provenance;
  readonly timestamp: number;
  readonly sourceApi?: string;
  readonly error?: string;
  readonly calculationMethod?: string;
}

export type HardwareOperationStatus =
  | 'SUCCESS_VERIFIED'
  | 'COMMAND_ACCEPTED_UNVERIFIED'
  | 'UNSUPPORTED'
  | 'UNAVAILABLE'
  | 'PERMISSION_REQUIRED'
  | 'PERMISSION_DENIED'
  | 'INVALID_PARAMETERS'
  | 'BUSY'
  | 'TIMEOUT'
  | 'CANCELLED'
  | 'EMERGENCY_STOP_ACTIVE'
  | 'FAILED';

export interface HardwareOperationResult {
  readonly operationId: string;
  readonly action: string;
  readonly actuatorId: string;
  readonly status: HardwareOperationStatus;
  readonly timestamp: number;
  readonly message: string;
  readonly verifiedPhysicalState?: boolean;
  readonly provenance: Provenance;
  readonly details?: Record<string, any>;
  readonly durationMs?: number;
}

export interface SensorCapability {
  readonly id: string;
  readonly type: string;
  readonly name: string;
  readonly vendor: string | null;
  readonly supported: boolean;
  readonly permissionStatus: 'granted' | 'prompt' | 'denied' | 'unsupported';
  readonly provenance: Provenance;
  readonly minDelayUs: number | null;
  readonly maxRange: number | null;
  readonly resolution: number | null;
  readonly powerMa: number | null;
}

export interface SensorReading<T = any> {
  readonly sensorId: string;
  readonly status: 'AVAILABLE' | 'STALE' | 'UNAVAILABLE' | 'PERMISSION_DENIED' | 'ERROR';
  readonly provenance: Provenance;
  readonly timestamp: number;
  readonly data: T | null;
  readonly error?: string;
}

export interface ActuatorCapability {
  readonly id: string;
  readonly type: 'TORCH' | 'HAPTIC' | 'AUDIO' | 'DISPLAY_BRIGHTNESS' | 'WIFI' | 'BLUETOOTH' | 'CAMERA';
  readonly name: string;
  readonly physicalDevice: string;
  readonly isControllable: boolean;
  readonly supportsStrengthLevel: boolean;
  readonly maxStrengthLevel: number | null;
  readonly supportsWaveModulation: boolean;
  readonly permissionStatus: 'granted' | 'prompt' | 'denied' | 'unsupported';
  readonly cameraFacing?: 'environment' | 'user' | 'unknown';
  readonly cameraId?: string;
  readonly provenance: Provenance;
}

export interface ObservedActuatorState {
  readonly actuatorId: string;
  readonly requestedState: any;
  readonly observedPhysicalState: any;
  readonly status: HardwareOperationStatus;
  readonly lastVerifiedMs: number | null;
  readonly activeOperationId: string | null;
  readonly provenance: Provenance;
  readonly verificationLimitation?: string;
}

export interface SystemTelemetry {
  readonly osName: HardwareValue<string>;
  readonly osVersion: HardwareValue<string>;
  readonly platform: HardwareValue<string>;
  readonly deviceModel: HardwareValue<string>;
  readonly deviceManufacturer: HardwareValue<string>;
  readonly isMobile: HardwareValue<boolean>;
}

export interface MemoryTelemetry {
  readonly totalRamMb: HardwareValue<number>;
  readonly calculatedSafeCeilingMb: HardwareValue<number>;
  readonly osOverheadMb: HardwareValue<number>;
  readonly activeAppBufferMb: HardwareValue<number>;
}

export interface ProcessorTelemetry {
  readonly cpuCores: HardwareValue<number>;
  readonly cpuArchitecture: HardwareValue<string>;
}

export interface GraphicsTelemetry {
  readonly gpuVendor: HardwareValue<string>;
  readonly gpuRenderer: HardwareValue<string>;
}

export interface DisplayTelemetry {
  readonly screenWidth: HardwareValue<number>;
  readonly screenHeight: HardwareValue<number>;
  readonly pixelRatio: HardwareValue<number>;
  readonly colorDepth: HardwareValue<number>;
  readonly touchPoints: HardwareValue<number>;
}

export interface BatteryTelemetry {
  readonly level: HardwareValue<number>;
  readonly charging: HardwareValue<boolean>;
  readonly chargingTime: HardwareValue<number>;
  readonly dischargingTime: HardwareValue<number>;
}

export interface NetworkTelemetry {
  readonly type: HardwareValue<string>;
  readonly effectiveType: HardwareValue<string>;
  readonly downlinkMbps: HardwareValue<number>;
  readonly rttMs: HardwareValue<number>;
  readonly saveData: HardwareValue<boolean>;
}

export interface DeviceAdaptiveRecommendation {
  readonly modelId: string;
  readonly modelName: string;
  readonly tier: 'TIER_1' | 'TIER_2' | 'TIER_3';
  readonly policy: 'MEASURED_PROFILE' | 'CONSERVATIVE_SANDBOX_POLICY' | 'USER_MANUAL_SELECTION';
  readonly rationale: string;
  readonly isRamMeasured: boolean;
  readonly ramUsedForCalcMb: number | null;
  readonly safeCeilingMb: number | null;
}

export interface DiscoveredHardwareRegistry {
  readonly system: SystemTelemetry;
  readonly memory: MemoryTelemetry;
  readonly processor: ProcessorTelemetry;
  readonly graphics: GraphicsTelemetry;
  readonly display: DisplayTelemetry;
  readonly battery: BatteryTelemetry;
  readonly network: NetworkTelemetry;
  readonly sensors: SensorCapability[];
  readonly actuators: ActuatorCapability[];
  readonly recommendation: DeviceAdaptiveRecommendation;
  readonly discoveredAt: number;
  readonly isRealPhysicalDiscovery: boolean;
}

export type OpticalWaveFormat =
  | 'CONSTANT'
  | 'SINE_WAVE'
  | 'STROBE_BURST'
  | 'DUAL_ALTERNATING'
  | 'SOS_BEACON'
  | 'HEARTBEAT';

export interface HardwareAuthorization {
  readonly userApproved: boolean;
  readonly grantedAt?: number;
  readonly context?: string;
}

export interface EmergencyStopReport {
  readonly stoppedAt: number;
  readonly cancelledOperationsCount: number;
  readonly actuatorShutdownResults: Record<string, HardwareOperationResult>;
  readonly allActuatorsConfirmedOff: boolean;
  readonly reason?: string;
  readonly actuatorsSilenced?: string[];
  readonly operationsCancelled?: string[];
}

// ==========================================
// GOOGLE ML KIT ON-DEVICE SENSORY PERCEPTION TYPES
// ==========================================

export interface RectBounds {
  readonly left: number;
  readonly top: number;
  readonly right: number;
  readonly bottom: number;
}

export interface MLKitTextElement {
  readonly text: string;
  readonly boundingBox?: RectBounds | null;
  readonly confidence?: number | null;
}

export interface MLKitTextLine {
  readonly text: string;
  readonly boundingBox?: RectBounds | null;
  readonly confidence?: number | null;
  readonly elements?: MLKitTextElement[];
  readonly recognizedLanguage?: string | null;
}

export interface MLKitTextBlock {
  readonly text: string;
  readonly boundingBox?: RectBounds | null;
  readonly lines?: MLKitTextLine[];
  readonly recognizedLanguages?: string[];
}

export interface MLKitTextPayload {
  readonly text: string;
  readonly blocks: MLKitTextBlock[];
  readonly timestampMs?: number;
  readonly frameWidth?: number | null;
  readonly frameHeight?: number | null;
}

export interface MLKitBarcodeItem {
  readonly rawValue: string;
  readonly displayValue?: string | null;
  readonly format: string;
  readonly valueType?: string | null;
  readonly boundingBox?: RectBounds | null;
}

export interface MLKitBarcodePayload {
  readonly barcodes: MLKitBarcodeItem[];
  readonly timestampMs?: number;
  readonly frameWidth?: number | null;
  readonly frameHeight?: number | null;
}

export interface MLKitDetectedObject {
  readonly trackingId?: number | null;
  readonly boundingBox: RectBounds;
  readonly labels: Array<{
    readonly text: string;
    readonly confidence?: number | null;
    readonly index?: number | null;
  }>;
}

export interface MLKitObjectPayload {
  readonly objects: MLKitDetectedObject[];
  readonly timestampMs?: number;
  readonly frameWidth?: number | null;
  readonly frameHeight?: number | null;
}

export interface MLKitPerceptionBundle {
  readonly captureTimestampMs: number;
  readonly sourceApi?: string;
  readonly frameWidth?: number | null;
  readonly frameHeight?: number | null;
  readonly cameraFacing?: 'environment' | 'user' | 'unknown';
  readonly textRecognition?: MLKitTextPayload | null;
  readonly barcodeScanning?: MLKitBarcodePayload | null;
  readonly objectDetection?: MLKitObjectPayload | null;
  readonly latencyMs?: number | null;
  readonly isSimulated?: boolean;
}

export interface StandardizedPerceptionBlock {
  readonly id: string;
  readonly modality: 'OCR_TEXT' | 'BARCODE' | 'OBJECT_DETECTION';
  readonly authoritativeTextBlock: string;
  readonly provenance: Provenance;
  readonly confidence: number | null;
  readonly boundingBox: RectBounds | null;
  readonly timestamp: number;
  readonly metadata: Record<string, any>;
}

export interface StandardizedPerceptionContext {
  readonly contextId: string;
  readonly generatedAtMs: number;
  readonly sourceApi: string;
  readonly frameDimensions: { readonly width: number | null; readonly height: number | null };
  readonly blocks: StandardizedPerceptionBlock[];
  readonly authoritativeContextString: string;
  readonly totalEntitiesDetected: number;
  readonly primaryModality: string;
  readonly provenance: Provenance;
}

export interface PerceptionLoopResult {
  readonly executionId: string;
  readonly goal: string;
  readonly perceptionContextId: string;
  readonly promptInjectedBlock: string;
  readonly actionPlanned: string;
  readonly hardwareOperationResult?: HardwareOperationResult | null;
  readonly verifiedOutcome: 'VERIFIED_SUCCESS' | 'BLOCKED_BY_SAFETY' | 'HARDWARE_REJECTED' | 'DIVERGENCE_RECOVERED' | 'UNSUPPORTED_ON_HOST';
  readonly durationMs: number;
  readonly timestamp: number;
}
