import SwiftUI

internal final class PlaylistHeaderParser: NSObject, XMLParserDelegate {
    var playlistName: String?
    var weight: Int?
    private var didSeeRoot = false
    
    func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String : String] = [:]) {
        if !didSeeRoot {
            didSeeRoot = true
            if elementName == "playlist" {
                playlistName = attributeDict["name"]
                if let wStr = attributeDict["weight"], let w = Int(wStr), w >= 0 {
                    weight = w
                } else {
                    weight = 0
                }
            } else {
                playlistName = nil
                weight = nil
                parser.abortParsing()
            }
        }
    }
}
