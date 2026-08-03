import Foundation

struct DropboxListFolderResponse: Decodable {
    let entries: [DropboxEntry]
    let cursor: String
    let has_more: Bool
}

struct DropboxEntry: Decodable {
    let tag: String?
    let name: String
    let path_lower: String?
    let path_display: String?
    let id: String?
    let client_modified: String?
    let server_modified: String?
    let size: UInt64?

    enum CodingKeys: String, CodingKey {
        case tag = ".tag"
        case name
        case path_lower
        case path_display
        case id
        case client_modified
        case server_modified
        case size
    }
}
