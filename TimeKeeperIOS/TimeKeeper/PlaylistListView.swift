import SwiftUI

struct PlaylistListView: View {
    @State private var playlists: [PlaylistHeader]? = nil
    @State private var showToast = false
    @State private var toastMessage = ""
    
    var body: some View {
        NavigationStack {
            ZStack {
                List(playlists ?? []) { entry in
                    NavigationLink {
                        PlaylistView(id: entry.id)
                    } label: {
                        Text(entry.name)
                    }
                }
                
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        Button(action: {
                            syncFromDropbox()
                        }) {
                            Image(systemName: "arrow.clockwise")
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundColor(.white)
                                .padding(20)
                                .background(Color.blue)
                                .clipShape(Circle())
                                .shadow(radius: 4, y: 2)
                        }
                        .accessibilityLabel("Sync from Dropbox")
                        .padding(.trailing, 20)
                        .padding(.bottom, 20)
                    }
                }
            }
            .overlay(
                Group {
                    if showToast {
                        VStack {
                            Spacer()
                            Text(toastMessage)
                                .font(.subheadline)
                                .foregroundColor(.white)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 10)
                                .background(Color.black.opacity(0.8))
                                .clipShape(Capsule())
                                .padding(.bottom, 24)
                        }
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                        .animation(.easeInOut(duration: 0.25), value: showToast)
                    }
                }
            )
            .navigationTitle("Playlists")
            .onAppear(perform: setPlaylists)
        }
    }
    
    private func setPlaylists() {
        playlists = loadPlaylists()
    }
    
    private func syncFromDropbox() {
        Task {
            do {
                toastMessage = "Syncing DropBox ..."
                withAnimation { showToast = true }
                
                try await syncDropBox()
                setPlaylists()
                toastMessage = "Successfully synced from Dropbox"
            } catch {
                toastMessage = "Failed to sync from Dropbox \(error.localizedDescription)"
            }
            
            try? await Task.sleep(nanoseconds: 5_000_000_000)
            withAnimation { showToast = false }
        }
    }
}

#Preview {
    NavigationStack { PlaylistListView() }
}
