import Foundation
import AVFoundation
import SwiftUI
import Combine
import MediaPlayer

final class SoundService: ObservableObject {
    private let engine = AVAudioEngine()
    private var player :AVAudioPlayerNode? = nil
    private var audioPlayer: AVAudioPlayer? = nil

    // Configuration
    var bpm: Int = 120
    var divisionCount: Int = 2
    var beatCount: Int = 4
    var mainVolume: Int = 100
    var divisionVolume: Int = 60
    var stereo: Bool = true

    init() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            // ok
        }
    }
    
    func start(song: SongData, stereo: Bool) {
        stop()

        do {
            setupNowPlaying(name: song.name)
            let track = stereo ? song.stereoTrackPath : song.monoTrackPath
            if track != nil, let t = track {
                try playTrack(track: t)
            } else {
                try playClick(song: song, stereo: stereo)
            }
        } catch {
            print("SoundService start error: \(error)")
        }
    }
    
    private func playTrack(track: String) throws {
        let urls = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        guard let documentsURL = urls.first else {
            print("Documents directory not found")
            return
        }
        let playlistFolder = documentsURL.appendingPathComponent("tracks")
        guard let filename = SoundService.extractFileName(from: track) else {
            print("Could not extract filename")
            return
        }
        let file = playlistFolder.appending(component: filename)
        if !FileManager.default.fileExists(atPath: file.path(percentEncoded: false)) {
            print("File not found \(file)")
            return
        }
        
        let pl = try AVAudioPlayer(contentsOf: file)
        pl.prepareToPlay()
        pl.play()
        audioPlayer = pl
    }
    
    private static func extractFileName(from urlString: String) -> String? {
        guard let url = URL(string: urlString) else {
            return nil
        }
        let encodedLastComponent = url.lastPathComponent
        guard let decoded = encodedLastComponent.removingPercentEncoding else {
            return nil
        }
        return URL(fileURLWithPath: decoded).lastPathComponent
    }
    
    private func playClick(song: SongData, stereo: Bool) throws {
        let pl = try createBufferPlayer(stereo: stereo)
        let waveUtil = WaveUtil()
        let buffer = createClickBuffer(waveUtil: waveUtil, song: song, stereo: stereo)
        if song.countOff {
            if (song.twoBarCountOff && song.beatCount == 4) {
                pl.scheduleBuffer(waveUtil.mixCountOff(click: buffer, bpm: song.tempo / UInt8(2), beats: 2, volume: 60, stereo: stereo))
            }
            pl.scheduleBuffer(waveUtil.mixCountOff(click: buffer, bpm: song.tempo, beats: song.beatCount, volume: 60, stereo: stereo))
        }
        pl.scheduleBuffer(buffer, at: nil, options: [.loops], completionHandler: nil)
        pl.play()
    }
    
    private func createBufferPlayer(stereo: Bool) throws -> AVAudioPlayerNode {
        let pl = AVAudioPlayerNode()
        engine.attach(pl)
        let format = AVAudioFormat(standardFormatWithSampleRate: Double(SAMPLES_PER_SECOND), channels: stereo ? 2 : 1)!
        engine.connect(pl, to: engine.mainMixerNode, format: format)
        if !engine.isRunning {
            try engine.start()
        }
        
        player = pl
        return pl
    }
    
    private func createClickBuffer(waveUtil: WaveUtil, song: SongData, stereo: Bool) -> AVAudioPCMBuffer {
        switch (song.clickType) {
        case SoundService.TYPE_SINE:
            return waveUtil.generateClickBuffer(
            beepFrequency: 1760,
            beepDurationMillis: 20,
            bpm: song.tempo,
            divisionCount: song.divisionCount,
            beatCount: song.beatCount,
            mainVolume: 100,
            divisionFrequency: 880,
            divisionVolume: 40,
            stereo: stereo
            )
        case SoundService.TYPE_SHAKER:
            return waveUtil.generateShakerLoopBuffer(
                bpm: song.tempo,
                divisionCount: song.divisionCount,
                beatCount: song.beatCount,
                volume: 100,
                stereo: stereo
            )
        default:
            return waveUtil.generateCowbellBuffer(
                bpm: song.tempo,
                divisionCount: song.divisionCount,
                beatCount: song.beatCount,
                mainVolume: 100,
                divisionVolume: 60,
                stereo: stereo
                )
        }
    }
    
    private func setupNowPlaying(name: String) {
        MPNowPlayingInfoCenter.default().nowPlayingInfo = [
            MPMediaItemPropertyTitle: name,
            MPMediaItemPropertyArtist: "TimeKeeper"
        ]
    }

    func stop() {
        player?.stop()
        player = nil
        engine.pause()
        audioPlayer?.stop()
        audioPlayer = nil
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
    }
    
    private static let TYPE_COWBELL : UInt8 = 1
    private static let TYPE_SINE : UInt8 = 2
    private static let TYPE_SHAKER : UInt8 = 3
}
