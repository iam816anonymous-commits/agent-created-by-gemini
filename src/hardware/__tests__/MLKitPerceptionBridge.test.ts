import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { MLKitPerceptionBridge, mlKitPerceptionBridge } from '../MLKitPerceptionBridge';
import { unifiedHardwareController } from '../UnifiedHardwareController';
import { perceptionExecutionLoopRunner } from '../PerceptionExecutionLoopRunner';
import {
  MLKitPerceptionBundle,
  MLKitTextPayload,
  MLKitBarcodePayload,
  MLKitObjectPayload
} from '../types';

describe('MLKitPerceptionBridge Unit & Zero-Fabrication Tests', () => {
  let bridge: MLKitPerceptionBridge;

  beforeEach(() => {
    bridge = new MLKitPerceptionBridge();
  });

  it('converts ML Kit OCR text recognition into authoritative blocks with MEASURED provenance', () => {
    const textPayload: MLKitTextPayload = {
      text: 'Total: $42.50\nConfirm & Pay',
      blocks: [
        {
          text: 'Total: $42.50\nConfirm & Pay',
          boundingBox: { left: 40, top: 120, right: 680, bottom: 260 },
          lines: [
            {
              text: 'Total: $42.50',
              confidence: 0.96,
              recognizedLanguage: 'en',
              boundingBox: { left: 40, top: 120, right: 400, bottom: 180 },
              elements: [
                { text: 'Total:', confidence: 0.97, boundingBox: { left: 40, top: 120, right: 180, bottom: 180 } },
                { text: '$42.50', confidence: 0.95, boundingBox: { left: 200, top: 120, right: 400, bottom: 180 } }
              ]
            },
            {
              text: 'Confirm & Pay',
              confidence: 0.92,
              recognizedLanguage: 'en',
              boundingBox: { left: 40, top: 200, right: 680, bottom: 260 }
            }
          ]
        }
      ],
      timestampMs: 1730000000000
    };

    const blocks = bridge.extractTextBlocks(textPayload);
    expect(blocks.length).toBe(2);

    // Block 1 assertions
    expect(blocks[0].modality).toBe('OCR_TEXT');
    expect(blocks[0].provenance).toBe('MEASURED');
    expect(blocks[0].confidence).toBe(0.96);
    expect(blocks[0].metadata.rawText).toBe('Total: $42.50');
    expect(blocks[0].authoritativeTextBlock).toContain('[SENSORY_PERCEPTION:OCR_TEXT | PROVENANCE=MEASURED | CONF=0.96 | LANG=en | BOUNDS=Rect(40, 120 - 400, 180)]');
    expect(blocks[0].authoritativeTextBlock).toContain('TEXT: "Total: $42.50"');

    // Block 2 assertions
    expect(blocks[1].metadata.rawText).toBe('Confirm & Pay');
    expect(blocks[1].provenance).toBe('MEASURED');
  });

  it('converts ML Kit Barcode scanning into authoritative blocks with format and type metadata', () => {
    const barcodePayload: MLKitBarcodePayload = {
      barcodes: [
        {
          rawValue: 'https://airport.terminal/gate/B14',
          displayValue: 'https://airport.terminal/gate/B14',
          format: 'QR_CODE',
          valueType: 'URL',
          boundingBox: { left: 100, top: 300, right: 400, bottom: 600 }
        }
      ],
      timestampMs: 1730000001000
    };

    const blocks = bridge.extractBarcodeBlocks(barcodePayload);
    expect(blocks.length).toBe(1);
    expect(blocks[0].modality).toBe('BARCODE');
    expect(blocks[0].provenance).toBe('MEASURED');
    expect(blocks[0].confidence).toBeNull(); // barcodes have no floating point confidence, never fabricated
    expect(blocks[0].authoritativeTextBlock).toContain('[SENSORY_PERCEPTION:BARCODE | PROVENANCE=MEASURED | FORMAT=QR_CODE | TYPE=URL | BOUNDS=Rect(100, 300 - 400, 600)]');
    expect(blocks[0].authoritativeTextBlock).toContain('VALUE: "https://airport.terminal/gate/B14"');
  });

  it('converts ML Kit Object Detection into authoritative blocks with tracking ID and bounds', () => {
    const objectPayload: MLKitObjectPayload = {
      objects: [
        {
          trackingId: 104,
          boundingBox: { left: 80, top: 220, right: 540, bottom: 700 },
          labels: [
            { text: 'Package (Fragile)', confidence: 0.89, index: 1 }
          ]
        }
      ],
      timestampMs: 1730000002000
    };

    const blocks = bridge.extractObjectBlocks(objectPayload);
    expect(blocks.length).toBe(1);
    expect(blocks[0].modality).toBe('OBJECT_DETECTION');
    expect(blocks[0].provenance).toBe('MEASURED');
    expect(blocks[0].confidence).toBe(0.89);
    expect(blocks[0].metadata.trackingId).toBe(104);
    expect(blocks[0].authoritativeTextBlock).toContain('[SENSORY_PERCEPTION:OBJECT_DETECTION | PROVENANCE=MEASURED | LABEL="Package (Fragile)" | CONF=0.89 | TRACK_ID=104 | BOUNDS=Rect(80, 220 - 540, 700)]');
  });

  it('preserves simulated test bundles as ESTIMATED provenance (Zero Fabrication)', () => {
    const bundle: MLKitPerceptionBundle = {
      captureTimestampMs: Date.now(),
      isSimulated: true,
      textRecognition: {
        text: 'Simulated Preview Screen',
        blocks: [
          {
            text: 'Simulated Preview Screen',
            lines: [{ text: 'Simulated Preview Screen', confidence: 0.85 }]
          }
        ]
      }
    };

    const context = bridge.processBundle(bundle);
    expect(context.provenance).toBe('ESTIMATED');
    expect(context.blocks[0].provenance).toBe('ESTIMATED');
    expect(context.authoritativeContextString).toContain('Provenance: ESTIMATED');
  });

  it('handles empty perception frame gracefully with UNAVAILABLE provenance', () => {
    const bundle: MLKitPerceptionBundle = {
      captureTimestampMs: Date.now(),
      textRecognition: { text: '', blocks: [] },
      barcodeScanning: { barcodes: [] },
      objectDetection: { objects: [] }
    };

    const context = bridge.processBundle(bundle);
    expect(context.totalEntitiesDetected).toBe(0);
    expect(context.provenance).toBe('UNAVAILABLE');
    expect(context.authoritativeContextString).toContain('[PERCEPTION: EMPTY_FRAME | PROVENANCE=UNAVAILABLE]');
  });
});

describe('UnifiedHardwareController Perception Integration Tests', () => {
  beforeEach(() => {
    unifiedHardwareController.resetEmergencyStop();
  });

  afterEach(() => {
    unifiedHardwareController.dispose();
  });

  it('ingests perception bundle, updates state, and dispatches to observer listeners', () => {
    let capturedContext: any = null;
    const unsub = unifiedHardwareController.onPerceptionContextChange((ctx) => {
      capturedContext = ctx;
    });

    const bundle: MLKitPerceptionBundle = {
      captureTimestampMs: Date.now(),
      frameWidth: 720,
      frameHeight: 1440,
      cameraFacing: 'environment',
      textRecognition: {
        text: 'Flight Gate B14',
        blocks: [
          {
            text: 'Flight Gate B14',
            lines: [{ text: 'Flight Gate B14', confidence: 0.99 }]
          }
        ]
      }
    };

    const ctx = unifiedHardwareController.ingestPerceptionBundle(bundle);
    expect(ctx).toBeDefined();
    expect(ctx.totalEntitiesDetected).toBe(1);
    expect(capturedContext).toBe(ctx);
    expect(unifiedHardwareController.getLatestPerceptionContext()).toBe(ctx);

    unsub();
  });

  it('executes PERCEPTION_CAPTURE hardware action through mutex serialization', async () => {
    const bundle: MLKitPerceptionBundle = {
      captureTimestampMs: Date.now(),
      textRecognition: {
        text: 'Card Number: 4111',
        blocks: [{ text: 'Card Number: 4111', lines: [{ text: 'Card Number: 4111', confidence: 0.91 }] }]
      }
    };

    const result = await unifiedHardwareController.executeHardwareAction({
      actuatorId: 'actuator-camera',
      actionType: 'PERCEPTION_CAPTURE',
      params: { perceptionBundle: bundle }
    });

    expect(result.status).toBe('SUCCESS_VERIFIED');
    expect(result.verifiedPhysicalState).toBe(true);
    expect(result.details?.entitiesCount).toBe(1);
  });

  it('blocks perception capture when emergency stop is active', async () => {
    unifiedHardwareController.emergencyStop('Emergency Test Barrier');

    const result = await unifiedHardwareController.executeHardwareAction({
      actuatorId: 'actuator-camera',
      actionType: 'PERCEPTION_CAPTURE',
      params: {}
    });

    expect(result.status).toBe('EMERGENCY_STOP_ACTIVE');
    expect(result.verifiedPhysicalState).toBe(false);
  });
});

describe('PerceptionExecutionLoopRunner Integration Tests', () => {
  beforeEach(() => {
    unifiedHardwareController.resetEmergencyStop();
  });

  afterEach(() => {
    unifiedHardwareController.dispose();
  });

  it('runs complete 4-phase loop cycle with ML Kit OCR perception and verified action', async () => {
    const bundle: MLKitPerceptionBundle = {
      captureTimestampMs: Date.now(),
      frameWidth: 1080,
      frameHeight: 2400,
      textRecognition: {
        text: 'Pay Now $18.50',
        blocks: [
          {
            text: 'Pay Now $18.50',
            lines: [{ text: 'Pay Now $18.50', confidence: 0.95 }]
          }
        ]
      }
    };

    const loopResult = await perceptionExecutionLoopRunner.runPerceptionCycle(
      'Verify price and confirm payment',
      'com.example.checkout',
      bundle,
      {
        actuatorId: 'actuator-camera',
        actionType: 'PERCEPTION_CAPTURE'
      }
    );

    expect(loopResult).toBeDefined();
    expect(loopResult.verifiedOutcome).toBe('VERIFIED_SUCCESS');
    expect(loopResult.promptInjectedBlock).toContain('Pay Now $18.50');
    expect(loopResult.hardwareOperationResult).toBeDefined();
    expect(loopResult.hardwareOperationResult?.status).toBe('SUCCESS_VERIFIED');

    const session = perceptionExecutionLoopRunner.getActiveSession();
    expect(session).toBeDefined();
    expect(session?.steps.length).toBe(4);
    expect(session?.steps.map(s => s.phase)).toEqual(['PLAN', 'ACT', 'REFLECT', 'REMEMBER']);
  });

  it('truthfully handles unsupported actuators as UNSUPPORTED_ON_HOST without fabricating execution', async () => {
    const bundle: MLKitPerceptionBundle = {
      captureTimestampMs: Date.now(),
      barcodeScanning: {
        barcodes: [{ rawValue: 'ORDER-99120', format: 'QR_CODE' }]
      }
    };

    const loopResult = await perceptionExecutionLoopRunner.runPerceptionCycle(
      'Scan ticket barcode and trigger physical actuator',
      'com.example.tickets',
      bundle,
      {
        actuatorId: 'vibrator',
        actionType: 'HAPTIC_TICK'
      }
    );

    expect(loopResult).toBeDefined();
    expect(loopResult.verifiedOutcome).toBe('UNSUPPORTED_ON_HOST');
    expect(loopResult.hardwareOperationResult?.status).toBe('UNSUPPORTED');
  });
});
