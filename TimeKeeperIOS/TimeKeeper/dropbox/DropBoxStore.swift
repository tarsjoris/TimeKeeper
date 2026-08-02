import Foundation

func syncDropBox() async throws {
    print("Syncing items ...")
    let accessToken = try await getAccessToken()
    var listing = try await listFolder(accessToken: accessToken, path: "/")
    while true {
        for e in listing.entries {
            try await downloadFile(accessToken: accessToken, dropboxEntry: e)
        }
        if (listing.has_more) {
            listing = try await listFolderContinue(accessToken: accessToken, cursor: listing.cursor)
        } else {
            return
        }
    }
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
    
    return try JSONDecoder().decode(
        DropboxListFolderResponse.self,
        from: data
    )
}

private func downloadFile(
    accessToken: String,
    dropboxEntry: DropboxEntry
) async throws {
    print(dropboxEntry.name)
    if dropboxEntry.path_display != nil, let p = dropboxEntry.path_display {
        let data = try await getFileContents(accessToken: accessToken, dropboxPath: p)
        //saveFile(data, dropboxEntry.name)
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
