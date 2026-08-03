import Foundation

func syncDropBox() async throws {
    print("Syncing items ...")
    
    let remoteNames = try await downloadFiles()
    deleteObsoleteFiles(remoteNames: remoteNames)
    
    print("Done")
}

private func downloadFiles() async throws -> [String] {
    var remoteNames: [String] = []
    let accessToken = try await getAccessToken()
    var listing = try await listFolder(accessToken: accessToken, path: "/")
    while true {
        for e in listing.entries {
            remoteNames.append(e.name)
            try await handleFile(accessToken: accessToken, dropboxEntry: e)
        }
        if (listing.has_more) {
            listing = try await listFolderContinue(accessToken: accessToken, cursor: listing.cursor)
        } else {
            break
        }
    }
    return remoteNames
}

private func listFolder(
    accessToken: String,
    path: String
) async throws -> DropboxListFolderResponse {
    guard let url = URL(string: "https://api.dropboxapi.com/2/files/list_folder") else { throw URLError(.badURL) }
    let body: [String: Any] = [
        "path": path,
        "recursive": true,
        "include_deleted": false
    ]
    return try await performListRequest(accessToken: accessToken, url: url, body: body)
}

private func listFolderContinue(
    accessToken: String,
    cursor: String
) async throws -> DropboxListFolderResponse {
    guard let url = URL(string: "https://api.dropboxapi.com/2/files/list_folder/continue") else { throw URLError(.badURL) }
    let body: [String: Any] = [
        "cursor": cursor
    ]
    return try await performListRequest(accessToken: accessToken, url: url, body: body)
}

private func performListRequest(
    accessToken: String,
    url: URL,
    body: [String: Any]
) async throws -> DropboxListFolderResponse {
    var request = URLRequest(url: url)
    request.httpMethod = "POST"

    request.setValue(
        "Bearer \(accessToken)",
        forHTTPHeaderField: "Authorization"
    )

    request.setValue(
        "application/json",
        forHTTPHeaderField: "Content-Type"
    )

    request.httpBody = try JSONSerialization.data(withJSONObject: body)

    let (data, response) = try await URLSession.shared.data(for: request)

    guard let http = response as? HTTPURLResponse,
          http.statusCode == 200 else {
        print(String(data: data, encoding: .utf8))
        throw URLError(.badServerResponse)
    }
    
    //print(String(data: data, encoding: .utf8))
    
    return try JSONDecoder().decode(
        DropboxListFolderResponse.self,
        from: data
    )
}

private func handleFile(
    accessToken: String,
    dropboxEntry: DropboxEntry
) async throws {
    //print("Checking file \(dropboxEntry.name) ...")
    if dropboxEntry.tag != "file" {
        print("Tag was \(dropboxEntry.tag ?? "nil") for \(dropboxEntry.name)")
        return
    }
    guard let remoteSize =  dropboxEntry.size else {
        print("Size was nil for \(dropboxEntry.name)")
        return
    }
    
    let localInfo = getLocalPlaylistFileInfo(filename: dropboxEntry.name)
    
    if isSizeDifferent(remoteSize: remoteSize, localSize: localInfo.size) ||
        isNewerServerDate(remoteLastModified: dropboxEntry.server_modified, localLastModified: localInfo.modified) {
        try await downloadFile(accessToken: accessToken, dropboxEntry: dropboxEntry)
    }
}

private func isSizeDifferent(remoteSize: UInt64, localSize: UInt64?) -> Bool {
    return localSize == nil || localSize != remoteSize
}

private func isNewerServerDate(remoteLastModified: String?, localLastModified: Date?) -> Bool {
    if let remoteLastModified,
       let serverModified = ISO8601DateFormatter().date(from: remoteLastModified) {
        if let localLastModified {
            //print("Local last modified \(localLastModified); remote last modified \(serverModified)")
            return serverModified > localLastModified
        }
    }
    return true
}

private func downloadFile(
    accessToken: String,
    dropboxEntry: DropboxEntry
) async throws {
    print("Downloading file \(dropboxEntry.name) ...")
    if dropboxEntry.path_display != nil, let p = dropboxEntry.path_display {
        let data = try await getFileContents(accessToken: accessToken, dropboxPath: p)
        savePlaylist(data: data, fileName: dropboxEntry.name)
    }
}

private func getFileContents(
    accessToken: String,
    dropboxPath: String
) async throws -> Data {
    let url = URL(
        string: "https://content.dropboxapi.com/2/files/download"
    )!

    var request = URLRequest(url: url)
    request.httpMethod = "POST"

    request.setValue(
        "Bearer \(accessToken)",
        forHTTPHeaderField: "Authorization"
    )

    request.setValue(
        "{\"path\":\"\(dropboxPath)\"}",
        forHTTPHeaderField: "Dropbox-API-Arg"
    )

    let (data, response) = try await URLSession.shared.data(for: request)

    guard let http = response as? HTTPURLResponse,
          http.statusCode == 200 else {
        throw URLError(.badServerResponse)
    }

    return data
}
