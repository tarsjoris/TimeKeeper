import Foundation
import AVFoundation
import Accelerate

public struct WaveHeader {
    public let audioFormat: Int
    public let numChannels: Int
    public let sampleRate: UInt32
    public let bitsPerSample: Int
    public let dataSize: UInt32
}

public let SAMPLES_PER_SECOND: Int = 44100

private let FORMAT_PCM_8BIT: Int = 1
private let FORMAT_FLOAT: Int = 3 // retained for parity, unused

public typealias OpenFile = (_ name: String) -> InputStream?

public final class WaveUtil {
    private let openFile: OpenFile

    public init(openFile: @escaping OpenFile) {
        self.openFile = openFile
    }

    public convenience init(bundle: Bundle = .main) {
        self.init(openFile: { name in
            let url = bundle.url(
                forResource: (name as NSString).deletingPathExtension,
                withExtension: (name as NSString).pathExtension
            )
            guard let url else { return nil }
            return InputStream(url: url)
        })
    }

    // MARK: - Public API
    
    public func generateClickBuffer(
        beepFrequency: Int,
        beepDurationMillis: Int,
        bpm: UInt8,
        divisionCount: UInt8,
        beatCount: UInt8,
        mainVolume: Int,
        divisionFrequency: Int,
        divisionVolume: Int,
        stereo: Bool
    ) -> AVAudioPCMBuffer {
        let divisionCount = (2...7).contains(divisionCount) ? divisionCount : 1
        let beatCount = (1...7).contains(beatCount) ? beatCount : 1
        let samplesPerBeat = SAMPLES_PER_SECOND * 60 / max(Int(bpm), 1)
        let frameCount = UInt32(Int(beatCount) * samplesPerBeat)
        let buffer = makeBuffer(frameCount: frameCount, stereo: stereo)
        guard let channels = buffer.floatChannelData else { return buffer }

        // Silence baseline
        vDSP_vclr(channels[0], 1, vDSP_Length(frameCount))
        if (stereo) {
            vDSP_vclr(channels[1], 1, vDSP_Length(frameCount))
        }

        for beatIndex in 0..<beatCount {
            let isDownbeat = beatIndex == 0
            let freq = isDownbeat ? beepFrequency : divisionFrequency
            let sampleCountOffset = Int(beatIndex) * samplesPerBeat
            writeBeepInBuffer(
                sinesPerSecond: freq,
                beepDurationMillis: beepDurationMillis,
                volume: mainVolume,
                channels: channels,
                sampleCountOffset: sampleCountOffset,
                totalFrames: Int(frameCount),
                stereo: stereo
            )
            if divisionCount > 1 {
                for subDivisionIndex in 1..<divisionCount {
                    let divisionSampleCountOffset = Int(round((Double(samplesPerBeat) / Double(divisionCount)) * Double(subDivisionIndex))) + sampleCountOffset
                    writeBeepInBuffer(
                        sinesPerSecond: divisionFrequency,
                        beepDurationMillis: beepDurationMillis,
                        volume: mainVolume * divisionVolume / 100,
                        channels: channels,
                        sampleCountOffset: divisionSampleCountOffset,
                        totalFrames: Int(frameCount),
                        stereo: stereo
                    )
                }
            }
        }
        return buffer
    }

    public func generateShakerLoopBuffer(
        bpm: UInt8,
        divisionCount: UInt8,
        beatCount: UInt8,
        volume: Int,
        stereo: Bool
    ) -> AVAudioPCMBuffer {
        let divisionCount = (1...4).contains(divisionCount) ? divisionCount : 4
        let samplesPerBeat = SAMPLES_PER_SECOND * 60 / max(Int(bpm), 1)
        let frameCount = UInt32(Int(beatCount) * samplesPerBeat)
        let buffer = makeBuffer(frameCount: frameCount, stereo: stereo)
        guard let channels = buffer.floatChannelData else { return buffer }

        // Silence baseline
        vDSP_vclr(channels[0], 1, vDSP_Length(frameCount))
        if stereo {
            vDSP_vclr(channels[1], 1, vDSP_Length(frameCount))
        }

        let samples = readSamples(prefix: "shakerloop", beats: 4, volume: volume)
        for i in 0..<beatCount {
            for j in 0..<Int(divisionCount) {
                let offsetFrames = Int((Double(i) + Double(j) / Double(divisionCount)) * Double(samplesPerBeat))
                Self.copyBytes(from: samples[safe: j] ?? nil, channels: channels, frameOffset: offsetFrames, totalFrames: Int(frameCount), stereo: stereo)
            }
        }
        return buffer
    }

    public func generateCowbellBuffer(
        bpm: UInt8,
        divisionCount: UInt8,
        beatCount: UInt8,
        mainVolume: Int,
        divisionVolume: Int,
        stereo: Bool
    ) -> AVAudioPCMBuffer {
        let samplesPerBeat = SAMPLES_PER_SECOND * 60 / max(Int(bpm), 1)
        let frameCount = UInt32(Int(beatCount) * samplesPerBeat)
        let buffer = makeBuffer(frameCount: frameCount, stereo: stereo)
        guard let channels = buffer.floatChannelData else { return buffer }

        // Silence baseline
        vDSP_vclr(channels[0], 1, vDSP_Length(frameCount))
        if stereo {
            vDSP_vclr(channels[1], 1, vDSP_Length(frameCount))
        }

        let high = readSample(filename: "high.wav", volume: mainVolume)
        let low = readSample(filename: "low.wav", volume: mainVolume)
        var softerLow = low
        if var s = softerLow { Self.adjustVolume(&s, volumePercentage: divisionVolume); softerLow = s }

        // Downbeat
        Self.copyBytes(from: high, channels: channels, frameOffset: 0, totalFrames: Int(frameCount), stereo: stereo)
        for i in 0..<beatCount {
            for j in 0..<divisionCount {
                let sample: [UInt8]?
                if j == 0 {
                    sample = (i == 0) ? high : low
                } else {
                    sample = softerLow
                }
                let offsetFrames = Int((Double(i) + Double(j) / Double(divisionCount)) * Double(samplesPerBeat))
                Self.copyBytes(from: sample, channels: channels, frameOffset: offsetFrames, totalFrames: Int(frameCount), stereo: stereo)
            }
        }
        return buffer
    }

    public func mixCountOff(
        click: AVAudioPCMBuffer,
        bpm: UInt8,
        beats: UInt8,
        volume: Int,
        stereo: Bool
    ) -> AVAudioPCMBuffer {
        // Create a copy buffer to avoid mutating the input
        let frameCount = click.frameLength
        let out = makeBuffer(frameCount: frameCount, stereo: stereo)
        guard let inCh = click.floatChannelData, let outCh = out.floatChannelData else { return click }
        vDSP_mmov(inCh[0], outCh[0], vDSP_Length(frameCount), 1, 1, 1)
        if stereo {
            vDSP_mmov(inCh[1], outCh[1], vDSP_Length(frameCount), 1, 1, 1)
        }

        let samplesPerBeat = SAMPLES_PER_SECOND * 60 / max(Int(bpm), 1)
        let samples = readSamples(prefix: "countdown", beats: min(Int(beats), 8), volume: volume)
        for i in 0..<Int(beats) {
            let sample = samples[safe: i % max(samples.count, 1)] ?? nil
            let offsetFrames = Int(round(Double(samplesPerBeat * i)))
            Self.mixIn(channels: outCh, sample: sample, frameOffset: offsetFrames, totalFrames: Int(frameCount), stereo: stereo)
        }
        return out
    }

    // MARK: - Private helpers

    private func makeBuffer(frameCount: UInt32, stereo: Bool) -> AVAudioPCMBuffer {
        let format = AVAudioFormat(standardFormatWithSampleRate: Double(SAMPLES_PER_SECOND), channels: stereo ? 2 : 1)!
        let buf = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: frameCount)!
        buf.frameLength = frameCount
        return buf
    }

    private static func byteToFloat(_ b: UInt8) -> Float {
        return Float(Int(b) - 128) / 128.0
    }

    private func writeBeepInBuffer(
        sinesPerSecond: Int,
        beepDurationMillis: Int,
        volume: Int,
        channels: UnsafePointer<UnsafeMutablePointer<Float>>,
        sampleCountOffset: Int,
        totalFrames: Int,
        stereo: Bool
    ) {
        let maxVolume = (1...100).contains(volume) ? Float(volume) / 100.0 : 1.0
        let desiredSampleCount = Float(SAMPLES_PER_SECOND) * Float(beepDurationMillis) / 1000.0
        let samplesBetweenZeroCrossings = Float(SAMPLES_PER_SECOND) / Float(max(sinesPerSecond, 1)) / 2.0
        let actualSampleCount = Int(floor((desiredSampleCount / samplesBetweenZeroCrossings).rounded() * samplesBetweenZeroCrossings))
        if actualSampleCount <= 0 { return }
        let amplitudeStepPerSample = 2.0 * Float.pi * Float(max(sinesPerSecond, 1)) / Float(SAMPLES_PER_SECOND)
        var s = 0
        while s < actualSampleCount {
            let amp = sin(Float(s) * amplitudeStepPerSample)
            let value = amp * maxVolume
            let idx = sampleCountOffset + s
            if idx >= 0 && idx < totalFrames {
                channels[0][idx] = value
                if stereo { channels[1][idx] = value }
            }
            s += 1
        }
    }

    private static func copyBytes(
        from: [UInt8]?,
        channels: UnsafePointer<UnsafeMutablePointer<Float>>,
        frameOffset: Int,
        totalFrames: Int,
        stereo: Bool
    ) {
        guard let from = from else { return }
        let sampleFrames = from.count / 2
        var f = 0
        if stereo {
            while f < sampleFrames {
                let l8 = byteToFloat(from[2 * f])
                let r8 = byteToFloat(from[2 * f + 1])
                let idx = frameOffset + f
                if idx >= 0 && idx < totalFrames {
                    channels[0][idx] = l8
                    channels[1][idx] = r8
                }
                f += 1
            }
        } else {
            while f < sampleFrames {
                let l8 = byteToFloat(from[2 * f])
                let idx = frameOffset + f
                if idx >= 0 && idx < totalFrames {
                    channels[0][idx] = l8
                }
                f += 1
            }
        }
    }

    private static func mixIn(
        channels: UnsafePointer<UnsafeMutablePointer<Float>>,
        sample: [UInt8]?,
        frameOffset: Int,
        totalFrames: Int,
        stereo: Bool
    ) {
        guard let sample = sample else { return }
        let sampleFrames = sample.count / 2
        var f = 0
        if stereo {
            while f < sampleFrames {
                let l8 = byteToFloat(sample[2 * f])
                let r8 = byteToFloat(sample[2 * f + 1])
                let idx = frameOffset + f
                if idx >= 0 && idx < totalFrames {
                    channels[0][idx] = max(min(channels[0][idx] + l8, 1.0), -1.0)
                    channels[1][idx] = max(min(channels[1][idx] + r8, 1.0), -1.0)
                }
                f += 1
            }
        } else {
            while f < sampleFrames {
                let l8 = byteToFloat(sample[2 * f])
                let idx = frameOffset + f
                if idx >= 0 && idx < totalFrames {
                    channels[0][idx] = max(min(channels[0][idx] + l8, 1.0), -1.0)
                }
                f += 1
            }
        }
    }

    private func writeBeepInBuffer(
        sinesPerSecond: Int,
        beepDurationMillis: Int,
        volume: Int,
        buffer: inout [UInt8],
        sampleCountOffset: Int,
        stereo: Bool
    ) {
        let maxVolume = (1...100).contains(volume) ? Double(volume) / 100.0 : 1.0
        let desiredSampleCount = Double(SAMPLES_PER_SECOND) * Double(beepDurationMillis) / 1000.0
        let samplesBetweenZeroCrossings = Double(SAMPLES_PER_SECOND) / Double(max(sinesPerSecond, 1)) / 2.0
        let actualSampleCount = Int(floor((desiredSampleCount / samplesBetweenZeroCrossings).rounded() * samplesBetweenZeroCrossings))
        let amplitudeStepPerSample = 2.0 * Double.pi * Double(max(sinesPerSecond, 1)) / Double(SAMPLES_PER_SECOND)
        if actualSampleCount <= 0 { return }
        for s in 0..<actualSampleCount {
            let amplitude = sin(Double(s) * amplitudeStepPerSample)
            let volumeAdjustedAmplitude = amplitude * maxVolume
            let value = (volumeAdjustedAmplitude + 1.0) * 255.0 / 2.0
            let byteVal = UInt8(clamping: Int(value.rounded()))
            let index = (sampleCountOffset + s) * 2
            if buffer.indices.contains(index) { buffer[index] = byteVal }
            if stereo {
                let idxR = index + 1
                if buffer.indices.contains(idxR) { buffer[idxR] = byteVal }
            }
        }
    }

    private func readSamples(prefix: String, beats: Int, volume: Int) -> [[UInt8]?] {
        return (0..<beats).map { i in
            return readSample(filename: "\(prefix)\(i+1).wav", volume: volume)
        }
    }

    private func readSample(filename: String, volume: Int) -> [UInt8]? {
        guard let input = openFile(filename) else { return nil }
        input.open()
        defer { input.close() }
        do {
            let header = try Self.readHeader(input: input)
            guard header.audioFormat == FORMAT_PCM_8BIT else { throw WaveError.unsupported("Only 8bit PCM is supported") }
            guard header.bitsPerSample == 8 else { throw WaveError.unsupported("Only 8bit PCM is supported") }
            guard header.sampleRate == UInt32(SAMPLES_PER_SECOND) else { throw WaveError.unsupported("Only \(SAMPLES_PER_SECOND) samples per second are supported") }
            guard header.numChannels == 2 else { throw WaveError.unsupported("Only 2 channels are supported") }
            var buffer = [UInt8](repeating: 0, count: Int(header.dataSize))
            let readCount = input.read(&buffer, maxLength: buffer.count)
            if readCount > 0 && readCount < buffer.count {
                buffer.removeSubrange(readCount..<buffer.count)
            }
            Self.adjustVolume(&buffer, volumePercentage: volume)
            return buffer
        } catch {
            return nil
        }
    }

    // MARK: - Static utilities (parity)

    public enum WaveError: Error { case malformed(String); case unsupported(String) }

    public static func readHeader(input: InputStream) throws -> WaveHeader {
        func readWord(_ input: InputStream) throws -> String {
            var bytes = [UInt8](repeating: 0, count: 4)
            let n = input.read(&bytes, maxLength: 4)
            guard n == 4 else { throw WaveError.malformed("Unexpected EOF while reading word") }
            return String(bytes: bytes, encoding: .ascii) ?? "\u{0}\u{0}\u{0}\u{0}"
        }
        func read4(_ input: InputStream) throws -> UInt32 {
            var b = [UInt8](repeating: 0, count: 4)
            let n = input.read(&b, maxLength: 4)
            guard n == 4 else { throw WaveError.malformed("Unexpected EOF while reading 4 bytes") }
            return UInt32(b[0]) | (UInt32(b[1]) << 8) | (UInt32(b[2]) << 16) | (UInt32(b[3]) << 24)
        }
        func read2(_ input: InputStream) throws -> Int {
            var b = [UInt8](repeating: 0, count: 2)
            let n = input.read(&b, maxLength: 2)
            guard n == 2 else { throw WaveError.malformed("Unexpected EOF while reading 2 bytes") }
            return Int(UInt32(b[0]) | (UInt32(b[1]) << 8))
        }
        func skip(_ input: InputStream, count: Int) throws {
            var remaining = count
            var buf = [UInt8](repeating: 0, count: min(4096, remaining))
            while remaining > 0 {
                let n = input.read(&buf, maxLength: min(buf.count, remaining))
                if n <= 0 { throw WaveError.malformed("Unexpected EOF while skipping bytes") }
                remaining -= n
            }
        }

        guard try readWord(input) == "RIFF" else { throw WaveError.malformed("Expected 'RIFF'") }
        _ = try read4(input) // ChunkSize
        guard try readWord(input) == "WAVE" else { throw WaveError.malformed("Expected 'WAVE'") }
        var word = try readWord(input)
        while word != "fmt " {
            let chunkSize = try read4(input)
            try skip(input, count: Int(chunkSize))
            word = try readWord(input)
        }
        let fmtSize = try read4(input)
        guard fmtSize == 16 else { throw WaveError.malformed("Expected fmt size 16") }
        let audioFormat = try read2(input)
        let numChannels = try read2(input)
        let sampleRate = try read4(input)
        _ = try read4(input) // ByteRate
        _ = try read2(input) // BlockAlign
        let bitsPerSample = try read2(input)
        var word2 = try readWord(input)
        while word2 != "data" {
            let chunkSize = try read4(input)
            try skip(input, count: Int(chunkSize))
            word2 = try readWord(input)
        }
        let dataSize = try read4(input)
        return WaveHeader(audioFormat: audioFormat, numChannels: numChannels, sampleRate: sampleRate, bitsPerSample: bitsPerSample, dataSize: dataSize)
    }

    private static func adjustVolume(_ input: inout [UInt8], volumePercentage: Int) {
        guard volumePercentage < 100 else { return }
        for i in 0..<input.count {
            let amplitude = Int(input[i]) - 128
            let adjusted = Int((Double(amplitude) * Double(volumePercentage) / 100.0).rounded())
            let final = UInt8(clamping: adjusted + 128)
            input[i] = final
        }
    }

    private static func copyBytes(from: [UInt8]?, to: inout [UInt8], bufferOffset: Int, stereo: Bool) {
        guard let from = from else { return }
        let indices: [Int]
        if stereo {
            indices = Array(from.indices)
        } else {
            indices = from.indices.filter { $0 % 2 == 0 }
        }
        for index in indices {
            let targetIndex = bufferOffset + index
            if to.indices.contains(targetIndex) {
                to[targetIndex] = from[index]
            }
        }
    }

    private static func mixIn(click: inout [UInt8], sample: [UInt8]?, offset: Int, stereo: Bool) {
        guard let sample = sample, offset >= 0, offset < click.count else { return }
        let step = stereo ? 1 : 2
        let upperExclusive = min(click.count, offset + sample.count)
        var i = offset
        while i < upperExclusive {
            let a = Int(click[i]) - 128
            let b = Int(sample[i - offset]) - 128
            let c = a + b
            let clipped = max(min(c, 127), -128)
            click[i] = UInt8(clamping: clipped + 128)
            i += step
        }
    }
}

// MARK: - Safe index helper
private extension Array {
    subscript(safe index: Int) -> Element? {
        guard indices.contains(index) else { return nil }
        return self[index]
    }
}
