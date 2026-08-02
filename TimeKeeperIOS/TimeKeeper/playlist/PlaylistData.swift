import Foundation

internal struct PlaylistHeader: Identifiable {
    let id: UInt64
    let name: String
    let weight: Int
    let lastModified: Date?
}

internal struct PlaylistData {
    var name: String
    let stereo: Bool
    let announceTitle: Bool
    let songs: [SongData]
}

internal struct SongData: Identifiable {
    let id: UInt8
    let name: String
    let tempo: UInt8
    let countOff: Bool
    let twoBarCountOff: Bool
    let clickType: UInt8
    let divisionCount: UInt8
    let beatCount: UInt8
    let stereoTrackPath: String?
    let monoTrackPath: String?
}
