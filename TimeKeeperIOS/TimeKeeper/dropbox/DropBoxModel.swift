import Foundation

struct DropboxListFolderResponse: Decodable {
    let entries: [DropboxEntry]
    let cursor: String
    let has_more: Bool
}

struct DropboxEntry: Decodable {
    let name: String
    let path_lower: String?
    let path_display: String?
    let id: String?

    enum CodingKeys: String, CodingKey {
        case name
        case path_lower
        case path_display
        case id
    }
}
