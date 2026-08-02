import SwiftUI

internal func loadPlaylists() -> [PlaylistHeader] {
    do {
        let folder = try ensurePlaylistsFolder()
        let urls = try FileManager.default.contentsOfDirectory(at: folder, includingPropertiesForKeys: nil, options: [])
        let xmlFiles = urls.filter { $0.pathExtension.lowercased() == "xml" }
        var entries: [PlaylistHeader] = []
        for url in xmlFiles {
            if let parsed = parsePlaylistHeader(from: url) {
                entries.append(parsed)
            }
        }
        let playlists = entries.sorted { $0.weight > $1.weight }
        return playlists
    } catch {
        print("Failed to load playlists: \(error)")
        return []
    }
}

private func parsePlaylistHeader(from url: URL) -> PlaylistHeader? {
    let stem = url.deletingPathExtension().lastPathComponent
    guard let fileID = UInt64(stem) else { return nil }
    guard let parser = XMLParser(contentsOf: url) else { return nil }
    let delegate = PlaylistHeaderParser()
    parser.delegate = delegate
    if parser.parse(),
       let name = delegate.playlistName,
       let weight = delegate.weight {
        let lastModified = (try? FileManager.default.attributesOfItem(atPath: url.path)[.modificationDate] as? Date) ?? nil
        return PlaylistHeader(id: fileID, name: name, weight: weight, lastModified: lastModified)
    } else {
        return nil
    }
}

internal func loadPlaylist(_ id: UInt64) -> PlaylistData? {
    do {
        let folder = try ensurePlaylistsFolder()
        let file = folder.appending(component: "\(id).xml")
        guard let parser = XMLParser(contentsOf: file) else { return nil }
        let delegate = PlaylistParser()
        parser.delegate = delegate
        if parser.parse(),
           let name = delegate.playlistName {
            return PlaylistData(name: name, stereo: delegate.stereo ?? true, announceTitle: delegate.announceTitle ?? false, songs: delegate.songs)
        } else {
            return nil
        }
    } catch {
        print("Failed to load playlists: \(error)")
        return nil
    }
}

private func ensurePlaylistsFolder() throws -> URL {
    let urls = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
    guard let documentsURL = urls.first else {
        throw NSError(domain: "PlaylistListView", code: 1, userInfo: [NSLocalizedDescriptionKey: "Documents directory not found"])
    }
    let playlistFolder = documentsURL.appendingPathComponent("playlists")
    if !FileManager.default.fileExists(atPath: playlistFolder.path) {
        try FileManager.default.createDirectory(at: playlistFolder, withIntermediateDirectories: true)
    }
    return playlistFolder
}

internal func savePlaylist(
    data: Data,
    fileName: String
) {
    do {
        let folder = try ensurePlaylistsFolder()
        let fileURL = folder.appendingPathComponent(fileName)

        try data.write(to: fileURL, options: .atomic)
    } catch {
        print("Failed to save playlist: \(error)")
    }
}

internal func deletePlaylist(
    fileName: String
) {
    do {
        let folder = try ensurePlaylistsFolder()
        let fileURL = folder.appendingPathComponent(fileName)
        
        try FileManager.default.removeItem(at: fileURL)
    } catch {
        print("Failed to delete playlist: \(error)")
    }
}
