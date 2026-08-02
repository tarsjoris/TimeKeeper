import Foundation
import UIKit
import CryptoKit

// https://www.dropbox.com/developers/apps/info?app_key=pm8yqqldb1ya1af

private let appKey = "pm8yqqldb1ya1af"
private let appSecret = "elkk3eggl74cftp"
private let redirectURI = "BeTarsTimeKeeper://oauth"

func getAccessToken() async throws -> String {
    if let refreshToken = retrieveRefreshToken() {
        return try await getAccessTokenForRefreshToken(refreshToken: refreshToken)
    } else {
        return try await createAccessToken()
    }
}

private func createAccessToken() async throws -> String {
    // 1) Open authorization URL in browser
    var components = URLComponents(string: "https://www.dropbox.com/oauth2/authorize")!
    let verifier = generateCodeVerifier()
    let challenge = codeChallenge(from: verifier)
    components.queryItems = [
        URLQueryItem(name: "client_id", value: appKey),
        URLQueryItem(name: "response_type", value: "code"),
        URLQueryItem(name: "redirect_uri", value: redirectURI),
        URLQueryItem(name: "code_challenge", value: challenge),
        URLQueryItem(name: "code_challenge_method", value: "S256"),
        URLQueryItem(name: "token_access_type", value: "offline")
    ]
    let url = components.url!
    await MainActor.run {
        UIApplication.shared.open(url, options: [:], completionHandler: nil)
    }

    // 2) Wait for redirect with authorization code via custom URL scheme
    let code: String = try await withCheckedThrowingContinuation { continuation in
        AuthRedirectWaiter.shared.waitForCode { result in
            switch result {
            case .success(let code):
                continuation.resume(returning: code)
            case .failure(let error):
                continuation.resume(throwing: error)
            }
        }
    }

    // 3) Exchange code for access and refresh token
    let tokenURL = URL(string: "https://api.dropboxapi.com/oauth2/token")!
    var request = URLRequest(url: tokenURL)
    request.httpMethod = "POST"
    let bodyItems = [
        URLQueryItem(name: "code", value: code),
        URLQueryItem(name: "grant_type", value: "authorization_code"),
        URLQueryItem(name: "client_id", value: appKey),
        URLQueryItem(name: "client_secret", value: appSecret),
        URLQueryItem(name: "redirect_uri", value: redirectURI),
        URLQueryItem(name: "code_verifier", value: verifier)
    ]
    var components2 = URLComponents()
    components2.queryItems = bodyItems
    request.httpBody = components2.query?.data(using: .utf8)
    request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")

    let (data, response) = try await URLSession.shared.data(for: request)
    guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
        let message = String(data: data, encoding: .utf8) ?? "Unknown error"
        throw NSError(domain: "Auth", code: 3, userInfo: [NSLocalizedDescriptionKey: "Token exchange failed: \(message)"])
    }

    struct TokenResponse: Decodable { let access_token: String; let refresh_token: String? }
    let decoded = try JSONDecoder().decode(TokenResponse.self, from: data)
    if let refresh = decoded.refresh_token {
        try? storeRefreshToken(refreshToken: refresh)
    }
    return decoded.access_token
}

private func getAccessTokenForRefreshToken(refreshToken: String) async throws -> String {
    let tokenURL = URL(string: "https://api.dropboxapi.com/oauth2/token")!
    var request = URLRequest(url: tokenURL)
    request.httpMethod = "POST"
    let bodyItems = [
        URLQueryItem(name: "refresh_token", value: refreshToken),
        URLQueryItem(name: "grant_type", value: "refresh_token"),
        URLQueryItem(name: "client_id", value: appKey),
        URLQueryItem(name: "client_secret", value: appSecret)
    ]
    var components = URLComponents()
    components.queryItems = bodyItems
    request.httpBody = components.query?.data(using: .utf8)
    request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")

    let (data, response) = try await URLSession.shared.data(for: request)
    guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
        let message = String(data: data, encoding: .utf8) ?? "Unknown error"
        throw NSError(domain: "Auth", code: 4, userInfo: [NSLocalizedDescriptionKey: "Token refresh failed: \(message)"])
    }

    struct RefreshResponse: Decodable { let access_token: String }
    let decoded = try JSONDecoder().decode(RefreshResponse.self, from: data)
    return decoded.access_token
}

private func generateCodeVerifier() -> String {
    // RFC 7636: code_verifier is a high-entropy cryptographic random string using the unreserved characters
    // length between 43 and 128
    var bytes = [UInt8](repeating: 0, count: 32)
    let status = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
    if status != errSecSuccess {
        // Fallback to UUID if random fails (very unlikely)
        return UUID().uuidString.replacingOccurrences(of: "-", with: "")
    }
    let data = Data(bytes)
    return base64URLEncode(data)
}

private func codeChallenge(from verifier: String) -> String {
    let data = Data(verifier.utf8)
    let hash = SHA256.hash(data: data)
    return base64URLEncode(Data(hash))
}

private func base64URLEncode(_ data: Data) -> String {
    let b64 = data.base64EncodedString()
    return b64
        .replacingOccurrences(of: "+", with: "-")
        .replacingOccurrences(of: "/", with: "_")
        .replacingOccurrences(of: "=", with: "")
}

private class AuthRedirectWaiter {
    static let shared = AuthRedirectWaiter()
    private var completion: ((Result<String, Error>) -> Void)?

    func waitForCode(completion: @escaping (Result<String, Error>) -> Void) {
        self.completion = completion
    }

    func handle(url: URL) {
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let code = components.queryItems?.first(where: { $0.name == "code" })?.value else {
            completion?(.failure(NSError(domain: "Auth", code: 5, userInfo: [NSLocalizedDescriptionKey: "Missing authorization code in redirect"])) )
            completion = nil
            return
        }
        completion?(.success(code))
        completion = nil
    }
}

// Call this from your App/Scene delegate when the app is opened via the custom URL scheme
func handleAuthRedirect(url: URL) {
    AuthRedirectWaiter.shared.handle(url: url)
}

private let service = "be.t-ars.timekeeper"
private let account = "refreshtoken"

private func storeRefreshToken(refreshToken: String) throws {
    let addQuery: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                   kSecAttrService as String: service,
                                   kSecAttrAccount as String: account,
                                   kSecValueData as String: refreshToken.data(using: .utf8)!]
    let status = SecItemAdd(addQuery as CFDictionary, nil)
    guard status == errSecSuccess else { throw NSError(domain: "Auth", code: 6, userInfo: [NSLocalizedDescriptionKey: "Cannot store refresh token in keychain"]) }
}

private func retrieveRefreshToken() -> String? {
    let getQuery: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                   kSecAttrService as String: service,
                                   kSecAttrAccount as String: account,
                                   kSecReturnData as String: true,
                                   kSecMatchLimit as String: kSecMatchLimitOne]
    var result: AnyObject?
    let status = SecItemCopyMatching(getQuery as CFDictionary, &result)
    guard status == errSecSuccess else { return nil }
    guard let data = result as? Data else { return nil }
    return String(data: data, encoding: .utf8)
}
