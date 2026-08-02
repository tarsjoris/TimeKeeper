import SwiftUI

internal final class PlaylistParser: NSObject, XMLParserDelegate {
    var playlistName: String?
    var stereo: Bool?
    var announceTitle: Bool?
    var songs: [SongData] = []
    var songIndex: UInt8 = 0
    
    func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String : String] = [:]) {
        if elementName == "playlist" {
            playlistName = attributeDict["name"]
            stereo = attributeDict["stereo"] != "false"
            announceTitle = attributeDict["announce_title"] == "true"
        } else if elementName == "song" {
            songs.append(SongData(
                id: songIndex,
                name: attributeDict["name"] ?? "<no name>",
                tempo: PlaylistParser.parseInt(attributeDict["tempo"]) ?? 120,
                countOff: attributeDict["count_off"] == "true",
                twoBarCountOff: attributeDict["two_bar_count_off"] == "true",
                clickType: PlaylistParser.parseInt(attributeDict["click_type"]) ?? 1,
                divisionCount: PlaylistParser.parseInt(attributeDict["division_count"]) ?? 1,
                beatCount: PlaylistParser.parseInt(attributeDict["beat_count"]) ?? 1,
                stereoTrackPath: attributeDict["stereo_track_path"],
                monoTrackPath: attributeDict["mono_track_path"]
            ))
            songIndex += 1
        }
    }
    
    private static func parseInt(_ string: String?) -> UInt8? {
        guard let safeString = string else { return nil }
        return UInt8(safeString)
    }
}
